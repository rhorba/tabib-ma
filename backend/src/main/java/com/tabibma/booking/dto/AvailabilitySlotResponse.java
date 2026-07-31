package com.tabibma.booking.dto;

import com.tabibma.booking.AvailabilitySlot;
import com.tabibma.booking.LocationType;

import java.time.Instant;
import java.util.UUID;

public record AvailabilitySlotResponse(
        UUID id,
        UUID doctorProfileId,
        Instant startsAt,
        Instant endsAt,
        LocationType locationType,
        UUID clinicId,
        boolean booked
) {
    public static AvailabilitySlotResponse from(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(slot.getId(), slot.getDoctorProfileId(), slot.getStartsAt(),
                slot.getEndsAt(), slot.getLocationType(), slot.getClinicId(), slot.isBooked());
    }
}
