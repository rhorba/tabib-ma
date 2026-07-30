package com.tabibma.clinic.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record DoctorSearchResultResponse(
        UUID doctorProfileId,
        String firstName,
        String lastName,
        String specialty,
        String city,
        BigDecimal consultationFeeMad
) implements Serializable {
}
