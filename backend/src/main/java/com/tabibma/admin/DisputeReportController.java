package com.tabibma.admin;

import com.tabibma.admin.dto.CreateDisputeRequest;
import com.tabibma.identity.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Story 10.1: a patient or doctor reporting a problem on their own appointment. Not under
 * /api/v1/admin/** — any authenticated role can reach it, ownership is checked in the service. */
@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeReportController {

    private final DisputeService disputeService;

    public DisputeReportController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<Void> report(@AuthenticationPrincipal UserContext principal,
                                        @Valid @RequestBody CreateDisputeRequest request) {
        disputeService.report(principal, request.appointmentId(), request.type(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
