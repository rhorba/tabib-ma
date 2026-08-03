package com.tabibma.booking.dto;

import java.math.BigDecimal;

public record ClinicDashboardResponse(long bookingVolume, BigDecimal revenueMad) {
}
