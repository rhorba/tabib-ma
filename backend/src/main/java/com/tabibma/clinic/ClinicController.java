package com.tabibma.clinic;

import com.tabibma.clinic.dto.ClinicInvitationResponse;
import com.tabibma.clinic.dto.ClinicResponse;
import com.tabibma.clinic.dto.CreateClinicRequest;
import com.tabibma.clinic.dto.InviteDoctorRequest;
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

@RestController
@RequestMapping("/api/v1/clinic/clinics")
public class ClinicController {

    private final ClinicOnboardingService clinicOnboardingService;

    public ClinicController(ClinicOnboardingService clinicOnboardingService) {
        this.clinicOnboardingService = clinicOnboardingService;
    }

    @PostMapping
    public ResponseEntity<ClinicResponse> createClinic(@AuthenticationPrincipal UserContext principal,
                                                        @Valid @RequestBody CreateClinicRequest request) {
        Clinic clinic = clinicOnboardingService.createClinic(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClinicResponse.from(clinic));
    }

    @GetMapping("/me")
    public ClinicResponse getMyClinic(@AuthenticationPrincipal UserContext principal) {
        return ClinicResponse.from(clinicOnboardingService.getMyClinic(principal));
    }

    @PostMapping("/{clinicId}/invitations")
    public ResponseEntity<ClinicInvitationResponse> inviteDoctor(@AuthenticationPrincipal UserContext principal,
                                                                  @PathVariable UUID clinicId,
                                                                  @Valid @RequestBody InviteDoctorRequest request) {
        ClinicInvitation invitation = clinicOnboardingService.inviteDoctor(principal, clinicId, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClinicInvitationResponse.from(invitation));
    }

    @GetMapping("/{clinicId}/invitations")
    public List<ClinicInvitationResponse> listInvitations(@AuthenticationPrincipal UserContext principal,
                                                            @PathVariable UUID clinicId) {
        return clinicOnboardingService.listInvitations(principal, clinicId).stream()
                .map(ClinicInvitationResponse::from)
                .toList();
    }
}
