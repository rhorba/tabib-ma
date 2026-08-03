package com.tabibma.booking;

import com.tabibma.booking.dto.ClinicDashboardResponse;
import com.tabibma.clinic.Clinic;
import com.tabibma.clinic.ClinicRepository;
import com.tabibma.clinic.ClinicStaffMembership;
import com.tabibma.clinic.ClinicStaffMembershipRepository;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Story 8.1: read-only clinic-wide booking volume + revenue, scoped to the calling clinic
 * admin's own clinic. Lives in the booking module rather than clinic — clinic must stay a
 * dependency-free module (see clinic.DoctorSearchService's own comment from Epic 9), and
 * booking already depends on both clinic (DoctorProfile) and payment (PaymentService) in the
 * safe direction, so aggregating here doesn't risk the clinic&lt;-&gt;booking cycle
 * ArchitectureTest caught the first time this kind of composition came up.
 *
 * <p>Booking volume counts CONFIRMED + COMPLETED appointments, not strictly COMPLETED: an
 * IN_PERSON appointment currently has no code path to COMPLETED at all (only a VIDEO
 * consultation's ConsultationService.complete() sets it), so a literal "completed appointments"
 * reading would near-zero this dashboard for any clinic with in-person doctors. Revenue sums
 * SUCCEEDED payments only, matching the "no CONFIRMED appointment without a matching SUCCEEDED
 * Payment" invariant (Architecture doc §3) — see .logs/decisions.md 2026-08-03.
 */
@Service
public class ClinicDashboardService {

    private static final Set<AppointmentStatus> COUNTED_STATUSES =
            EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);

    private final ClinicRepository clinicRepository;
    private final ClinicStaffMembershipRepository clinicStaffMembershipRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    public ClinicDashboardService(ClinicRepository clinicRepository,
                                   ClinicStaffMembershipRepository clinicStaffMembershipRepository,
                                   AppointmentRepository appointmentRepository,
                                   PaymentRepository paymentRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicStaffMembershipRepository = clinicStaffMembershipRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
    }

    public ClinicDashboardResponse getDashboard(UserContext principal) {
        Clinic clinic = clinicRepository.findByAdminUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("You don't have a clinic yet."));

        List<UUID> doctorProfileIds = clinicStaffMembershipRepository.findAllByClinicId(clinic.getId()).stream()
                .map(ClinicStaffMembership::getDoctorProfileId)
                .toList();
        if (doctorProfileIds.isEmpty()) {
            return new ClinicDashboardResponse(0, BigDecimal.ZERO);
        }

        List<Appointment> appointments =
                appointmentRepository.findAllByDoctorProfileIdInAndStatusIn(doctorProfileIds, COUNTED_STATUSES);
        if (appointments.isEmpty()) {
            return new ClinicDashboardResponse(0, BigDecimal.ZERO);
        }

        List<UUID> appointmentIds = appointments.stream().map(Appointment::getId).toList();
        BigDecimal revenue = paymentRepository
                .findAllByAppointmentIdInAndStatus(appointmentIds, PaymentStatus.SUCCEEDED).stream()
                .map(Payment::getAmountMad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClinicDashboardResponse(appointments.size(), revenue);
    }
}
