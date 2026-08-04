package com.tabibma.booking;

import com.tabibma.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceAllocationGuardTest {

    @Mock
    private AvailabilitySlotResourceRepository availabilitySlotResourceRepository;
    @Mock
    private AppointmentResourceAllocationRepository appointmentResourceAllocationRepository;

    private ResourceAllocationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ResourceAllocationGuard(availabilitySlotResourceRepository, appointmentResourceAllocationRepository);
    }

    @Test
    void allocateForSlot_isNoOpWhenSlotRequiresNoResources() {
        UUID slotId = UUID.randomUUID();
        when(availabilitySlotResourceRepository.findAllByAvailabilitySlotId(slotId)).thenReturn(List.of());

        guard.allocateForSlot(UUID.randomUUID(), slotId, Instant.now(), Instant.now().plusSeconds(1800));

        verify(appointmentResourceAllocationRepository, never()).saveAndFlush(any());
    }

    @Test
    void allocateForSlot_savesAnAllocationPerRequiredResource() {
        UUID slotId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID resourceId1 = UUID.randomUUID();
        UUID resourceId2 = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1800);
        when(availabilitySlotResourceRepository.findAllByAvailabilitySlotId(slotId)).thenReturn(List.of(
                new AvailabilitySlotResource(slotId, resourceId1),
                new AvailabilitySlotResource(slotId, resourceId2)));
        when(appointmentResourceAllocationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        guard.allocateForSlot(appointmentId, slotId, start, end);

        verify(appointmentResourceAllocationRepository).saveAndFlush(
                argMatchingResource(appointmentId, resourceId1, start, end));
        verify(appointmentResourceAllocationRepository).saveAndFlush(
                argMatchingResource(appointmentId, resourceId2, start, end));
    }

    @Test
    void allocateForSlot_translatesDataAccessExceptionIntoConflictException() {
        UUID slotId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(availabilitySlotResourceRepository.findAllByAvailabilitySlotId(slotId))
                .thenReturn(List.of(new AvailabilitySlotResource(slotId, resourceId)));
        when(appointmentResourceAllocationRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("exclude constraint violated"));

        assertThatThrownBy(() -> guard.allocateForSlot(UUID.randomUUID(), slotId, Instant.now(), Instant.now().plusSeconds(1800)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void releaseForAppointment_delegatesToRepository() {
        UUID appointmentId = UUID.randomUUID();

        guard.releaseForAppointment(appointmentId);

        verify(appointmentResourceAllocationRepository).deleteAllByAppointmentId(appointmentId);
    }

    private static AppointmentResourceAllocation argMatchingResource(UUID appointmentId, UUID resourceId,
                                                                       Instant start, Instant end) {
        return org.mockito.ArgumentMatchers.argThat(allocation ->
                allocation.getAppointmentId().equals(appointmentId)
                        && allocation.getResourceId().equals(resourceId)
                        && allocation.getStartsAt().equals(start)
                        && allocation.getEndsAt().equals(end));
    }
}
