package com.tabibma.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findAllByStatusOrderByCreatedAtAsc(DisputeStatus status);

    List<Dispute> findAllByAppointmentIdAndStatus(UUID appointmentId, DisputeStatus status);
}
