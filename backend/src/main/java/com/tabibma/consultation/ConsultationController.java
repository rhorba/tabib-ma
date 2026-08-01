package com.tabibma.consultation;

import com.tabibma.consultation.dto.ConsultationResponse;
import com.tabibma.consultation.dto.JoinConsultationResponse;
import com.tabibma.identity.UserContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public ConsultationResponse getByAppointment(@AuthenticationPrincipal UserContext principal,
                                                  @PathVariable UUID appointmentId) {
        return ConsultationResponse.from(consultationService.getByAppointmentId(principal, appointmentId));
    }

    @PostMapping("/{consultationId}/join")
    public JoinConsultationResponse join(@AuthenticationPrincipal UserContext principal,
                                          @PathVariable UUID consultationId) {
        return JoinConsultationResponse.from(consultationService.join(principal, consultationId));
    }
}
