package com.tabibma.clinic;

import com.tabibma.clinic.dto.ClinicInvitationResponse;
import com.tabibma.identity.UserContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Doctor-side of Story 2.3: view/accept/decline invitations addressed to the caller's own email. */
@RestController
@RequestMapping("/api/v1/clinic/invitations")
public class ClinicInvitationController {

    private final DoctorOnboardingService doctorOnboardingService;

    public ClinicInvitationController(DoctorOnboardingService doctorOnboardingService) {
        this.doctorOnboardingService = doctorOnboardingService;
    }

    @GetMapping("/me")
    public List<ClinicInvitationResponse> listMyPendingInvitations(@AuthenticationPrincipal UserContext principal) {
        return doctorOnboardingService.listMyPendingInvitations(principal).stream()
                .map(ClinicInvitationResponse::from)
                .toList();
    }

    @PostMapping("/{invitationId}/accept")
    public ClinicInvitationResponse accept(@AuthenticationPrincipal UserContext principal,
                                            @PathVariable UUID invitationId) {
        return ClinicInvitationResponse.from(doctorOnboardingService.acceptInvitation(principal, invitationId));
    }

    @PostMapping("/{invitationId}/decline")
    public ClinicInvitationResponse decline(@AuthenticationPrincipal UserContext principal,
                                             @PathVariable UUID invitationId) {
        return ClinicInvitationResponse.from(doctorOnboardingService.declineInvitation(principal, invitationId));
    }
}
