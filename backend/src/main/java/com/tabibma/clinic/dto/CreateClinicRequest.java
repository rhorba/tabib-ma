package com.tabibma.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClinicRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 500) String address
) {
}
