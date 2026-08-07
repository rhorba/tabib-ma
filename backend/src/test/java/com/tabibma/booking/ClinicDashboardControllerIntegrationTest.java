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

/** Story 8.1. See ClinicDashboardService's class javadoc for why this test lives in the
 * booking package rather than clinic. */
class ClinicDashboardControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getDashboard_throwsNotFoundForACallerWithNoClinic() throws Exception {
        registerAndLogin("dashboard-doctor1@example.com", "DOCTOR");
        String token = login("dashboard-doctor1@example.com");

        mockMvc.perform(get("/api/v1/clinic/clinics/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDashboard_returnsZeroesForAClinicWithNoBookingsYet() throws Exception {
        String adminToken = createClinicAdminAndLogin("dashboard-admin1@example.com");
        createClinic(adminToken, "Cabinet Dashboard 1", "Rabat");

        mockMvc.perform(get("/api/v1/clinic/clinics/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingVolume").value(0))
                .andExpect(jsonPath("$.revenueMad").value(0));
    }

    @Test
    void getDashboard_aggregatesOnlyThisClinicsBookingsNotAnotherDoctorsSoloBookings() throws Exception {
        String adminToken = createClinicAdminAndLogin("dashboard-admin2@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Dashboard 2", "Casablanca");

        // A doctor employed by the clinic, with a real paid booking.
        String clinicDoctorEmail = "dashboard-doctor2@example.com";
        registerAndLogin(clinicDoctorEmail, "DOCTOR");
        String clinicDoctorToken = login(clinicDoctorEmail);
        approve(createDoctorProfile(clinicDoctorToken, "Cardiology", "Casablanca"));
        acceptInvitation(adminToken, clinicId, clinicDoctorEmail, clinicDoctorToken);
        String slotId = createSingleOpenSlot(clinicDoctorToken, DayOfWeek.MONDAY);

        registerAndLogin("dashboard-patient1@example.com", "PATIENT");
        String patientToken = login("dashboard-patient1@example.com");
        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(slotId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // An unrelated solo doctor (no clinic) with their own paid booking — must not count.
        String soloDoctorEmail = "dashboard-doctor3@example.com";
        registerAndLogin(soloDoctorEmail, "DOCTOR");
        String soloDoctorToken = login(soloDoctorEmail);
        approve(createDoctorProfile(soloDoctorToken, "Dermatology", "Fes"));
        String soloSlotId = createSingleOpenSlot(soloDoctorToken, DayOfWeek.TUESDAY);
        registerAndLogin("dashboard-patient2@example.com", "PATIENT");
        String patient2Token = login("dashboard-patient2@example.com");
        mockMvc.perform(post("/api/v1/booking/appointments")
                        .header("Authorization", "Bearer " + patient2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availabilitySlotId\":\"%s\"}".formatted(soloSlotId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clinic/clinics/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingVolume").value(1))
                .andExpect(jsonPath("$.revenueMad").value(150.00));
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

    private String createClinicAdminAndLogin(String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("correcthorsebattery"), Role.CLINIC_ADMIN, "C", "A");
        userRepository.save(admin);
        return login(email);
    }

    private String createClinic(String token, String name, String city) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/clinics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","city":"%s"}
                                """.formatted(name, city)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createDoctorProfile(String token, String specialty, String city) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"%s","bio":"bio","consultationFeeMad":150.00,"city":"%s"}
                                """.formatted(specialty, city)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void approve(String profileId) throws Exception {
        String adminEmail = "platform-admin-cdb-" + profileId + "@example.com";
        User admin = new User(adminEmail, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        String adminToken = login(adminEmail);

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private void acceptInvitation(String adminToken, String clinicId, String doctorEmail, String doctorToken)
            throws Exception {
        var inviteResult = mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/invitations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(doctorEmail)))
                .andReturn();
        String invitationId = objectMapper.readTree(inviteResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/clinic/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
    }

    /** Creates one Monday-only rule and generates exactly one open slot for the next
     * occurrence of the given day of week — mirrors BookingControllerIntegrationTest. */
    private String createSingleOpenSlot(String doctorToken, DayOfWeek dayOfWeek) throws Exception {
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
        return slots.get(0).get("id").asText();
    }
}
