package com.tabibma.clinic.dto;

import com.tabibma.clinic.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClinicResourceRequest(
        @NotNull ResourceType type,
        @NotBlank @Size(max = 200) String name
) {
}
