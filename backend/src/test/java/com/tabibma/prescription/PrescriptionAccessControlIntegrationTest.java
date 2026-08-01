package com.tabibma.prescription;

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
 * Story 7.2 (ownership-only access), adversarial scenarios from Test Strategy §2+§3: "Patient A
 * cannot access Patient B's prescription" tested directly against the real backend/DB, not mocks —
 * this is exactly the kind of authorization boundary a unit test with a stubbed repository can't
 * meaningfully prove (it would just prove the mock returns what it's told to).
 */
class PrescriptionAccessControlIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getById_patientACannotAccessPatientBsPrescription() throws Exception {
        Fixture a = bookAndPrescribe(DayOfWeek.MONDAY, "acl-doctor1@example.com", "acl-patientA1@example.com");
        Fixture b = bookAndPrescribe(DayOfWeek.TUESDAY, "acl-doctor2@example.com", "acl-patientB1@example.com");

        mockMvc.perform(get("/api/v1/prescriptions/" + b.prescriptionId)
                        .header("Authorization", "Bearer " + a.patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadPdf_patientACannotDownloadPatientBsPrescription() throws Exception {
        Fixture a = bookAndPrescribe(DayOfWeek.WEDNESDAY, "acl-doctor3@example.com", "acl-patientA2@example.com");
        Fixture b = bookAndPrescribe(DayOfWeek.THURSDAY, "acl-doctor4@example.com", "acl-patientB2@example.com");

        mockMvc.perform(get("/api/v1/prescriptions/" + b.prescriptionId + "/pdf")
                        .header("Authorization", "Bearer " + a.patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void correct_anUnrelatedDoctorCannotCorrectSomeoneElsesPrescription() throws Exception {
        Fixture a = bookAndPrescribe(DayOfWeek.FRIDAY, "acl-doctor5@example.com", "acl-patientA3@example.com");
        Fixture b = bookAndPrescribe(DayOfWeek.SATURDAY, "acl-doctor6@example.com", "acl-patientB3@example.com");

        mockMvc.perform(post("/api/v1/prescriptions/" + a.prescriptionId + "/correct")
                        .header("Authorization", "Bearer " + b.doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"medicationName":"Amoxicillin","dosage":"999mg"}]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMine_onlyReturnsThatPatientsOwnPrescriptions() throws Exception {
        Fixture a = bookAndPrescribe(DayOfWeek.SUNDAY, "acl-doctor7@example.com", "acl-patientA4@example.com");
        bookAndPrescribe(DayOfWeek.MONDAY, "acl-doctor8@example.com", "acl-patientB4@example.com");

        var result = mockMvc.perform(get("/api/v1/prescriptions/mine")
                        .header("Authorization", "Bearer " + a.patientToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("id").asText()).isEqualTo(a.prescriptionId);
    }

    @Test
    void getById_unauthenticatedRequestIsRejected() throws Exception {
        Fixture a = bookAndPrescribe(DayOfWeek.TUESDAY, "acl-doctor9@example.com", "acl-patientA5@example.com");

        mockMvc.perform(get("/api/v1/prescriptions/" + a.prescriptionId))
                .andExpect(status().isUnauthorized());
    }

    private record Fixture(String doctorToken, String patientToken, String prescriptionId) {
    }

    private Fixture bookAndPrescribe(DayOfWeek dayOfWeek, String doctorEmail, String patientEmail) throws Exception {
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
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
        String slotId = slots.get(0).get("id").asText();

        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        var bookResult = mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andReturn();
        String appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asText();

        var consultResult = mockMvc.perform(get("/api/v1/consultations/by-appointment/" + appointmentId)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andReturn();
        String consultationId = objectMapper.readTree(consultResult.getResponse().getContentAsString()).get("id").asText();

        var completeResult = mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/complete")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"medicationName":"Amoxicillin","dosage":"500mg","instructions":"3x/day"}]}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String prescriptionId = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("prescription").get("id").asText();

        return new Fixture(doctorToken, patientToken, prescriptionId);
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
