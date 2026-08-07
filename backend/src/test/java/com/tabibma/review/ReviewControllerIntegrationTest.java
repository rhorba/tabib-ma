package com.tabibma.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
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
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Story 9.1. No API path reaches AppointmentStatus.COMPLETED without a full real WebRTC
 * consult (Epic 6+7's e2e job) — these tests force it directly via the repository instead,
 * matching the Test Strategy doc's Minimal tier for this feature. */
class ReviewControllerIntegrationTest extends AbstractIntegrationTest {

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
    void submit_recordsAReviewForACompletedAppointmentAndItAppearsInMine() throws Exception {
        String doctorToken = registerDoctorWithSlot("review-doctor1@example.com", DayOfWeek.MONDAY);
        String patientEmail = "review-patient1@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);
        forceComplete(appointmentId);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":5,\"comment\":\"Great doctor\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Great doctor"));

        mockMvc.perform(get("/api/v1/reviews/mine")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appointmentId").value(appointmentId))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void submit_thenTheDoctorsPublicProfileReflectsTheRealRatingAndComment() throws Exception {
        String doctorToken = registerDoctorWithSlot("review-doctor7@example.com", DayOfWeek.SATURDAY);
        String patientEmail = "review-patient7@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);
        forceComplete(appointmentId);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":4,\"comment\":\"Very professional\"}"
                                .formatted(appointmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/" + doctorProfileId + "/public")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.recentReviews[0].rating").value(4))
                .andExpect(jsonPath("$.recentReviews[0].comment").value("Very professional"))
                .andExpect(jsonPath("$.recentReviews[0].patientFirstName").value("A"));
    }

    @Test
    void submit_rejectsAnAppointmentThatIsNotCompletedYet() throws Exception {
        String doctorToken = registerDoctorWithSlot("review-doctor2@example.com", DayOfWeek.TUESDAY);
        String patientEmail = "review-patient2@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":4}".formatted(appointmentId)))
                .andExpect(status().isConflict());
    }

    @Test
    void submit_rejectsSubmittingTwiceForTheSameAppointment() throws Exception {
        String doctorToken = registerDoctorWithSlot("review-doctor3@example.com", DayOfWeek.WEDNESDAY);
        String patientEmail = "review-patient3@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);
        forceComplete(appointmentId);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":5}".formatted(appointmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":1}".formatted(appointmentId)))
                .andExpect(status().isConflict());
    }

    @Test
    void submit_rejectsAPatientReviewingSomeoneElsesAppointment() throws Exception {
        String doctorToken = registerDoctorWithSlot("review-doctor4@example.com", DayOfWeek.THURSDAY);
        registerAndLogin("review-patient4@example.com", "PATIENT");
        String ownerToken = login("review-patient4@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, ownerToken);
        forceComplete(appointmentId);

        registerAndLogin("review-patient5@example.com", "PATIENT");
        String strangerToken = login("review-patient5@example.com");

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":3}".formatted(appointmentId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void submit_rejectsDoctorRole() throws Exception {
        String doctorToken = registerDoctorWithSlot("review-doctor6@example.com", DayOfWeek.FRIDAY);
        registerAndLogin("review-patient6@example.com", "PATIENT");
        String patientToken = login("review-patient6@example.com");
        String appointmentId = bookOnlyOpenSlot(doctorToken, patientToken);
        forceComplete(appointmentId);

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appointmentId\":\"%s\",\"rating\":5}".formatted(appointmentId)))
                .andExpect(status().isForbidden());
    }

    /** Called from registerDoctorWithSlot — BookingService.bookAndPay now rejects a non-APPROVED
     * doctor, and the /public endpoint used by one test also 404s for a non-APPROVED profile.
     * Mirrors DoctorSearchControllerIntegrationTest's approve(). */
    private void approveProfile(String profileId) throws Exception {
        String adminEmail = "review-admin-" + profileId + "@example.com";
        User admin = new User(adminEmail, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        String adminToken = login(adminEmail);

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private void forceComplete(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(UUID.fromString(appointmentId)).orElseThrow();
        appointment.complete();
        appointmentRepository.save(appointment);
    }

    private String bookOnlyOpenSlot(String doctorToken, String patientToken) throws Exception {
        // The generated slot's actual start time can be earlier *today* than "now" (nextOrSame
        // picks today when today already matches the target weekday) — widen `from` well into the
        // past rather than a tight window around "now" to reliably include it either way.
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

    /** Registers a doctor, creates their profile, one rule, and generates exactly one open slot
     * for the next occurrence of the given day of week — mirrors
     * BookingControllerIntegrationTest's createSingleOpenSlot. */
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
