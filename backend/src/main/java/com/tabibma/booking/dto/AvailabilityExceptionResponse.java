package com.tabibma.booking.dto;

import com.tabibma.booking.AvailabilityBlockedDate;

import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityExceptionResponse(UUID id, LocalDate exceptionDate, String reason) {
    public static AvailabilityExceptionResponse from(AvailabilityBlockedDate blockedDate) {
        return new AvailabilityExceptionResponse(blockedDate.getId(), blockedDate.getExceptionDate(), blockedDate.getReason());
    }
}
