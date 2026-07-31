package com.tabibma.booking;

import java.util.UUID;

/** Domain event (Architecture doc §4) fired by {@link ReminderService}'s sweep once an
 * appointment enters its reminder lead time — consumed by the notification module's listener. */
public record ReminderDueEvent(UUID appointmentId) {
}
