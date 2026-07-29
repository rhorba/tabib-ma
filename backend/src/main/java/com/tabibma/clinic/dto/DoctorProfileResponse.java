package com.tabibma.clinic.dto;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorProfileResponse(
        UUID id,
        UUID userId,
        String specialty,
        String bio,
        BigDecimal consultationFeeMad,
        VerificationStatus verificationStatus,
        String city
) {
    public static DoctorProfileResponse from(DoctorProfile profile) {
        return new DoctorProfileResponse(profile.getId(), profile.getUserId(), profile.getSpecialty(),
                profile.getBio(), profile.getConsultationFeeMad(), profile.getVerificationStatus(), profile.getCity());
    }
}
