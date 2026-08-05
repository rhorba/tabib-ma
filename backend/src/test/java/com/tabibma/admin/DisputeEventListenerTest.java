package com.tabibma.admin;

import com.tabibma.booking.AppointmentNoShowEvent;
import com.tabibma.booking.AppointmentPaymentFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeEventListenerTest {

    @Mock
    private DisputeService disputeService;

    private DisputeEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DisputeEventListener(disputeService);
    }

    @Test
    void onAppointmentNoShow_autoFilesANoShowDispute() {
        UUID appointmentId = UUID.randomUUID();

        listener.onAppointmentNoShow(new AppointmentNoShowEvent(appointmentId));

        verify(disputeService).createSystem(appointmentId, DisputeType.NO_SHOW);
    }

    @Test
    void onAppointmentPaymentFailed_autoFilesAPaymentIssueDispute() {
        UUID appointmentId = UUID.randomUUID();

        listener.onAppointmentPaymentFailed(new AppointmentPaymentFailedEvent(appointmentId));

        verify(disputeService).createSystem(appointmentId, DisputeType.PAYMENT_ISSUE);
    }

    @Test
    void onAppointmentNoShow_swallowsUnexpectedFailuresInsteadOfPropagating() {
        UUID appointmentId = UUID.randomUUID();
        when(disputeService.createSystem(appointmentId, DisputeType.NO_SHOW))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> listener.onAppointmentNoShow(new AppointmentNoShowEvent(appointmentId)))
                .doesNotThrowAnyException();
    }

    @Test
    void onAppointmentPaymentFailed_swallowsUnexpectedFailuresInsteadOfPropagating() {
        UUID appointmentId = UUID.randomUUID();
        when(disputeService.createSystem(appointmentId, DisputeType.PAYMENT_ISSUE))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> listener.onAppointmentPaymentFailed(new AppointmentPaymentFailedEvent(appointmentId)))
                .doesNotThrowAnyException();
    }
}
