package com.tabibma.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAvailabilityExceptionRequest(
        @NotNull LocalDate exceptionDate,
        @Size(max = 500) String reason
) {
}
