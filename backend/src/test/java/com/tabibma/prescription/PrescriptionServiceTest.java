package com.tabibma.prescription;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserContext;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.storage.ObjectStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private ObjectStorageClient objectStorageClient;
    @Mock
    private PrescriptionPdfGenerator pdfGenerator;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    private PrescriptionService service;

    private UUID doctorId;
    private UUID patientId;
    private UUID consultationId;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptionRepository, objectStorageClient, pdfGenerator, userRepository,
                doctorProfileRepository);
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        consultationId = UUID.randomUUID();
    }

    private void stubPeople() {
        when(userRepository.findById(doctorId))
                .thenReturn(Optional.of(new User("d@example.com", "hash", Role.DOCTOR, "Amina", "Tazi")));
        when(userRepository.findById(patientId))
                .thenReturn(Optional.of(new User("p@example.com", "hash", Role.PATIENT, "Youssef", "Alami")));
        when(doctorProfileRepository.findByUserId(doctorId))
                .thenReturn(Optional.of(new DoctorProfile(doctorId, "Cardiology", "bio", new BigDecimal("250"), "Rabat")));
        when(pdfGenerator.generate(anyString(), anyString(), anyString(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(objectStorageClient.store(anyString(), anyString(), any(), anyLong(), anyString())).thenReturn("storage-key");
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void issue_generatesAndStoresAPdfAndSavesTheNewPrescription() {
        stubPeople();
        List<PrescriptionItem> items = List.of(new PrescriptionItem("Amoxicillin", "500mg", null));

        Prescription result = service.issue(consultationId, doctorId, patientId, items);

        assertThat(result.getConsultationId()).isEqualTo(consultationId);
        assertThat(result.getDoctorId()).isEqualTo(doctorId);
        assertThat(result.getPatientId()).isEqualTo(patientId);
        assertThat(result.getSupersedesId()).isNull();
        assertThat(result.getPdfStorageKey()).isEqualTo("storage-key");
        verify(objectStorageClient).store(eq("prescriptions/" + patientId), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void correct_rejectsSomeoneOtherThanThePrescribingDoctor() {
        Prescription original = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "key", java.time.Instant.now());
        when(prescriptionRepository.findById(any())).thenReturn(Optional.of(original));
        UserContext stranger = new UserContext(UUID.randomUUID(), "x@example.com", Role.DOCTOR);

        assertThatThrownBy(() -> service.correct(stranger, UUID.randomUUID(), List.of()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void correct_createsANewPrescriptionReferencingTheOriginalAndLeavesItUnchanged() {
        stubPeople();
        Prescription original = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "original-key", java.time.Instant.now());
        UUID originalId = UUID.randomUUID();
        when(prescriptionRepository.findById(originalId)).thenReturn(Optional.of(original));
        UserContext doctor = new UserContext(doctorId, "d@example.com", Role.DOCTOR);
        List<PrescriptionItem> correctedItems = List.of(new PrescriptionItem("Amoxicillin", "250mg", "corrected dose"));

        Prescription correction = service.correct(doctor, originalId, correctedItems);

        assertThat(original.getSupersedesId()).isNull();
        assertThat(original.getItems()).extracting(PrescriptionItem::getDosage).containsExactly("500mg");
        assertThat(correction.getConsultationId()).isEqualTo(consultationId);
        assertThat(correction.getItems()).extracting(PrescriptionItem::getDosage).containsExactly("250mg");
    }

    @Test
    void getById_allowsThePatient() {
        Prescription prescription = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "key", java.time.Instant.now());
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));

        assertThat(service.getById(new UserContext(patientId, "p@example.com", Role.PATIENT), id)).isSameAs(prescription);
    }

    @Test
    void getById_allowsTheDoctor() {
        Prescription prescription = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "key", java.time.Instant.now());
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));

        assertThat(service.getById(new UserContext(doctorId, "d@example.com", Role.DOCTOR), id)).isSameAs(prescription);
    }

    @Test
    void getById_rejectsAnUnrelatedUser() {
        Prescription prescription = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "key", java.time.Instant.now());
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));
        UserContext stranger = new UserContext(UUID.randomUUID(), "x@example.com", Role.PATIENT);

        assertThatThrownBy(() -> service.getById(stranger, id)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getById_throwsNotFoundForAnUnknownPrescription() {
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(new UserContext(patientId, "p@example.com", Role.PATIENT), id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void loadPdf_streamsFromObjectStorageAfterAnOwnershipCheck() {
        Prescription prescription = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "storage-key", java.time.Instant.now());
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));
        var stream = new java.io.ByteArrayInputStream(new byte[]{9});
        when(objectStorageClient.load("storage-key")).thenReturn(stream);

        assertThat(service.loadPdf(new UserContext(patientId, "p@example.com", Role.PATIENT), id)).isSameAs(stream);
    }

    @Test
    void getMine_delegatesToRepositoryScopedByPatientId() {
        Prescription prescription = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "key", java.time.Instant.now());
        when(prescriptionRepository.findAllByPatientId(patientId)).thenReturn(List.of(prescription));

        assertThat(service.getMine(new UserContext(patientId, "p@example.com", Role.PATIENT)))
                .containsExactly(prescription);
    }
}
