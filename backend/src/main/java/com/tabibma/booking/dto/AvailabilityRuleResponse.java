package com.tabibma.booking.dto;

import com.tabibma.booking.AvailabilityRule;
import com.tabibma.booking.LocationType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AvailabilityRuleResponse(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        LocationType locationType,
        UUID clinicId,
        boolean active,
        List<UUID> resourceIds
) {
    public static AvailabilityRuleResponse from(AvailabilityRule rule, List<UUID> resourceIds) {
        return new AvailabilityRuleResponse(rule.getId(), rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime(),
                rule.getSlotDurationMinutes(), rule.getLocationType(), rule.getClinicId(), rule.isActive(), resourceIds);
    }
}
