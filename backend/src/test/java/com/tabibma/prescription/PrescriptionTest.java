package com.tabibma.prescription;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionTest {

    @Test
    void constructor_copiesTheItemsListDefensively() {
        List<PrescriptionItem> items = new java.util.ArrayList<>(
                List.of(new PrescriptionItem("Amoxicillin", "500mg", "3x/day for 7 days")));
        Prescription prescription = new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                items, "key", Instant.now());

        items.add(new PrescriptionItem("Ibuprofen", "200mg", null));

        assertThat(prescription.getItems()).hasSize(1);
    }

    @Test
    void constructor_setsAllFieldsAndLeavesSupersedesIdNullForANewPrescription() {
        UUID consultationId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Instant signedAt = Instant.now();

        Prescription prescription = new Prescription(consultationId, doctorId, patientId, null,
                List.of(new PrescriptionItem("Amoxicillin", "500mg", null)), "key", signedAt);

        assertThat(prescription.getConsultationId()).isEqualTo(consultationId);
        assertThat(prescription.getDoctorId()).isEqualTo(doctorId);
        assertThat(prescription.getPatientId()).isEqualTo(patientId);
        assertThat(prescription.getSupersedesId()).isNull();
        assertThat(prescription.getPdfStorageKey()).isEqualTo("key");
        assertThat(prescription.getSignedAt()).isEqualTo(signedAt);
    }

    @Test
    void constructor_setsSupersedesIdForACorrection() {
        UUID originalId = UUID.randomUUID();

        Prescription correction = new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                originalId, List.of(new PrescriptionItem("Amoxicillin", "250mg", null)), "key", Instant.now());

        assertThat(correction.getSupersedesId()).isEqualTo(originalId);
    }
}
