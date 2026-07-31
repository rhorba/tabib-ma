package com.tabibma.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** No price/fee field on purpose — the amount is always server-recomputed from the doctor's
 * consultationFeeMad (Story 4.3's "tampered price/fee payload" adversarial note), never accepted
 * from the client. */
public record BookAppointmentRequest(@NotNull UUID availabilitySlotId) {
}
