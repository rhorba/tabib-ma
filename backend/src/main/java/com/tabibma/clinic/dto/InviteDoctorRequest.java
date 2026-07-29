package com.tabibma.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteDoctorRequest(
        @NotBlank @Email String email
) {
}
