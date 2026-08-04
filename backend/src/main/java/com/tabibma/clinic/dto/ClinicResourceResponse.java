package com.tabibma.clinic.dto;

import com.tabibma.clinic.ClinicResource;
import com.tabibma.clinic.ResourceType;

import java.util.UUID;

public record ClinicResourceResponse(
        UUID id,
        UUID clinicId,
        ResourceType type,
        String name,
        boolean active
) {
    public static ClinicResourceResponse from(ClinicResource resource) {
        return new ClinicResourceResponse(resource.getId(), resource.getClinicId(), resource.getType(),
                resource.getName(), resource.isActive());
    }
}
