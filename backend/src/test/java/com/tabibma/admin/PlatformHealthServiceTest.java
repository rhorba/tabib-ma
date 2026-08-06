package com.tabibma.admin;

import com.tabibma.admin.dto.PlatformHealthResponse;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.AppointmentStatus;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformHealthServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private PlatformHealthService service;

    @BeforeEach
    void setUp() {
        service = new PlatformHealthService(appointmentRepository, paymentRepository);
    }

    @Test
    void getHealth_returnsZeroesWhenNothingHasHappenedYet() {
        when(appointmentRepository.count()).thenReturn(0L);
        when(appointmentRepository.countByStatus(any())).thenReturn(0L);
        when(paymentRepository.countByStatus(any())).thenReturn(0L);

        PlatformHealthResponse response = service.getHealth();

        assertThat(response.totalAppointments()).isZero();
        assertThat(response.confirmedAppointments()).isZero();
        assertThat(response.succeededPayments()).isZero();
        assertThat(response.failedPayments()).isZero();
    }

    @Test
    void getHealth_aggregatesEachStatusIndependently() {
        when(appointmentRepository.count()).thenReturn(42L);
        when(appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED)).thenReturn(10L);
        when(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED)).thenReturn(5L);
        when(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED)).thenReturn(20L);
        when(appointmentRepository.countByStatus(AppointmentStatus.NO_SHOW)).thenReturn(3L);
        when(appointmentRepository.countByStatus(AppointmentStatus.PENDING_PAYMENT)).thenReturn(4L);
        when(paymentRepository.countByStatus(PaymentStatus.SUCCEEDED)).thenReturn(30L);
        when(paymentRepository.countByStatus(PaymentStatus.FAILED)).thenReturn(2L);
        when(paymentRepository.countByStatus(PaymentStatus.REFUNDED)).thenReturn(1L);

        PlatformHealthResponse response = service.getHealth();

        assertThat(response.totalAppointments()).isEqualTo(42);
        assertThat(response.confirmedAppointments()).isEqualTo(10);
        assertThat(response.cancelledAppointments()).isEqualTo(5);
        assertThat(response.completedAppointments()).isEqualTo(20);
        assertThat(response.noShowAppointments()).isEqualTo(3);
        assertThat(response.pendingPaymentAppointments()).isEqualTo(4);
        assertThat(response.succeededPayments()).isEqualTo(30);
        assertThat(response.failedPayments()).isEqualTo(2);
        assertThat(response.refundedPayments()).isEqualTo(1);
    }
}
