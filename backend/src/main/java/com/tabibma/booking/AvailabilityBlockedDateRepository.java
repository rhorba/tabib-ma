package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityBlockedDateRepository extends JpaRepository<AvailabilityBlockedDate, UUID> {

    List<AvailabilityBlockedDate> findAllByDoctorProfileId(UUID doctorProfileId);

    boolean existsByDoctorProfileIdAndExceptionDate(UUID doctorProfileId, LocalDate exceptionDate);
}
