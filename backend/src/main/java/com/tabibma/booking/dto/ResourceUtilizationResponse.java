package com.tabibma.booking.dto;

import com.tabibma.clinic.ResourceType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResourceUtilizationResponse(
        UUID resourceId,
        String resourceName,
        ResourceType type,
        boolean active,
        List<AllocationWindow> allocations) {

    public record AllocationWindow(UUID appointmentId, Instant startsAt, Instant endsAt) {
    }
}
