package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilitySlotResourceRepository extends JpaRepository<AvailabilitySlotResource, UUID> {

    List<AvailabilitySlotResource> findAllByAvailabilitySlotId(UUID availabilitySlotId);
}
