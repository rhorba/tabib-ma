package com.tabibma.booking;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void book_rejectsDoctorRole() throws Exception {
        String doctorEmail = "booking-doctor1@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String slotId = createSingleOpenSlot(doctorToken, DayOfWeek.MONDAY);

        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void book_confirmsAppointmentOnSuccessfulMockPaymentAndListsIt() throws Exception {
        String doctorEmail = "booking-doctor2@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String slotId = createSingleOpenSlot(doctorToken, DayOfWeek.TUESDAY);

        String patientEmail = "booking-patient1@example.com";
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

        mockMvc.perform(get("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(appointmentId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void book_rejectsSecondPatientBookingTheSameSlot() throws Exception {
        String doctorEmail = "booking-doctor3@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String slotId = createSingleOpenSlot(doctorToken, DayOfWeek.WEDNESDAY);

        String firstPatientEmail = "booking-patient2@example.com";
        registerAndLogin(firstPatientEmail, "PATIENT");
        String firstPatientToken = login(firstPatientEmail);
        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + firstPatientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated());

        String secondPatientEmail = "booking-patient3@example.com";
        registerAndLogin(secondPatientEmail, "PATIENT");
        String secondPatientToken = login(secondPatientEmail);
        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + secondPatientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isConflict());
    }

    @Test
    void listMine_returnsTheDoctorsOwnAppointmentsNotAnotherDoctors() throws Exception {
        String doctorEmail = "booking-doctor4@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String slotId = createSingleOpenSlot(doctorToken, DayOfWeek.THURSDAY);

        String patientEmail = "booking-patient4@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andReturn();
        String appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(appointmentId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        String otherDoctorEmail = "booking-doctor5@example.com";
        registerAndLogin(otherDoctorEmail, "DOCTOR");
        String otherDoctorToken = login(otherDoctorEmail);
        createProfile(otherDoctorToken, "Dermatology", "Marrakech");

        mockMvc.perform(get("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + otherDoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    /** Creates a doctor profile, one Monday-only rule, and generates exactly one open slot for the
     * next occurrence of the given day of week. */
    private String createSingleOpenSlot(String doctorToken, DayOfWeek dayOfWeek) throws Exception {
        createProfile(doctorToken, "Cardiology", "Rabat");

        mockMvc.perform(post("/api/v1/booking/availability/rules")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":"%s","startTime":"09:00:00","endTime":"09:30:00","slotDurationMinutes":30,"locationType":"IN_PERSON"}
                                """.formatted(dayOfWeek)))
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
}
