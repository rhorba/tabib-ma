package com.tabibma.booking;

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
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full-stack proof of Story 4.4: cancel through the real HTTP+DB stack and confirm the refund
 * actually lands on the Payment row, not just on the in-memory Appointment returned to the caller. */
class CancellationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void cancel_wellWithinWindow_cancelsAndRefunds() throws Exception {
        String doctorEmail = "cancel-doctor1@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        approve(createProfile(doctorToken, "Cardiology", "Rabat"));

        // A slot 8+ days out is always > 24h away regardless of which day-of-week runs today.
        DayOfWeek dayOfWeek = DayOfWeek.SUNDAY;
        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """.formatted(dayOfWeek)))
                .andExpect(status().isCreated());
        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE).plusDays(8);
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
        String slotId = slots.get(0).get("id").asText();

        String patientEmail = "cancel-patient1@example.com";
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

        mockMvc.perform(post("/api/v1/booking/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        boolean refunded = paymentRepository.findByAppointmentId(UUID.fromString(appointmentId))
                .map(payment -> payment.getStatus() == PaymentStatus.REFUNDED)
                .orElse(false);
        assertThat(refunded).as("payment should be REFUNDED after a well-within-window cancellation").isTrue();
    }

    @Test
    void cancel_rejectsWhenNotTheOwningPatient() throws Exception {
        String doctorEmail = "cancel-doctor2@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        approve(createProfile(doctorToken, "Dermatology", "Casablanca"));
        DayOfWeek dayOfWeek = DayOfWeek.SATURDAY;
        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """.formatted(dayOfWeek)))
                .andExpect(status().isCreated());
        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE).plusDays(8);
        LocalDate targetDate = from.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        LocalDate to = targetDate.plusDays(1);
        var generateResult = mockMvc.perform(post("/api/v1/booking/availability/generate")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"%s\",\"toDate\":\"%s\"}".formatted(from, to)))
                .andExpect(status().isOk())
                .andReturn();
        String slotId = objectMapper.readTree(generateResult.getResponse().getContentAsString()).get(0).get("id").asText();

        String ownerEmail = "cancel-patient2@example.com";
        registerAndLogin(ownerEmail, "PATIENT");
        String ownerToken = login(ownerEmail);
        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andReturn();
        String appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();

        String attackerEmail = "cancel-patient3@example.com";
        registerAndLogin(attackerEmail, "PATIENT");
        String attackerToken = login(attackerEmail);
        mockMvc.perform(post("/api/v1/booking/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
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

    private String createProfile(String token, String specialty, String city) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"%s","bio":"bio","consultationFeeMad":150.00,"city":"%s"}
                                """.formatted(specialty, city)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus())
                .as("createProfile response was not 201 Created; body=%s", body)
                .isEqualTo(201);
        return objectMapper.readTree(body).get("id").asText();
    }

    private void approve(String profileId) throws Exception {
        String adminEmail = "platform-admin-cancel-" + profileId + "@example.com";
        User admin = new User(adminEmail, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        String adminToken = login(adminEmail);

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
