package com.tabibma.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 8.2 Batch 3 — proves the AC's actual "prevents" language against a real Postgres, not
 * mocks (same rationale as DoubleBookingConcurrencyIntegrationTest): a resource required by two
 * overlapping IN_PERSON slots for *different* doctors must not be double-allocated, and cancelling
 * a booking must free its resource for the next patient.
 */
class ResourceAllocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ResourceAllocationGuard resourceAllocationGuard;

    @Autowired
    private AppointmentResourceAllocationRepository appointmentResourceAllocationRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Autowired
    private AvailabilitySlotResourceRepository availabilitySlotResourceRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void bookingAResourceScopedSlot_allocatesTheResourceAndCancellationReleasesIt() throws Exception {
        String adminToken = createClinicAdminAndLogin("resource-alloc-admin1@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Ressources", "Rabat");
        String resourceId = createResource(adminToken, clinicId, "ROOM", "Salle 1");

        String doctorEmail = "resource-alloc-doctor1@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String slotId = createResourceScopedOpenSlot(doctorToken, DayOfWeek.MONDAY, clinicId, resourceId);

        String patientEmail = "resource-alloc-patient1@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);

        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();
        String appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();

        assertThat(appointmentResourceAllocationRepository.findAllByAppointmentId(UUID.fromString(appointmentId)))
                .hasSize(1)
                .allSatisfy(allocation -> assertThat(allocation.getResourceId()).isEqualTo(UUID.fromString(resourceId)));

        mockMvc.perform(post("/api/v1/booking/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        assertThat(appointmentResourceAllocationRepository.findAllByAppointmentId(UUID.fromString(appointmentId)))
                .isEmpty();
    }

    @Test
    void bookingASecondOverlappingSlotForTheSameResourceButADifferentDoctor_isRejected() throws Exception {
        String adminToken = createClinicAdminAndLogin("resource-alloc-admin2@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Ressources", "Casablanca");
        String resourceId = createResource(adminToken, clinicId, "EQUIPMENT", "Echographe");

        String doctorAEmail = "resource-alloc-doctorA@example.com";
        registerAndLogin(doctorAEmail, "DOCTOR");
        String doctorAToken = login(doctorAEmail);
        String slotA = createResourceScopedOpenSlot(doctorAToken, DayOfWeek.TUESDAY, clinicId, resourceId);

        String doctorBEmail = "resource-alloc-doctorB@example.com";
        registerAndLogin(doctorBEmail, "DOCTOR");
        String doctorBToken = login(doctorBEmail);
        // Same day/time window as slotA (both rules run 09:00-09:30) — different doctor, different
        // appointments-table EXCLUDE key (doctor_profile_id), so only the resource-keyed EXCLUDE
        // constraint (V15) can catch this overlap.
        String slotB = createResourceScopedOpenSlot(doctorBToken, DayOfWeek.TUESDAY, clinicId, resourceId);

        String patientEmail = "resource-alloc-patient2@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotA)))
                .andExpect(status().isCreated());

        String otherPatientEmail = "resource-alloc-patient3@example.com";
        registerAndLogin(otherPatientEmail, "PATIENT");
        String otherPatientToken = login(otherPatientEmail);
        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + otherPatientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotB)))
                .andExpect(status().isConflict());
    }

    @Test
    void allocateForSlot_underConcurrentRequestsForTheSameResource_exactlyOneWins() throws Exception {
        String adminToken = createClinicAdminAndLogin("resource-alloc-admin3@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Ressources", "Fes");
        String resourceId = createResource(adminToken, clinicId, "ROOM", "Salle Concurrence");
        UUID resourceUuid = UUID.fromString(resourceId);

        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(1800);

        Object[] outcomes = raceTwoAllocations(resourceUuid, start, end);

        long succeeded = Arrays.stream(outcomes).filter(o -> o == null).count();
        long conflicts = Arrays.stream(outcomes).filter(o -> o instanceof ConflictException).count();
        assertThat(succeeded).as("outcomes=%s", Arrays.toString(outcomes)).isEqualTo(1);
        assertThat(conflicts).as("outcomes=%s", Arrays.toString(outcomes)).isEqualTo(1);
    }

    private Object[] raceTwoAllocations(UUID resourceId, Instant start, Instant end) throws Exception {
        // ResourceAllocationGuard.allocateForSlot looks up required resources via the slot id, so
        // this test seeds two real availability_slots rows (each requiring the same resource) and
        // two real appointments rows (appointment_resource_allocations FKs to appointments)
        // directly, rather than going through the full generate/book HTTP flow — it's exercising
        // the guard's own concurrency behaviour, not the booking orchestration (already covered by
        // the other two tests in this class).
        UUID doctorA = createDoctorProfile("resource-alloc-race-doctorA@example.com");
        UUID doctorB = createDoctorProfile("resource-alloc-race-doctorB@example.com");
        UUID patientA = createPatient("resource-alloc-race-patientA@example.com");
        UUID patientB = createPatient("resource-alloc-race-patientB@example.com");
        UUID slotIdA = seedSlotRequiringResource(doctorA, resourceId, start, end);
        UUID slotIdB = seedSlotRequiringResource(doctorB, resourceId, start, end);
        UUID appointmentA = appointmentRepository.save(
                new Appointment(patientA, doctorA, slotIdA, start, end, LocationType.IN_PERSON)).getId();
        UUID appointmentB = appointmentRepository.save(
                new Appointment(patientB, doctorB, slotIdB, start, end, LocationType.IN_PERSON)).getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<Object> taskA = attempt(ready, go,
                    () -> resourceAllocationGuard.allocateForSlot(appointmentA, slotIdA, start, end));
            Callable<Object> taskB = attempt(ready, go,
                    () -> resourceAllocationGuard.allocateForSlot(appointmentB, slotIdB, start, end));
            Future<Object> futureA = executor.submit(taskA);
            Future<Object> futureB = executor.submit(taskB);
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();
            return new Object[] {futureA.get(10, TimeUnit.SECONDS), futureB.get(10, TimeUnit.SECONDS)};
        } finally {
            executor.shutdown();
        }
    }

    private Callable<Object> attempt(CountDownLatch ready, CountDownLatch go, Runnable allocation) {
        return () -> {
            ready.countDown();
            go.await();
            try {
                allocation.run();
                return null;
            } catch (Exception e) {
                return e;
            }
        };
    }

    private UUID seedSlotRequiringResource(UUID doctorProfileId, UUID resourceId, Instant start, Instant end) {
        // AvailabilitySlotResource FKs to availability_slots/clinic_resources — a real slot row
        // (owned by a real doctor profile, per availability_slots' own FK) must exist even though
        // this test only cares about the allocation guard's concurrency behaviour.
        AvailabilitySlot slot = availabilitySlotRepository.save(
                new AvailabilitySlot(doctorProfileId, start, end, LocationType.IN_PERSON, null));
        availabilitySlotResourceRepository.save(new AvailabilitySlotResource(slot.getId(), resourceId));
        return slot.getId();
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

    private String createResourceScopedOpenSlot(String doctorToken, DayOfWeek dayOfWeek, String clinicId,
                                                  String resourceId) throws Exception {
        createProfile(doctorToken, "Cardiology", "Rabat");

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"IN_PERSON","clinicId":"%s","resourceIds":["%s"]}
                                """.formatted(dayOfWeek, clinicId, resourceId)))
                .andExpect(status().isCreated());

        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE);
        LocalDate targetDate = from.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        LocalDate to = targetDate.plusDays(1);

        var generateResult = mockMvc.perform(post("/api/v1/booking/availability/generate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"%s\",\"toDate\":\"%s\"}".formatted(from, to)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode slots = objectMapper.readTree(generateResult.getResponse().getContentAsString());
        assertThat(slots).hasSize(1);
        return slots.get(0).get("id").asText();
    }

    private void registerAndLogin(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery","role":"%s","firstName":"A","lastName":"B"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery"}
                                """.formatted(email)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus())
                .as("login response for %s was not 200 OK; body=%s", email, body)
                .isEqualTo(200);
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private void createProfile(String token, String specialty, String city) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"%s","bio":"bio","consultationFeeMad":150.00,"city":"%s"}
                                """.formatted(specialty, city)))
                .andReturn();
        assertThat(result.getResponse().getStatus())
                .as("createProfile response was not 201 Created; body=%s", result.getResponse().getContentAsString())
                .isEqualTo(201);
    }

    private String createClinicAdminAndLogin(String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("correcthorsebattery"), Role.CLINIC_ADMIN, "C", "A");
        userRepository.save(admin);
        return login(email);
    }

    private String createClinic(String token, String name, String city) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/clinics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","city":"%s"}
                                """.formatted(name, city)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createResource(String token, String clinicId, String type, String name) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"%s","name":"%s"}
                                """.formatted(type, name)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
