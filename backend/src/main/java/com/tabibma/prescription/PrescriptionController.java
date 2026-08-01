package com.tabibma.prescription;

import com.tabibma.identity.UserContext;
import com.tabibma.prescription.dto.CorrectPrescriptionRequest;
import com.tabibma.prescription.dto.PrescriptionResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/mine")
    public List<PrescriptionResponse> getMine(@AuthenticationPrincipal UserContext principal) {
        return prescriptionService.getMine(principal).stream().map(PrescriptionResponse::from).toList();
    }

    @GetMapping("/{prescriptionId}")
    public PrescriptionResponse getById(@AuthenticationPrincipal UserContext principal,
                                         @PathVariable UUID prescriptionId) {
        return PrescriptionResponse.from(prescriptionService.getById(principal, prescriptionId));
    }

    @GetMapping("/{prescriptionId}/pdf")
    public ResponseEntity<InputStreamResource> downloadPdf(@AuthenticationPrincipal UserContext principal,
                                                             @PathVariable UUID prescriptionId) {
        InputStreamResource body = new InputStreamResource(prescriptionService.loadPdf(principal, prescriptionId));
        ContentDisposition disposition = ContentDisposition.attachment().filename("prescription.pdf").build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    @PostMapping("/{prescriptionId}/correct")
    public PrescriptionResponse correct(@AuthenticationPrincipal UserContext principal,
                                         @PathVariable UUID prescriptionId,
                                         @Valid @RequestBody CorrectPrescriptionRequest request) {
        List<PrescriptionItem> items = request.items().stream()
                .map(i -> new PrescriptionItem(i.medicationName(), i.dosage(), i.instructions()))
                .toList();
        return PrescriptionResponse.from(prescriptionService.correct(principal, prescriptionId, items));
    }
}
