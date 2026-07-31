package com.tabibma.booking;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adversarial suite for Story 4.3 (the highest-risk story in the backlog) — proves both lines of
 * defense from ADR-4 under real concurrency, per docs/database-tabib-ma.md Section 10's explicit
 * ask for this test and docs/test-strategy-tabib-ma.md §5's "mocking the DB gives false confidence"
 * rule. Uses a real Postgres (AbstractIntegrationTest), not mocks.
 */
class DoubleBookingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DoubleBookingGuard doubleBookingGuard;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DoctorProfileRepository doctorProfileRepository;
    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void reserveSlot_underConcurrentRequestsForTheSameSlot_exactlyOneWins() throws Exception {
        UUID doctorProfileId = createDoctorProfile("concurrency-doctor1@example.com");
        Instant start = Instant.now().plusSeconds(3600);
        AvailabilitySlot slot = availabilitySlotRepository.save(
                new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null));
        UUID patientA = createPatient("concurrency-patient1@example.com");
        UUID patientB = createPatient("concurrency-patient2@example.com");

        Object[] outcomes = raceTwoReservations(patientA, slot.getId(), patientB, slot.getId());

        assertExactlyOneAppointmentAndOneConflict(outcomes);
        assertThat(appointmentRepository.findAllByAvailabilitySlotId(slot.getId())).hasSize(1);
    }

    @Test
    void reserveSlot_underConcurrentRequestsForOverlappingSlots_excludeConstraintCatchesTheSecond() throws Exception {
        UUID doctorProfileId = createDoctorProfile("concurrency-doctor2@example.com");
        Instant start = Instant.now().plusSeconds(7200);
        // Two DIFFERENT slot rows with overlapping (not identical — availability_slots has its own
        // UNIQUE(doctor_profile_id, starts_at) from V6) time ranges for the same doctor. Different
        // rows means no row-lock contention, so this is exactly what the appointments EXCLUDE
        // constraint exists to catch as the second line of defense.
        AvailabilitySlot slotA = availabilitySlotRepository.save(
                new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null));
        AvailabilitySlot slotB = availabilitySlotRepository.save(
                new AvailabilitySlot(doctorProfileId, start.plusSeconds(900), start.plusSeconds(2700), LocationType.VIDEO, null));
        UUID patientA = createPatient("concurrency-patient3@example.com");
        UUID patientB = createPatient("concurrency-patient4@example.com");

        Object[] outcomes = raceTwoReservations(patientA, slotA.getId(), patientB, slotB.getId());

        assertExactlyOneAppointmentAndOneConflict(outcomes);
        long appointmentsForDoctor = appointmentRepository.findAll().stream()
                .filter(a -> a.getDoctorProfileId().equals(doctorProfileId))
                .count();
        assertThat(appointmentsForDoctor).isEqualTo(1);
    }

    private Object[] raceTwoReservations(UUID patientA, UUID slotIdA, UUID patientB, UUID slotIdB) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<Object> taskA = attempt(ready, go, () -> doubleBookingGuard.reserveSlot(patientA, slotIdA));
            Callable<Object> taskB = attempt(ready, go, () -> doubleBookingGuard.reserveSlot(patientB, slotIdB));
            Future<Object> futureA = executor.submit(taskA);
            Future<Object> futureB = executor.submit(taskB);
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            return new Object[] {futureA.get(10, TimeUnit.SECONDS), futureB.get(10, TimeUnit.SECONDS)};
        } finally {
            executor.shutdown();
        }
    }

    private Callable<Object> attempt(CountDownLatch ready, CountDownLatch go, Callable<Appointment> reservation) {
        return () -> {
            ready.countDown();
            go.await();
            try {
                return reservation.call();
            } catch (Exception e) {
                return e;
            }
        };
    }

    private void assertExactlyOneAppointmentAndOneConflict(Object[] outcomes) {
        long succeeded = Arrays.stream(outcomes).filter(o -> o instanceof Appointment).count();
        long conflicts = Arrays.stream(outcomes).filter(o -> o instanceof ConflictException).count();
        assertThat(succeeded).as("outcomes=%s", Arrays.toString(outcomes)).isEqualTo(1);
        assertThat(conflicts).as("outcomes=%s", Arrays.toString(outcomes)).isEqualTo(1);
    }

    private UUID createDoctorProfile(String email) {
        User user = userRepository.save(new User(email, "n/a", Role.DOCTOR, "Doc", "Tor"));
        DoctorProfile profile = doctorProfileRepository.save(
                new DoctorProfile(user.getId(), "Cardiology", "bio", BigDecimal.TEN, "Rabat"));
        return profile.getId();
    }

    private UUID createPatient(String email) {
        return userRepository.save(new User(email, "n/a", Role.PATIENT, "Pat", "Ient")).getId();
    }
}
