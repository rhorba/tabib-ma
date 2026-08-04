package com.tabibma.booking;

import com.tabibma.booking.dto.ResourceUtilizationResponse;
import com.tabibma.clinic.Clinic;
import com.tabibma.clinic.ClinicRepository;
import com.tabibma.clinic.ClinicResource;
import com.tabibma.clinic.ClinicResourceRepository;
import com.tabibma.clinic.ResourceType;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUtilizationServiceTest {

    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private ClinicResourceRepository clinicResourceRepository;
    @Mock
    private AppointmentResourceAllocationRepository appointmentResourceAllocationRepository;

    private ResourceUtilizationService service;
    private UUID adminUserId;
    private UUID clinicId;
    private Clinic clinic;

    @BeforeEach
    void setUp() {
        service = new ResourceUtilizationService(
                clinicRepository, clinicResourceRepository, appointmentResourceAllocationRepository);
        adminUserId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        clinic = new Clinic(adminUserId, "Clinique Atlas", "Rabat", "1 Rue X");
        ReflectionTestUtils.setField(clinic, "id", clinicId);
    }

    private UserContext adminPrincipal() {
        return new UserContext(adminUserId, "admin@example.com", Role.CLINIC_ADMIN);
    }

    @Test
    void getUtilization_throwsNotFoundWhenCallerHasNoClinic() {
        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUtilization(adminPrincipal())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getUtilization_returnsEmptyListWhenTheClinicHasNoResources() {
        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.findAllByClinicId(clinicId)).thenReturn(List.of());

        assertThat(service.getUtilization(adminPrincipal())).isEmpty();
    }

    @Test
    void getUtilization_includesAnIdleResourceWithAnEmptyAllocationList() {
        ClinicResource room = new ClinicResource(clinicId, ResourceType.ROOM, "Salle 1");
        UUID roomId = UUID.randomUUID();
        ReflectionTestUtils.setField(room, "id", roomId);

        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.findAllByClinicId(clinicId)).thenReturn(List.of(room));
        when(appointmentResourceAllocationRepository
                .findAllByResourceIdInAndEndsAtAfterOrderByStartsAtAsc(any(), any()))
                .thenReturn(List.of());

        List<ResourceUtilizationResponse> result = service.getUtilization(adminPrincipal());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resourceId()).isEqualTo(roomId);
        assertThat(result.get(0).type()).isEqualTo(ResourceType.ROOM);
        assertThat(result.get(0).active()).isTrue();
        assertThat(result.get(0).allocations()).isEmpty();
    }

    @Test
    void getUtilization_groupsAllocationsUnderTheirOwningResourceOrderedByStartTime() {
        ClinicResource room = new ClinicResource(clinicId, ResourceType.ROOM, "Salle 1");
        UUID roomId = UUID.randomUUID();
        ReflectionTestUtils.setField(room, "id", roomId);
        ClinicResource equipment = new ClinicResource(clinicId, ResourceType.EQUIPMENT, "Echographe");
        UUID equipmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(equipment, "id", equipmentId);

        Instant start = Instant.now().plusSeconds(3600);
        UUID appointmentId = UUID.randomUUID();
        AppointmentResourceAllocation allocation =
                new AppointmentResourceAllocation(appointmentId, roomId, start, start.plusSeconds(1800));

        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.of(clinic));
        when(clinicResourceRepository.findAllByClinicId(clinicId)).thenReturn(List.of(room, equipment));
        when(appointmentResourceAllocationRepository
                .findAllByResourceIdInAndEndsAtAfterOrderByStartsAtAsc(any(), any()))
                .thenReturn(List.of(allocation));

        List<ResourceUtilizationResponse> result = service.getUtilization(adminPrincipal());

        ResourceUtilizationResponse roomEntry = result.stream()
                .filter(r -> r.resourceId().equals(roomId)).findFirst().orElseThrow();
        ResourceUtilizationResponse equipmentEntry = result.stream()
                .filter(r -> r.resourceId().equals(equipmentId)).findFirst().orElseThrow();

        assertThat(roomEntry.allocations()).hasSize(1);
        assertThat(roomEntry.allocations().get(0).appointmentId()).isEqualTo(appointmentId);
        assertThat(equipmentEntry.allocations()).isEmpty();
    }
}
