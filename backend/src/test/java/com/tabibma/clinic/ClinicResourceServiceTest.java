package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateClinicResourceRequest;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicResourceServiceTest {

    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private ClinicResourceRepository clinicResourceRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private ClinicStaffMembershipRepository clinicStaffMembershipRepository;

    private ClinicResourceService service;

    @BeforeEach
    void setUp() {
        service = new ClinicResourceService(clinicRepository, clinicResourceRepository,
                doctorProfileRepository, clinicStaffMembershipRepository);
    }

    @Test
    void createResource_rejectsWhenClinicNotFound() {
        UserContext admin = new UserContext(UUID.randomUUID(), "a@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createResource(admin, clinicId,
                new CreateClinicResourceRequest(ResourceType.ROOM, "Salle 1")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createResource_rejectsWhenCallerDoesNotOwnClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext otherAdmin = new UserContext(UUID.randomUUID(), "other@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));

        assertThatThrownBy(() -> service.createResource(otherAdmin, clinicId,
                new CreateClinicResourceRequest(ResourceType.ROOM, "Salle 1")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createResource_savesResourceForOwnedClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.save(any(ClinicResource.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicResource saved = service.createResource(admin, clinicId,
                new CreateClinicResourceRequest(ResourceType.EQUIPMENT, "Echographe"));

        assertThat(saved.getClinicId()).isEqualTo(clinicId);
        assertThat(saved.getType()).isEqualTo(ResourceType.EQUIPMENT);
        assertThat(saved.getName()).isEqualTo("Echographe");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void listResources_rejectsWhenCallerDoesNotOwnClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext otherAdmin = new UserContext(UUID.randomUUID(), "other@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));

        assertThatThrownBy(() -> service.listResources(otherAdmin, clinicId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listResources_returnsResourcesForOwner() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        ClinicResource resource = new ClinicResource(clinicId, ResourceType.ROOM, "Salle 1");
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.findAllByClinicId(clinicId)).thenReturn(List.of(resource));

        assertThat(service.listResources(admin, clinicId)).containsExactly(resource);
    }

    @Test
    void listResources_returnsActiveResourcesOnlyForStaffDoctor() {
        UUID ownerId = UUID.randomUUID();
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        DoctorProfile profile = new DoctorProfile(doctor.userId(), "Cardiology", "bio", java.math.BigDecimal.TEN, "Rabat");
        ClinicResource resource = new ClinicResource(clinicId, ResourceType.ROOM, "Salle 1");
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));
        when(clinicStaffMembershipRepository.existsByClinicIdAndDoctorProfileId(eq(clinicId), any()))
                .thenReturn(true);
        when(clinicResourceRepository.findAllByClinicIdAndActiveTrue(clinicId)).thenReturn(List.of(resource));

        assertThat(service.listResources(doctor, clinicId)).containsExactly(resource);
    }

    @Test
    void listResources_rejectsDoctorWhoIsNotStaffAtTheClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listResources(doctor, clinicId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivateResource_rejectsWhenResourceBelongsToADifferentClinic() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        UUID otherClinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        ClinicResource resource = new ClinicResource(otherClinicId, ResourceType.ROOM, "Salle 1");
        UUID resourceId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.deactivateResource(admin, clinicId, resourceId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivateResource_marksResourceInactive() {
        UUID ownerId = UUID.randomUUID();
        UserContext admin = new UserContext(ownerId, "a@example.com", Role.CLINIC_ADMIN);
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic(ownerId, "Cabinet Test", "Rabat", null);
        ClinicResource resource = new ClinicResource(clinicId, ResourceType.ROOM, "Salle 1");
        UUID resourceId = UUID.randomUUID();
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(clinicResourceRepository.save(any(ClinicResource.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicResource deactivated = service.deactivateResource(admin, clinicId, resourceId);

        assertThat(deactivated.isActive()).isFalse();
    }
}
