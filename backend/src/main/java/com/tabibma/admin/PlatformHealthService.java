package com.tabibma.admin;

import com.tabibma.admin.dto.PlatformHealthResponse;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.AppointmentStatus;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import org.springframework.stereotype.Service;

/**
 * Story 10.3: platform-wide, read-only snapshot counts across the booking and payment modules —
 * no clinic/doctor scoping, unlike {@code ClinicDashboardService}. Plain {@code count(status)}
 * queries rather than a single aggregate query: this endpoint is PLATFORM_ADMIN-only and called
 * on-demand (no polling requirement in the AC), so the extra round trips aren't worth a hand-rolled
 * projection query for the readability lost.
 */
@Service
public class PlatformHealthService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    public PlatformHealthService(AppointmentRepository appointmentRepository, PaymentRepository paymentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
    }

    public PlatformHealthResponse getHealth() {
        return new PlatformHealthResponse(
                appointmentRepository.count(),
                appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED),
                appointmentRepository.countByStatus(AppointmentStatus.CANCELLED),
                appointmentRepository.countByStatus(AppointmentStatus.COMPLETED),
                appointmentRepository.countByStatus(AppointmentStatus.NO_SHOW),
                appointmentRepository.countByStatus(AppointmentStatus.PENDING_PAYMENT),
                paymentRepository.countByStatus(PaymentStatus.SUCCEEDED),
                paymentRepository.countByStatus(PaymentStatus.FAILED),
                paymentRepository.countByStatus(PaymentStatus.REFUNDED)
        );
    }
}
