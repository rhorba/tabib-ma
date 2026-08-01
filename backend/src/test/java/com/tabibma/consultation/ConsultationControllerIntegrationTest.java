package com.tabibma.consultation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slots generated through the normal availability-generation flow always land in the future (the
 * earliest is "next occurrence of a weekday"), so a freshly-booked appointment is naturally still
 * outside the ±10min join window when these tests run — exercising Story 6.1's "join window
 * enforcement" AC from the too-early side. The successful-join / in-progress path is covered at
 * the unit level (ConsultationServiceTest) and end to end in the Playwright suite (Batch 7).
 */
class ConsultationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void bookingAVideoAppointment_createsAJoinableFalseConsultation() throws Exception {
        String doctorEmail = "consult-doctor1@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String appointmentId = bookVideoAppointment(doctorToken, DayOfWeek.MONDAY, "consult-patient1@example.com");

        mockMvc.perform(get("/api/v1/consultations/by-appointment/" + appointmentId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.joinable").value(false));
    }

    @Test
    void getByAppointment_rejectsSomeoneWhoIsNotAParticipant() throws Exception {
        String doctorEmail = "consult-doctor2@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String appointmentId = bookVideoAppointment(doctorToken, DayOfWeek.TUESDAY, "consult-patient2@example.com");

        String strangerEmail = "consult-stranger1@example.com";
        registerAndLogin(strangerEmail, "PATIENT");
        String strangerToken = login(strangerEmail);

        mockMvc.perform(get("/api/v1/consultations/by-appointment/" + appointmentId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void join_rejectedBeforeTheJoinWindowOpens() throws Exception {
        String doctorEmail = "consult-doctor3@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String appointmentId = bookVideoAppointment(doctorToken, DayOfWeek.WEDNESDAY, "consult-patient3@example.com");
        String consultationId = getConsultationId(doctorToken, appointmentId);

        mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/join")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void join_rejectsSomeoneWhoIsNotAParticipant() throws Exception {
        String doctorEmail = "consult-doctor4@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String appointmentId = bookVideoAppointment(doctorToken, DayOfWeek.THURSDAY, "consult-patient4@example.com");
        String consultationId = getConsultationId(doctorToken, appointmentId);

        String strangerEmail = "consult-stranger2@example.com";
        registerAndLogin(strangerEmail, "PATIENT");
        String strangerToken = login(strangerEmail);

        mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/join")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }

    private String getConsultationId(String token, String appointmentId) throws Exception {
        var result = mockMvc.perform(get("/api/v1/consultations/by-appointment/" + appointmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String bookVideoAppointment(String doctorToken, DayOfWeek dayOfWeek, String patientEmail) throws Exception {
        String slotId = createSingleOpenVideoSlot(doctorToken, dayOfWeek);
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);

        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();
        return objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();
    }

    private String createSingleOpenVideoSlot(String doctorToken, DayOfWeek dayOfWeek) throws Exception {
        createProfile(doctorToken, "Cardiology", "Rabat");

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"VIDEO"}
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
}
