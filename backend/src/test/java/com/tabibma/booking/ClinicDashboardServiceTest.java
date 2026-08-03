package com.tabibma.booking;

import com.tabibma.booking.dto.ClinicDashboardResponse;
import com.tabibma.clinic.Clinic;
import com.tabibma.clinic.ClinicRepository;
import com.tabibma.clinic.ClinicStaffMembership;
import com.tabibma.clinic.ClinicStaffMembershipRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicDashboardServiceTest {

    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private ClinicStaffMembershipRepository clinicStaffMembershipRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private ClinicDashboardService service;
    private UUID adminUserId;
    private UUID clinicId;
    private Clinic clinic;

    @BeforeEach
    void setUp() {
        service = new ClinicDashboardService(
                clinicRepository, clinicStaffMembershipRepository, appointmentRepository, paymentRepository);
        adminUserId = UUID.randomUUID();
        clinicId = UUID.randomUUID();
        clinic = new Clinic(adminUserId, "Clinique Atlas", "Rabat", "1 Rue X");
        ReflectionTestUtils.setField(clinic, "id", clinicId);
    }

    private UserContext adminPrincipal() {
        return new UserContext(adminUserId, "admin@example.com", Role.CLINIC_ADMIN);
    }

    @Test
    void getDashboard_throwsNotFoundWhenCallerHasNoClinic() {
        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboard(adminPrincipal())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getDashboard_returnsZeroesWhenTheClinicHasNoStaff() {
        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.of(clinic));
        when(clinicStaffMembershipRepository.findAllByClinicId(clinicId)).thenReturn(List.of());

        ClinicDashboardResponse response = service.getDashboard(adminPrincipal());

        assertThat(response.bookingVolume()).isZero();
        assertThat(response.revenueMad()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getDashboard_countsConfirmedAndCompletedAppointmentsAndSumsSucceededPayments() {
        UUID doctorProfileId = UUID.randomUUID();
        when(clinicRepository.findByAdminUserId(adminUserId)).thenReturn(Optional.of(clinic));
        when(clinicStaffMembershipRepository.findAllByClinicId(clinicId))
                .thenReturn(List.of(new ClinicStaffMembership(clinicId, doctorProfileId)));

        Instant start = Instant.now();
        Appointment confirmed = new Appointment(UUID.randomUUID(), doctorProfileId, UUID.randomUUID(), start,
                start.plusSeconds(1800), LocationType.IN_PERSON);
        confirmed.confirm();
        Appointment completed = new Appointment(UUID.randomUUID(), doctorProfileId, UUID.randomUUID(), start,
                start.plusSeconds(1800), LocationType.VIDEO);
        completed.confirm();
        completed.complete();
        when(appointmentRepository.findAllByDoctorProfileIdInAndStatusIn(any(), any()))
                .thenReturn(List.of(confirmed, completed));

        Payment succeeded1 = new Payment(confirmed.getId(), new BigDecimal("200.00"), "key-1");
        succeeded1.succeed("CMI-1");
        Payment succeeded2 = new Payment(completed.getId(), new BigDecimal("150.50"), "key-2");
        succeeded2.succeed("CMI-2");
        when(paymentRepository.findAllByAppointmentIdInAndStatus(any(), any()))
                .thenReturn(List.of(succeeded1, succeeded2));

        ClinicDashboardResponse response = service.getDashboard(adminPrincipal());

        assertThat(response.bookingVolume()).isEqualTo(2);
        assertThat(response.revenueMad()).isEqualByComparingTo(new BigDecimal("350.50"));
    }
}
