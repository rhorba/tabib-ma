package com.tabibma.admin;

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
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Story 10.1. Reuses ReviewControllerIntegrationTest's registerDoctorWithSlot/bookOnlyOpenSlot
 * pattern to get a real CONFIRMED appointment via the booking API. */
class DisputeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String doctorProfileId;

    @Test
    void report_rejectsAnAppointmentTheCallerDoesNotOwn() throws Exception {
        String doctorToken = registerDoctorWithSlot("dispute-doctor1@example.com", DayOfWeek.MONDAY);
        registerAndLogin("dispute-patient1@example.com", "PATIENT");
        String ownerToken = login("dispute-patient1@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, ownerToken);

        registerAndLogin("dispute-patient2@example.com", "PATIENT");
        String strangerToken = login("dispute-patient2@example.com");

        mockMvc.perform(post("/api/v1/disputes")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"type\":\"COMPLAINT\",\"reason\":\"n/a\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void report_rejectsTheNoShowType() throws Exception {
        String doctorToken = registerDoctorWithSlot("dispute-doctor2@example.com", DayOfWeek.TUESDAY);
        registerAndLogin("dispute-patient3@example.com", "PATIENT");
        String patientToken = login("dispute-patient3@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        mockMvc.perform(post("/api/v1/disputes")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"type\":\"NO_SHOW\",\"reason\":\"n/a\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOpen_rejectsNonPlatformAdmin() throws Exception {
        registerAndLogin("dispute-doctor3@example.com", "DOCTOR");
        String doctorToken = login("dispute-doctor3@example.com");

        mockMvc.perform(get("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void report_thenTheDisputeAppearsInTheAdminQueueWithFullContext() throws Exception {
        String doctorToken = registerDoctorWithSlot("dispute-doctor4@example.com", DayOfWeek.WEDNESDAY);
        registerAndLogin("dispute-patient4@example.com", "PATIENT");
        String patientToken = login("dispute-patient4@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        mockMvc.perform(post("/api/v1/disputes")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"type\":\"PAYMENT_ISSUE\",\"reason\":\"Charged twice\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isCreated());

        String adminToken = createPlatformAdminAndLogin("dispute-admin1@example.com");
        mockMvc.perform(get("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')]").exists())
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')].reason")
                        .value(org.hamcrest.Matchers.hasItem("Charged twice")))
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')].type")
                        .value(org.hamcrest.Matchers.hasItem("PAYMENT_ISSUE")))
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')].patientName")
                        .value(org.hamcrest.Matchers.hasItem("A B")))
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')].doctorName")
                        .value(org.hamcrest.Matchers.hasItem("A B")));
    }

    @Test
    void resolve_marksResolvedAndRemovesItFromTheOpenQueue() throws Exception {
        String doctorToken = registerDoctorWithSlot("dispute-doctor5@example.com", DayOfWeek.THURSDAY);
        registerAndLogin("dispute-patient5@example.com", "PATIENT");
        String patientToken = login("dispute-patient5@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);
        mockMvc.perform(post("/api/v1/disputes")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"type\":\"COMPLAINT\",\"reason\":\"Late arrival\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isCreated());

        String adminToken = createPlatformAdminAndLogin("dispute-admin2@example.com");
        var listResult = mockMvc.perform(get("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        JsonNode disputes = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String disputeId = null;
        for (JsonNode node : disputes) {
            if (node.get("appointmentId").asText().equals(appointmentId)) {
                disputeId = node.get("id").asText();
            }
        }
        assertThat(disputeId).as("dispute for appointment %s: %s", appointmentId, disputes).isNotNull();

        mockMvc.perform(post("/api/v1/admin/platform/disputes/" + disputeId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + disputeId + "')]").doesNotExist());
    }

    @Test
    void resolve_rejectsNonPlatformAdmin() throws Exception {
        registerAndLogin("dispute-doctor6@example.com", "DOCTOR");
        String doctorToken = login("dispute-doctor6@example.com");

        mockMvc.perform(post("/api/v1/admin/platform/disputes/" + UUID.randomUUID() + "/resolve")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createManual_allowsAdminToLogANoShowDisputeDirectly() throws Exception {
        String doctorToken = registerDoctorWithSlot("dispute-doctor7@example.com", DayOfWeek.FRIDAY);
        registerAndLogin("dispute-patient6@example.com", "PATIENT");
        String patientToken = login("dispute-patient6@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        String adminToken = createPlatformAdminAndLogin("dispute-admin3@example.com");
        mockMvc.perform(post("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"type\":\"NO_SHOW\",\"reason\":\"Reported by phone\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')].type")
                        .value(org.hamcrest.Matchers.hasItem("NO_SHOW")));
    }

    private String createPlatformAdminAndLogin(String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        return login(email);
    }

    private void approveProfile(String profileId) throws Exception {
        String adminEmail = "platform-admin-dispute-" + profileId + "@example.com";
        User admin = new User(adminEmail, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        String adminToken = login(adminEmail);

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String bookOnlyOpenSlot(String doctorToken, String patientToken) throws Exception {
        var slotsResult = mockMvc.perform(get("/api/v1/booking/availability/slots")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("doctorProfileId", doctorProfileId)
                        .param("from", java.time.Instant.now().minusSeconds(60L * 60 * 24 * 30).toString())
                        .param("to", java.time.Instant.now().plusSeconds(60L * 60 * 24 * 60).toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode slotsNode = objectMapper.readTree(slotsResult.getResponse().getContentAsString());
        assertThat(slotsNode).as("open slots response: %s", slotsResult.getResponse().getContentAsString()).isNotEmpty();
        String slotId = slotsNode.get(0).get("id").asText();

        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerDoctorWithSlot(String doctorEmail, DayOfWeek dayOfWeek) throws Exception {
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);

        var profileResult = mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"Cardiology","bio":"bio","consultationFeeMad":150.00,"city":"Rabat"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        doctorProfileId = objectMapper.readTree(profileResult.getResponse().getContentAsString()).get("id").asText();
        approveProfile(doctorProfileId);

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """.formatted(dayOfWeek)))
                .andExpect(status().isCreated());

        LocalDate from = LocalDate.now(ZoneId.of("Africa/Casablanca"));
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
        return doctorToken;
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
