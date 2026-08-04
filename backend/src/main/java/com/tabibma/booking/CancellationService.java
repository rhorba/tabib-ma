package com.tabibma.booking;

import com.tabibma.identity.UserContext;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Story 4.4: cancellation is always allowed (Appointment.cancel()'s own guard is the only gate);
 * this service decides refund eligibility via CancellationPolicy and releases the slot. */
@Service
public class CancellationService {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final PaymentRepository paymentRepository;
    private final ResourceAllocationGuard resourceAllocationGuard;

    public CancellationService(AppointmentRepository appointmentRepository,
                                AvailabilitySlotRepository availabilitySlotRepository,
                                PaymentRepository paymentRepository,
                                ResourceAllocationGuard resourceAllocationGuard) {
        this.appointmentRepository = appointmentRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.paymentRepository = paymentRepository;
        this.resourceAllocationGuard = resourceAllocationGuard;
    }

    @Transactional
    public Appointment cancel(UserContext principal, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        if (!appointment.getPatientId().equals(principal.userId())) {
            throw new ForbiddenException("You can only cancel your own appointments.");
        }

        CancellationPolicy policy = new CancellationPolicy(appointment.getCancellationWindowHours());
        boolean refundEligible = policy.isWithinWindow(Instant.now(), appointment.getStartsAt());

        appointment.cancel();
        appointmentRepository.save(appointment);

        availabilitySlotRepository.findById(appointment.getAvailabilitySlotId()).ifPresent(slot -> {
            slot.release();
            availabilitySlotRepository.save(slot);
        });
        resourceAllocationGuard.releaseForAppointment(appointmentId);

        if (refundEligible) {
            paymentRepository.findByAppointmentId(appointmentId)
                    .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                    .ifPresent(payment -> {
                        payment.refund();
                        paymentRepository.save(payment);
                    });
        }

        return appointment;
    }
}
