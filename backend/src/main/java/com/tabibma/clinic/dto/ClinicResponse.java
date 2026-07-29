package com.tabibma.clinic.dto;

import com.tabibma.clinic.Clinic;

import java.util.UUID;

public record ClinicResponse(
        UUID id,
        UUID adminUserId,
        String name,
        String city,
        String address
) {
    public static ClinicResponse from(Clinic clinic) {
        return new ClinicResponse(clinic.getId(), clinic.getAdminUserId(), clinic.getName(),
                clinic.getCity(), clinic.getAddress());
    }
}
