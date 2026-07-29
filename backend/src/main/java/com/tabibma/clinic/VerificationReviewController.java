package com.tabibma.clinic;

import com.tabibma.clinic.dto.DoctorProfileResponse;
import com.tabibma.clinic.dto.VerificationDocumentResponse;
import com.tabibma.identity.UserContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Path lives under /api/v1/admin/platform, already restricted to PLATFORM_ADMIN by SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin/platform/verification-queue")
public class VerificationReviewController {

    private final VerificationReviewService verificationReviewService;

    public VerificationReviewController(VerificationReviewService verificationReviewService) {
        this.verificationReviewService = verificationReviewService;
    }

    @GetMapping
    public List<DoctorProfileResponse> listPending() {
        return verificationReviewService.listPendingProfiles().stream()
                .map(DoctorProfileResponse::from)
                .toList();
    }

    @GetMapping("/{doctorProfileId}/documents")
    public List<VerificationDocumentResponse> listDocuments(@PathVariable UUID doctorProfileId) {
        return verificationReviewService.listDocuments(doctorProfileId).stream()
                .map(VerificationDocumentResponse::from)
                .toList();
    }

    @PostMapping("/{doctorProfileId}/approve")
    public DoctorProfileResponse approve(@AuthenticationPrincipal UserContext principal, @PathVariable UUID doctorProfileId) {
        return DoctorProfileResponse.from(verificationReviewService.approve(principal, doctorProfileId));
    }

    @PostMapping("/{doctorProfileId}/reject")
    public DoctorProfileResponse reject(@AuthenticationPrincipal UserContext principal, @PathVariable UUID doctorProfileId) {
        return DoctorProfileResponse.from(verificationReviewService.reject(principal, doctorProfileId));
    }
}
