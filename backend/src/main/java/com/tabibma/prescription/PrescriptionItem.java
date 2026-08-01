package com.tabibma.prescription;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Value object (Architecture doc §3) — no identity of its own, owned entirely by a Prescription. */
@Embeddable
public class PrescriptionItem {

    @Column(name = "medication_name", nullable = false)
    private String medicationName;

    @Column(nullable = false)
    private String dosage;

    @Column
    private String instructions;

    protected PrescriptionItem() {
        // JPA
    }

    public PrescriptionItem(String medicationName, String dosage, String instructions) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.instructions = instructions;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getInstructions() {
        return instructions;
    }
}
