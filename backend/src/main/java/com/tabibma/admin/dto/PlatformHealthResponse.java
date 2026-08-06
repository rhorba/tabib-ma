package com.tabibma.admin.dto;

/** Story 10.3 AC lists three metric families: bookings, payment failures, and video call
 * quality. Video call quality is omitted here — nothing in the codebase records any such
 * metric yet (see .logs/decisions.md 2026-08-05), and fabricating a placeholder value would be
 * worse than not showing it. */
public record PlatformHealthResponse(
        long totalAppointments,
        long confirmedAppointments,
        long cancelledAppointments,
        long completedAppointments,
        long noShowAppointments,
        long pendingPaymentAppointments,
        long succeededPayments,
        long failedPayments,
        long refundedPayments
) {
}
