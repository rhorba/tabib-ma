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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Story 6.3 (complete + prescribe in one session) and Story 7.1 (signed, immutable PDF). */
class PrescriptionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void complete_issuesAPrescriptionAndMarksConsultationCompleted() throws Exception {
        Fixture fixture = bookVideoAppointment(DayOfWeek.MONDAY, "presc-doctor1@example.com", "presc-patient1@example.com");

        var completeResult = mockMvc.perform(post("/api/v1/consultations/" + fixture.consultationId + "/complete")
                        .header("Authorization", "Bearer " + fixture.doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"medicationName":"Amoxicillin","dosage":"500mg","instructions":"3x/day"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescription.items[0].medicationName").value("Amoxicillin"))
                .andReturn();
        String prescriptionId = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("prescription").get("id").asText();

        mockMvc.perform(get("/api/v1/consultations/by-appointment/" + fixture.appointmentId)
                        .header("Authorization", "Bearer " + fixture.doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/prescriptions/" + prescriptionId)
                        .header("Authorization", "Bearer " + fixture.patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supersedesId").doesNotExist());
    }

    @Test
    void complete_rejectsThePatient() throws Exception {
        Fixture fixture = bookVideoAppointment(DayOfWeek.TUESDAY, "presc-doctor2@example.com", "presc-patient2@example.com");

        mockMvc.perform(post("/api/v1/consultations/" + fixture.consultationId + "/complete")
                        .header("Authorization", "Bearer " + fixture.patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"medicationName":"Amoxicillin","dosage":"500mg"}]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadPdf_returnsAPdfAttachment() throws Exception {
        Fixture fixture = bookVideoAppointment(DayOfWeek.WEDNESDAY, "presc-doctor3@example.com", "presc-patient3@example.com");
        String prescriptionId = complete(fixture);

        mockMvc.perform(get("/api/v1/prescriptions/" + prescriptionId + "/pdf")
                        .header("Authorization", "Bearer " + fixture.doctorToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("prescription.pdf")));
    }

    @Test
    void correct_createsANewPrescriptionReferencingTheOriginal() throws Exception {
        Fixture fixture = bookVideoAppointment(DayOfWeek.THURSDAY, "presc-doctor4@example.com", "presc-patient4@example.com");
        String originalId = complete(fixture);

        var correctResult = mockMvc.perform(post("/api/v1/prescriptions/" + originalId + "/correct")
                        .header("Authorization", "Bearer " + fixture.doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"medicationName":"Amoxicillin","dosage":"250mg","instructions":"corrected dose"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supersedesId").value(originalId))
                .andReturn();
        String correctionId = objectMapper.readTree(correctResult.getResponse().getContentAsString()).get("id").asText();
        assertThat(correctionId).isNotEqualTo(originalId);

        mockMvc.perform(get("/api/v1/prescriptions/" + originalId)
                        .header("Authorization", "Bearer " + fixture.doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].dosage").value("500mg"));
    }

    private String complete(Fixture fixture) throws Exception {
        var result = mockMvc.perform(post("/api/v1/consultations/" + fixture.consultationId + "/complete")
                        .header("Authorization", "Bearer " + fixture.doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"medicationName":"Amoxicillin","dosage":"500mg","instructions":"3x/day"}]}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("prescription").get("id").asText();
    }

    private record Fixture(String doctorToken, String patientToken, String appointmentId, String consultationId) {
    }

    private Fixture bookVideoAppointment(DayOfWeek dayOfWeek, String doctorEmail, String patientEmail) throws Exception {
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

        return new Fixture(doctorToken, patientToken, appointmentId, consultationId);
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
