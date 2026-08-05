package com.tabibma.booking;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Story 10.1 Batch 2: a doctor marking their own CONFIRMED appointment as a no-show, once its
 * start time has passed. Publishes AppointmentNoShowEvent rather than creating a dispute
 * directly — the admin module's DisputeEventListener reacts to it, keeping booking
 * dependency-free of admin (same direction as every other cross-module reaction here). */
@Service
public class NoShowService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NoShowService(AppointmentRepository appointmentRepository, DoctorProfileRepository doctorProfileRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Appointment markNoShow(UserContext principal, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        DoctorProfile profile = doctorProfileRepository.findById(appointment.getDoctorProfileId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
        if (!profile.getUserId().equals(principal.userId())) {
            throw new ForbiddenException("You can only mark your own appointments as a no-show.");
        }
        if (Instant.now().isBefore(appointment.getStartsAt())) {
            throw new ValidationException("This appointment hasn't started yet.");
        }

        appointment.markNoShow();
        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new AppointmentNoShowEvent(appointmentId));
        return saved;
    }
}
