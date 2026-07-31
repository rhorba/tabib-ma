package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    List<AvailabilitySlot> findAllByDoctorProfileIdAndStartsAtBetweenAndBookedFalseOrderByStartsAt(
            UUID doctorProfileId, Instant from, Instant to);

    @Query("select s.startsAt from AvailabilitySlot s where s.doctorProfileId = :doctorProfileId "
            + "and s.startsAt >= :from and s.startsAt < :to")
    Set<Instant> findStartTimesBetween(@Param("doctorProfileId") UUID doctorProfileId,
                                        @Param("from") Instant from, @Param("to") Instant to);
}
