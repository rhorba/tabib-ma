package com.tabibma.booking;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentService;
import com.tabibma.payment.PaymentStatus;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates Story 4.2's "book and pay in one flow": reserve the slot (Story 4.3's
 * DoubleBookingGuard), capture payment (Story 5.1's PaymentService) for a server-recomputed
 * amount, then confirm or roll back. The invariant "no CONFIRMED appointment without a matching
 * SUCCEEDED Payment" (Architecture doc §3) holds because confirm() only runs after
 * capturePayment() returns SUCCEEDED.
 */
@Service
public class BookingService {

    private final DoubleBookingGuard doubleBookingGuard;
    private final ResourceAllocationGuard resourceAllocationGuard;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(DoubleBookingGuard doubleBookingGuard,
                           ResourceAllocationGuard resourceAllocationGuard,
                           AvailabilitySlotRepository availabilitySlotRepository,
                           AppointmentRepository appointmentRepository,
                           DoctorProfileRepository doctorProfileRepository,
                           PaymentService paymentService,
                           ApplicationEventPublisher eventPublisher) {
        this.doubleBookingGuard = doubleBookingGuard;
        this.resourceAllocationGuard = resourceAllocationGuard;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Appointment bookAndPay(UserContext principal, UUID availabilitySlotId) {
        if (principal.role() != Role.PATIENT) {
            throw new ForbiddenException("Only patients can book appointments.");
        }

        Appointment appointment = doubleBookingGuard.reserveSlot(principal.userId(), availabilitySlotId);
        // Checked before payment capture so a resource conflict never results in charging the
        // patient for a booking that can't actually be honoured; any ConflictException here rolls
        // back this whole transaction, including the appointment/slot rows reserveSlot just wrote.
        resourceAllocationGuard.allocateForSlot(appointment.getId(), availabilitySlotId,
                appointment.getStartsAt(), appointment.getEndsAt());

        DoctorProfile doctorProfile = doctorProfileRepository.findById(appointment.getDoctorProfileId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
        BigDecimal amountMad = doctorProfile.getConsultationFeeMad();

        String idempotencyKey = "appointment:" + appointment.getId();
        Payment payment = paymentService.capturePayment(appointment.getId(), amountMad, idempotencyKey);

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            appointment.confirm();
            appointmentRepository.save(appointment);
            eventPublisher.publishEvent(new BookingConfirmedEvent(appointment.getId()));
        } else {
            appointment.cancel();
            appointmentRepository.save(appointment);
            releaseSlot(availabilitySlotId);
            resourceAllocationGuard.releaseForAppointment(appointment.getId());
        }
        return appointment;
    }

    public List<Appointment> listMyAppointments(UserContext principal) {
        if (principal.role() == Role.DOCTOR) {
            DoctorProfile profile = doctorProfileRepository.findByUserId(principal.userId())
                    .orElseThrow(() -> new NotFoundException("You don't have a doctor profile yet."));
            return appointmentRepository.findAllByDoctorProfileId(profile.getId());
        }
        return appointmentRepository.findAllByPatientId(principal.userId());
    }

    private void releaseSlot(UUID availabilitySlotId) {
        availabilitySlotRepository.findById(availabilitySlotId).ifPresent(slot -> {
            slot.release();
            availabilitySlotRepository.save(slot);
        });
    }
}
