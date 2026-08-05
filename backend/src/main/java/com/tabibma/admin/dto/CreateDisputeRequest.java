package com.tabibma.admin.dto;

import com.tabibma.admin.DisputeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDisputeRequest(
        @NotNull UUID appointmentId,
        @NotNull DisputeType type,
        @NotBlank String reason
) {
}
