package com.tabibma.prescription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findAllByPatientId(UUID patientId);

    List<Prescription> findAllByConsultationId(UUID consultationId);
}
