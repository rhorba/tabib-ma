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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Story 10.1 Batch 2: proves the real AFTER_COMMIT/REQUIRES_NEW round trip between
 * NoShowService (booking) and DisputeEventListener (admin) — the one test that would catch
 * a regression to the exact REQUIRES_NEW bug class ConsultationBookingListener's javadoc warns
 * about (see .logs/activity.md 2026-08-05). Mirrors ReviewControllerIntegrationTest's pattern of
 * forcing appointment state directly via the repository rather than waiting for real time to pass. */
class NoShowDisputeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String doctorProfileId;

    @Test
    void markNoShow_autoFilesADisputeVisibleInTheAdminQueue() throws Exception {
        String doctorToken = registerDoctorWithSlot("noshow-doctor1@example.com", DayOfWeek.MONDAY);
        registerAndLogin("noshow-patient1@example.com", "PATIENT");
        String patientToken = login("noshow-patient1@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);
        forceStarted(appointmentId);

        mockMvc.perform(post("/api/v1/booking/appointments/" + appointmentId + "/no-show")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        String adminToken = createPlatformAdminAndLogin("noshow-admin1@example.com");
        mockMvc.perform(get("/api/v1/admin/platform/disputes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.appointmentId=='" + appointmentId + "')].type")
                        .value(org.hamcrest.Matchers.hasItem("NO_SHOW")));
    }

    private void forceStarted(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(UUID.fromString(appointmentId)).orElseThrow();
        ReflectionTestUtils.setField(appointment, "startsAt", Instant.now().minusSeconds(3600));
        appointmentRepository.save(appointment);
    }

    private String createPlatformAdminAndLogin(String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        return login(email);
    }

    private String bookOnlyOpenSlot(String doctorToken, String patientToken) throws Exception {
        var slotsResult = mockMvc.perform(get("/api/v1/booking/availability/slots")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("doctorProfileId", doctorProfileId)
                        .param("from", Instant.now().minusSeconds(60L * 60 * 24 * 30).toString())
                        .param("to", Instant.now().plusSeconds(60L * 60 * 24 * 60).toString()))
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

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """.formatted(dayOfWeek)))
                .andExpect(status().isCreated());

        LocalDate from = LocalDate.now(ZoneId.of("Africa/Casablanca"));
        LocalDate targetDate = from.with(TemporalAdjusters.next(dayOfWeek));
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
