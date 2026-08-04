package com.tabibma.clinic;

import com.tabibma.clinic.dto.ClinicResourceResponse;
import com.tabibma.clinic.dto.CreateClinicResourceRequest;
import com.tabibma.identity.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinic/clinics/{clinicId}/resources")
public class ClinicResourceController {

    private final ClinicResourceService clinicResourceService;

    public ClinicResourceController(ClinicResourceService clinicResourceService) {
        this.clinicResourceService = clinicResourceService;
    }

    @PostMapping
    public ResponseEntity<ClinicResourceResponse> createResource(@AuthenticationPrincipal UserContext principal,
                                                                   @PathVariable UUID clinicId,
                                                                   @Valid @RequestBody CreateClinicResourceRequest request) {
        ClinicResource resource = clinicResourceService.createResource(principal, clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClinicResourceResponse.from(resource));
    }

    @GetMapping
    public List<ClinicResourceResponse> listResources(@AuthenticationPrincipal UserContext principal,
                                                        @PathVariable UUID clinicId) {
        return clinicResourceService.listResources(principal, clinicId).stream()
                .map(ClinicResourceResponse::from)
                .toList();
    }

    @PatchMapping("/{resourceId}/deactivate")
    public ClinicResourceResponse deactivateResource(@AuthenticationPrincipal UserContext principal,
                                                       @PathVariable UUID clinicId,
                                                       @PathVariable UUID resourceId) {
        return ClinicResourceResponse.from(
                clinicResourceService.deactivateResource(principal, clinicId, resourceId));
    }
}
