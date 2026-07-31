package com.tabibma.booking.dto;

import com.tabibma.booking.LocationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record CreateAvailabilityRuleRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(5) int slotDurationMinutes,
        @NotNull LocationType locationType,
        UUID clinicId
) {
}
