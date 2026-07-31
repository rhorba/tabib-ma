package com.tabibma.booking.dto;

import java.time.LocalDate;

/** Both fields optional — the service defaults to [today, today+30 days) in the clinic's timezone. */
public record GenerateSlotsRequest(LocalDate fromDate, LocalDate toDate) {
}
