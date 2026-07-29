package com.tabibma.clinic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateDoctorProfileRequest(
        @NotBlank @Size(max = 100) String specialty,
        @Size(max = 2000) String bio,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal consultationFeeMad,
        @NotBlank @Size(max = 100) String city
) {
}
