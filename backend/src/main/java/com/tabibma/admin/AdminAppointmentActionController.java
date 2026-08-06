package com.tabibma.admin;

import com.tabibma.identity.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Story 10.2. Path lives under /api/v1/admin/platform, already restricted to PLATFORM_ADMIN by
 * SecurityConfig (same convention as AdminDisputeController/VerificationReviewController). */
@RestController
@RequestMapping("/api/v1/admin/platform/appointments")
public class AdminAppointmentActionController {

    private final AdminAppointmentActionService adminAppointmentActionService;

    public AdminAppointmentActionController(AdminAppointmentActionService adminAppointmentActionService) {
        this.adminAppointmentActionService = adminAppointmentActionService;
    }

    @PostMapping("/{appointmentId}/refund")
    public ResponseEntity<Void> refund(@AuthenticationPrincipal UserContext principal, @PathVariable UUID appointmentId) {
        adminAppointmentActionService.refund(principal, appointmentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{appointmentId}/force-cancel")
    public ResponseEntity<Void> forceCancel(@AuthenticationPrincipal UserContext principal, @PathVariable UUID appointmentId) {
        adminAppointmentActionService.forceCancel(principal, appointmentId);
        return ResponseEntity.ok().build();
    }
}
