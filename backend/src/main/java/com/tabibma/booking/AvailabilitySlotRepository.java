package com.tabibma.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    List<AvailabilitySlot> findAllByDoctorProfileIdAndStartsAtBetweenAndBookedFalseOrderByStartsAt(
            UUID doctorProfileId, Instant from, Instant to);

    @Query("select s.startsAt from AvailabilitySlot s where s.doctorProfileId = :doctorProfileId "
            + "and s.startsAt >= :from and s.startsAt < :to")
    Set<Instant> findStartTimesBetween(@Param("doctorProfileId") UUID doctorProfileId,
                                        @Param("from") Instant from, @Param("to") Instant to);

    /** ADR-4 (Architecture doc): row lock is the first line of defense against double-booking,
     * serializing concurrent reservation attempts on the same slot row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AvailabilitySlot s where s.id = :id")
    Optional<AvailabilitySlot> findByIdForUpdate(@Param("id") UUID id);
}
