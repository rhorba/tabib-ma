package com.tabibma.clinic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorPublicProfileResponse(
        UUID doctorProfileId,
        String firstName,
        String lastName,
        String specialty,
        String city,
        String bio,
        BigDecimal consultationFeeMad,
        Double averageRating,
        long reviewCount
) {
}
