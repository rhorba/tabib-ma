package com.tabibma.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Story 8.2 Batch 4. See ResourceUtilizationService's class javadoc for why this test lives in
 * the booking package rather than clinic. */
class ResourceUtilizationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getUtilization_throwsNotFoundForACallerWithNoClinic() throws Exception {
        registerAndLogin("utilization-doctor1@example.com", "DOCTOR");
        String token = login("utilization-doctor1@example.com");

        mockMvc.perform(get("/api/v1/clinic/clinics/resources/utilization")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUtilization_returnsAnIdleResourceWithNoAllocationsWhenNothingIsBooked() throws Exception {
        String adminToken = createClinicAdminAndLogin("utilization-admin1@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Utilisation 1", "Rabat");
        createResource(adminToken, clinicId, "ROOM", "Salle 1");

        mockMvc.perform(get("/api/v1/clinic/clinics/resources/utilization")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].resourceName").value("Salle 1"))
                .andExpect(jsonPath("$[0].allocations.length()").value(0));
    }

    @Test
    void getUtilization_showsARealBookingAsAnAllocationAndClearsItOnCancellation() throws Exception {
        String adminToken = createClinicAdminAndLogin("utilization-admin2@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Utilisation 2", "Casablanca");
        String resourceId = createResource(adminToken, clinicId, "ROOM", "Salle 2");

        String doctorEmail = "utilization-doctor2@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String slotId = createResourceScopedOpenSlot(doctorToken, DayOfWeek.WEDNESDAY, clinicId, resourceId);

        String patientEmail = "utilization-patient2@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andReturn();
        String appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/clinic/clinics/resources/utilization")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceId").value(resourceId))
                .andExpect(jsonPath("$[0].allocations.length()").value(1))
                .andExpect(jsonPath("$[0].allocations[0].appointmentId").value(appointmentId));

        mockMvc.perform(post("/api/v1/booking/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/clinic/clinics/resources/utilization")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allocations.length()").value(0));
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

        LocalDate today = LocalDate.now(AvailabilityService.CLINIC_ZONE);
        // Strictly-future "next" (not "nextOrSame") — if today happens to be dayOfWeek but the
        // fixed 09:00-09:30 window has already passed in clinic-local time by the moment this
        // runs, nextOrSame would generate an already-past slot, which ResourceUtilizationService
        // correctly excludes as a "no longer upcoming" allocation, failing the assertion below for
        // reasons that have nothing to do with the feature under test (same class of bug fixed in
        // the Epic 6/7 e2e suite on 2026-08-03). The generation window itself must start tomorrow,
        // not today, so it doesn't also pick up today's (possibly already-past) occurrence when
        // today happens to be dayOfWeek.
        LocalDate from = today.plusDays(1);
        LocalDate targetDate = today.with(TemporalAdjusters.next(dayOfWeek));
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
