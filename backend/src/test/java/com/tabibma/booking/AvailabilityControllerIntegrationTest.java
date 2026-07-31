package com.tabibma.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRule_rejectsPatientRole() throws Exception {
        String email = "availability-patient1@example.com";
        registerAndLogin(email, "PATIENT");
        String token = login(email);

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"MONDAY","startTime":"09:00:00","endTime":"12:00:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRule_allowsDoctor() throws Exception {
        String email = "availability-doctor1@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        createProfile(token, "Cardiology", "Rabat");

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"MONDAY","startTime":"09:00:00","endTime":"12:00:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRule_rejectsEndBeforeStart() throws Exception {
        String email = "availability-doctor2@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        createProfile(token, "Dermatology", "Casablanca");

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"TUESDAY","startTime":"12:00:00","endTime":"09:00:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createException_rejectsDuplicateDate() throws Exception {
        String email = "availability-doctor3@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        createProfile(token, "Pediatrics", "Fes");
        String date = LocalDate.now(AvailabilityService.CLINIC_ZONE).plusDays(10).toString();

        mockMvc.perform(post("/api/v1/booking/availability/exceptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exceptionDate\":\"%s\",\"reason\":\"holiday\"}".formatted(date)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/booking/availability/exceptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exceptionDate\":\"%s\",\"reason\":\"holiday again\"}".formatted(date)))
                .andExpect(status().isConflict());
    }

    @Test
    void generateSlots_skipsExceptionDateAndIsIdempotent() throws Exception {
        String email = "availability-doctor4@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        createProfile(token, "Neurology", "Marrakesh");

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"WEDNESDAY","startTime":"09:00:00","endTime":"10:00:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """))
                .andExpect(status().isCreated());

        LocalDate from = LocalDate.now(AvailabilityService.CLINIC_ZONE);
        LocalDate firstWednesday = from.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        LocalDate secondWednesday = firstWednesday.plusWeeks(1);
        LocalDate to = secondWednesday.plusDays(1);

        mockMvc.perform(post("/api/v1/booking/availability/exceptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exceptionDate\":\"%s\",\"reason\":\"blocked\"}".formatted(firstWednesday)))
                .andExpect(status().isCreated());

        var firstRun = mockMvc.perform(post("/api/v1/booking/availability/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"%s\",\"toDate\":\"%s\"}".formatted(from, to)))
                .andExpect(status().isOk())
                .andReturn();
        var firstSlots = objectMapper.readTree(firstRun.getResponse().getContentAsString());

        // Only the second Wednesday's two 30-minute slots should exist — the first Wednesday is excepted.
        assertThat(firstSlots).hasSize(2);
        for (var slot : firstSlots) {
            String startsAt = slot.get("startsAt").asText();
            assertThat(startsAt).doesNotContain(firstWednesday.toString());
        }

        // Re-running generate for the same window must not create duplicates (idempotent).
        var secondRun = mockMvc.perform(post("/api/v1/booking/availability/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"%s\",\"toDate\":\"%s\"}".formatted(from, to)))
                .andExpect(status().isOk())
                .andReturn();
        var secondSlots = objectMapper.readTree(secondRun.getResponse().getContentAsString());
        assertThat(secondSlots).isEmpty();
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
}
