package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateDoctorProfileRequest;
import com.tabibma.clinic.dto.DoctorProfileResponse;
import com.tabibma.clinic.dto.DoctorSearchResponse;
import com.tabibma.clinic.dto.VerificationDocumentResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinic/doctor-profiles")
public class DoctorProfileController {

    private final DoctorOnboardingService doctorOnboardingService;
    private final DoctorSearchService doctorSearchService;

    public DoctorProfileController(DoctorOnboardingService doctorOnboardingService,
                                    DoctorSearchService doctorSearchService) {
        this.doctorOnboardingService = doctorOnboardingService;
        this.doctorSearchService = doctorSearchService;
    }

    @GetMapping("/search")
    public DoctorSearchResponse search(@RequestParam(required = false) String specialty,
                                        @RequestParam(required = false) String city,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return doctorSearchService.search(specialty, city, page, size);
    }

    @PostMapping
    public ResponseEntity<DoctorProfileResponse> createProfile(@AuthenticationPrincipal UserContext principal,
                                                                 @Valid @RequestBody CreateDoctorProfileRequest request) {
        DoctorProfile profile = doctorOnboardingService.createProfile(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DoctorProfileResponse.from(profile));
    }

    @PostMapping("/{doctorProfileId}/documents")
    public ResponseEntity<VerificationDocumentResponse> uploadDocument(@AuthenticationPrincipal UserContext principal,
                                                                        @PathVariable UUID doctorProfileId,
                                                                        @RequestParam DocumentType documentType,
                                                                        @RequestPart MultipartFile file) {
        VerificationDocument document = doctorOnboardingService.uploadDocument(principal, doctorProfileId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(VerificationDocumentResponse.from(document));
    }

    @GetMapping("/me")
    public DoctorProfileResponse getMyProfile(@AuthenticationPrincipal UserContext principal) {
        return DoctorProfileResponse.from(doctorOnboardingService.getMyProfile(principal));
    }

    @GetMapping("/{doctorProfileId}/documents")
    public List<VerificationDocumentResponse> listMyDocuments(@AuthenticationPrincipal UserContext principal,
                                                                @PathVariable UUID doctorProfileId) {
        return doctorOnboardingService.listMyDocuments(principal, doctorProfileId).stream()
                .map(VerificationDocumentResponse::from)
                .toList();
    }
}
