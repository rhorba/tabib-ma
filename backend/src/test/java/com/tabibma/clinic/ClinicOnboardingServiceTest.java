package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateClinicRequest;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicOnboardingServiceTest {

    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private ClinicInvitationRepository clinicInvitationRepository;

    private ClinicOnboardingService service;

    @BeforeEach
    void setUp() {
        service = new ClinicOnboardingService(clinicRepository, clinicInvitationRepository);
    }

    @Test
    void createClinic_rejectsNonClinicAdminRole() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        CreateClinicRequest request = new CreateClinicRequest("Cabinet Test", "Rabat", null);

        assertThatThrownBy(() -> service.createClinic(doctor, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createClinic_rejectsWhenClinicAlreadyExists() {
        UserContext admin = new UserContext(UUID.randomUUID(), "a@example.com", Role.CLINIC_ADMIN);
        CreateClinicRequest request = new CreateClinicRequest("Cabinet Test", "Rabat", null);
        when(clinicRepository.existsByAdminUserId(admin.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.createClinic(admin, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createClinic_savesClinicForAdmin() {
        UserContext admin = new UserContext(UUID.randomUUID(), "a@example.com", Role.CLINIC_ADMIN);
        CreateClinicRequest request = new CreateClinicRequest("Cabinet Test", "Rabat", "12 Rue Test");
        when(clinicRepository.existsByAdminUserId(admin.userId())).thenReturn(false);
        when(clinicRepository.save(any(Clinic.class))).thenAnswer(inv -> inv.getArgument(0));

        Clinic saved = service.createClinic(admin, request);

        assertThat(saved.getAdminUserId()).isEqualTo(admin.userId());
        assertThat(saved.getName()).isEqualTo("Cabinet Test");
    }

    @Test
    void getMyClinic_rejectsWhenNoneExists() {
        UserContext admin = new UserContext(UUID.randomUUID(), "a@example.com", Role.CLINIC_ADMIN);
        when(clinicRepository.findByAdminUserId(admin.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyClinic(admin)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void inviteDoctor_rejectsWhenClinicNotFound() {
        UserContext admin = new UserContext(UUID.randomUUID(), "a@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inviteDoctor(admin, clinicId, "doc@example.com"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void inviteDoctor_rejectsWhenCallerDoesNotOwnClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext otherAdmin = new UserContext(UUID.randomUUID(), "other@example.com", Role.CLINIC_ADMIN);
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        UUID clinicId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));

        assertThatThrownBy(() -> service.inviteDoctor(otherAdmin, clinicId, "doc@example.com"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void inviteDoctor_rejectsDuplicatePendingInvitation() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        UUID clinicId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicInvitationRepository.existsByClinicIdAndInvitedEmailAndStatus(
                any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.inviteDoctor(admin, clinicId, "doc@example.com"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void inviteDoctor_createsInvitation() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        UUID clinicId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicInvitationRepository.existsByClinicIdAndInvitedEmailAndStatus(
                any(), any(), any())).thenReturn(false);
        when(clinicInvitationRepository.save(any(ClinicInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicInvitation invitation = service.inviteDoctor(admin, clinicId, "doc@example.com");

        assertThat(invitation.getInvitedEmail()).isEqualTo("doc@example.com");
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void listInvitations_rejectsWhenCallerDoesNotOwnClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext otherAdmin = new UserContext(UUID.randomUUID(), "other@example.com", Role.CLINIC_ADMIN);
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        UUID clinicId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));

        assertThatThrownBy(() -> service.listInvitations(otherAdmin, clinicId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listInvitations_returnsInvitationsForOwner() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        UUID clinicId = UUID.randomUUID();
        ClinicInvitation invitation = new ClinicInvitation(clinicId, "doc@example.com");
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicInvitationRepository.findAllByClinicId(clinicId)).thenReturn(List.of(invitation));

        assertThat(service.listInvitations(admin, clinicId)).containsExactly(invitation);
    }
}
