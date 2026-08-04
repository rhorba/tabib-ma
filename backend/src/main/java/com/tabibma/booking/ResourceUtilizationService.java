package com.tabibma.booking;

import com.tabibma.booking.dto.ResourceUtilizationResponse;
import com.tabibma.booking.dto.ResourceUtilizationResponse.AllocationWindow;
import com.tabibma.clinic.Clinic;
import com.tabibma.clinic.ClinicRepository;
import com.tabibma.clinic.ClinicResource;
import com.tabibma.clinic.ClinicResourceRepository;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Story 8.2 Batch 4: read-only calendar/utilization view of a clinic's shared resources — which
 * rooms/equipment are currently booked and when, scoped to the calling clinic admin's own
 * clinic. Lives in booking, not clinic, for the same reason as ClinicDashboardService: it reads
 * appointment_resource_allocations, and clinic must stay dependency-free.
 *
 * <p>Only allocations that haven't ended yet are returned (matches the "calendar" framing —
 * this is a scheduling view, not a historical report); a resource with no upcoming allocations
 * still appears, with an empty list, so an admin can see idle resources too.
 */
@Service
public class ResourceUtilizationService {

    private final ClinicRepository clinicRepository;
    private final ClinicResourceRepository clinicResourceRepository;
    private final AppointmentResourceAllocationRepository appointmentResourceAllocationRepository;

    public ResourceUtilizationService(ClinicRepository clinicRepository,
                                       ClinicResourceRepository clinicResourceRepository,
                                       AppointmentResourceAllocationRepository appointmentResourceAllocationRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicResourceRepository = clinicResourceRepository;
        this.appointmentResourceAllocationRepository = appointmentResourceAllocationRepository;
    }

    public List<ResourceUtilizationResponse> getUtilization(UserContext principal) {
        Clinic clinic = clinicRepository.findByAdminUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("You don't have a clinic yet."));

        List<ClinicResource> resources = clinicResourceRepository.findAllByClinicId(clinic.getId());
        if (resources.isEmpty()) {
            return List.of();
        }

        List<UUID> resourceIds = resources.stream().map(ClinicResource::getId).toList();
        Map<UUID, List<AllocationWindow>> allocationsByResource = appointmentResourceAllocationRepository
                .findAllByResourceIdInAndEndsAtAfterOrderByStartsAtAsc(resourceIds, Instant.now())
                .stream()
                .collect(Collectors.groupingBy(
                        AppointmentResourceAllocation::getResourceId,
                        Collectors.mapping(
                                a -> new AllocationWindow(a.getAppointmentId(), a.getStartsAt(), a.getEndsAt()),
                                Collectors.toList())));

        return resources.stream()
                .map(r -> new ResourceUtilizationResponse(r.getId(), r.getName(), r.getType(), r.isActive(),
                        allocationsByResource.getOrDefault(r.getId(), List.of())))
                .toList();
    }
}
