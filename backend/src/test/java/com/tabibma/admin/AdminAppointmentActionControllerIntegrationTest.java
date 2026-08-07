package com.tabibma.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Story 10.2. Reuses DisputeControllerIntegrationTest's registerDoctorWithSlot/bookOnlyOpenSlot
 * pattern to get a real CONFIRMED-and-paid appointment via the booking API (the mock CMI gateway
 * always succeeds, so booking always yields a SUCCEEDED payment). */
class AdminAppointmentActionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String doctorProfileId;

    @Test
    void refund_rejectsNonPlatformAdmin() throws Exception {
        registerAndLogin("action-doctor1@example.com", "DOCTOR");
        String doctorToken = login("action-doctor1@example.com");

        mockMvc.perform(post("/api/v1/admin/platform/appointments/" + UUID.randomUUID() + "/refund")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void forceCancel_rejectsNonPlatformAdmin() throws Exception {
        registerAndLogin("action-doctor2@example.com", "DOCTOR");
        String doctorToken = login("action-doctor2@example.com");

        mockMvc.perform(post("/api/v1/admin/platform/appointments/" + UUID.randomUUID() + "/force-cancel")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_marksTheSucceededPaymentRefunded() throws Exception {
        String doctorToken = registerDoctorWithSlot("action-doctor3@example.com", DayOfWeek.MONDAY);
        registerAndLogin("action-patient1@example.com", "PATIENT");
        String patientToken = login("action-patient1@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        String adminToken = createPlatformAdminAndLogin("action-admin1@example.com");
        mockMvc.perform(post("/api/v1/admin/platform/appointments/" + appointmentId + "/refund")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        boolean refunded = paymentRepository.findByAppointmentId(UUID.fromString(appointmentId))
                .map(payment -> payment.getStatus() == PaymentStatus.REFUNDED)
                .orElse(false);
        assertThat(refunded).as("payment should be REFUNDED after an admin-issued refund").isTrue();
    }

    @Test
    void refund_rejectsAnAppointmentWithoutASucceededPayment() throws Exception {
        String doctorToken = registerDoctorWithSlot("action-doctor4@example.com", DayOfWeek.TUESDAY);
        registerAndLogin("action-patient2@example.com", "PATIENT");
        String patientToken = login("action-patient2@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        String adminToken = createPlatformAdminAndLogin("action-admin2@example.com");
        // A first refund succeeds and moves the payment to REFUNDED...
        mockMvc.perform(post("/api/v1/admin/platform/appointments/" + appointmentId + "/refund")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // ...a second refund on the same appointment has no SUCCEEDED payment left to refund.
        mockMvc.perform(post("/api/v1/admin/platform/appointments/" + appointmentId + "/refund")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void forceCancel_cancelsTheAppointmentEvenThoughTheAdminIsNotTheOwningPatient() throws Exception {
        String doctorToken = registerDoctorWithSlot("action-doctor5@example.com", DayOfWeek.WEDNESDAY);
        registerAndLogin("action-patient3@example.com", "PATIENT");
        String patientToken = login("action-patient3@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        String adminToken = createPlatformAdminAndLogin("action-admin3@example.com");
        mockMvc.perform(post("/api/v1/admin/platform/appointments/" + appointmentId + "/force-cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        var listResult = mockMvc.perform(get("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode appointments = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String status = null;
        for (JsonNode node : appointments) {
            if (node.get("id").asText().equals(appointmentId)) {
                status = node.get("status").asText();
            }
        }
        assertThat(status).isEqualTo("CANCELLED");
    }

    private String createPlatformAdminAndLogin(String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        return login(email);
    }

    private void approveProfile(String profileId) throws Exception {
        String adminEmail = "platform-admin-action-" + profileId + "@example.com";
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

        LocalDate today = LocalDate.now(ZoneId.of("Africa/Casablanca"));
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
