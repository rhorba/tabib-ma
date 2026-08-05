package com.tabibma.admin;

import com.tabibma.admin.dto.CreateDisputeRequest;
import com.tabibma.admin.dto.DisputeResponse;
import com.tabibma.identity.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Path lives under /api/v1/admin/platform, already restricted to PLATFORM_ADMIN by SecurityConfig
 * (same convention as VerificationReviewController). */
@RestController
@RequestMapping("/api/v1/admin/platform/disputes")
public class AdminDisputeController {

    private final DisputeService disputeService;

    public AdminDisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<Void> createManual(@AuthenticationPrincipal UserContext principal,
                                              @Valid @RequestBody CreateDisputeRequest request) {
        disputeService.createManual(principal, request.appointmentId(), request.type(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public List<DisputeResponse> listOpen() {
        return disputeService.listOpen();
    }

    @PostMapping("/{disputeId}/resolve")
    public ResponseEntity<Void> resolve(@AuthenticationPrincipal UserContext principal, @PathVariable UUID disputeId) {
        disputeService.resolve(principal, disputeId);
        return ResponseEntity.ok().build();
    }
}
