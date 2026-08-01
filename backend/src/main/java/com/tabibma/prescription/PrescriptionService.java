package com.tabibma.prescription;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.User;
import com.tabibma.identity.UserContext;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.storage.ObjectStorageClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Story 7.1 (signed, immutable PDF) + Story 7.2 (ownership-only access). {@code issue} is called
 * internally by the consultation module's completion flow (Story 6.3) with already-authorized
 * doctorId/patientId — it does no authorization itself, same trust boundary as
 * PaymentService.capturePayment. {@code correct} and {@code getById} are directly
 * controller-facing and do their own checks.
 */
@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ObjectStorageClient objectStorageClient;
    private final PrescriptionPdfGenerator pdfGenerator;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, ObjectStorageClient objectStorageClient,
                                PrescriptionPdfGenerator pdfGenerator, UserRepository userRepository,
                                DoctorProfileRepository doctorProfileRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.objectStorageClient = objectStorageClient;
        this.pdfGenerator = pdfGenerator;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Transactional
    public Prescription issue(UUID consultationId, UUID doctorId, UUID patientId, List<PrescriptionItem> items) {
        return generateAndSave(consultationId, doctorId, patientId, null, items);
    }

    @Transactional
    public Prescription correct(UserContext principal, UUID originalPrescriptionId, List<PrescriptionItem> items) {
        Prescription original = prescriptionRepository.findById(originalPrescriptionId)
                .orElseThrow(() -> new NotFoundException("Prescription not found."));
        if (!original.getDoctorId().equals(principal.userId())) {
            throw new ForbiddenException("Only the prescribing doctor can issue a correction.");
        }
        return generateAndSave(original.getConsultationId(), original.getDoctorId(), original.getPatientId(),
                original.getId(), items);
    }

    public Prescription getById(UserContext principal, UUID prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new NotFoundException("Prescription not found."));
        assertOwner(principal, prescription);
        return prescription;
    }

    public InputStream loadPdf(UserContext principal, UUID prescriptionId) {
        Prescription prescription = getById(principal, prescriptionId);
        return objectStorageClient.load(prescription.getPdfStorageKey());
    }

    /** "My Prescriptions" (UX doc §3, patient nav) — scoped by patientId only, same
     * no-role-check-needed pattern as BookingService.listMyAppointments: a caller only ever gets
     * back rows where they are the patient. */
    public List<Prescription> getMine(UserContext principal) {
        return prescriptionRepository.findAllByPatientId(principal.userId());
    }

    private void assertOwner(UserContext principal, Prescription prescription) {
        boolean isPatient = prescription.getPatientId().equals(principal.userId());
        boolean isDoctor = prescription.getDoctorId().equals(principal.userId());
        if (!isPatient && !isDoctor) {
            throw new ForbiddenException("You do not have access to this prescription.");
        }
    }

    private Prescription generateAndSave(UUID consultationId, UUID doctorId, UUID patientId, UUID supersedesId,
                                          List<PrescriptionItem> items) {
        User doctorUser = userRepository.findById(doctorId).orElseThrow(() -> new NotFoundException("Doctor not found."));
        User patientUser = userRepository.findById(patientId).orElseThrow(() -> new NotFoundException("Patient not found."));
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));

        Instant signedAt = Instant.now();
        byte[] pdf = pdfGenerator.generate(fullName(doctorUser), doctorProfile.getSpecialty(), fullName(patientUser),
                signedAt, items);

        String storageKey;
        try (InputStream content = new ByteArrayInputStream(pdf)) {
            storageKey = objectStorageClient.store("prescriptions/" + patientId, "prescription.pdf", content,
                    pdf.length, "application/pdf");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store prescription PDF.", e);
        }

        Prescription prescription = new Prescription(consultationId, doctorId, patientId, supersedesId, items,
                storageKey, signedAt);
        return prescriptionRepository.save(prescription);
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
