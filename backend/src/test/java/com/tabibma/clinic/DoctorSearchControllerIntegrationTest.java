package com.tabibma.clinic;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorSearchControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void search_rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_excludesNonApprovedProfiles() throws Exception {
        String email = "doctor-search1@example.com";
        registerAndLogin(email, "DOCTOR");
        String doctorToken = login(email);
        createProfile(doctorToken, "Endocrinology", "Kenitra");
        String anyToken = doctorToken;

        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/search")
                        .param("specialty", "Endocrinology")
                        .param("city", "Kenitra")
                        .header("Authorization", "Bearer " + anyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void search_returnsApprovedProfilesMatchingSpecialtyAndCityCaseInsensitively() throws Exception {
        String doctorEmail = "doctor-search2@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String profileId = createProfile(doctorToken, "Gastroenterology", "Tetouan");
        approve(profileId);

        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/search")
                        .param("specialty", "gastroenterology")
                        .param("city", "TETOUAN")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.results[0].specialty").value("Gastroenterology"))
                .andExpect(jsonPath("$.results[0].firstName").value("A"));
    }

    @Test
    void search_withNoFiltersReturnsAllApprovedProfiles() throws Exception {
        String doctorEmail = "doctor-search3@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String profileId = createProfile(doctorToken, "Rheumatology", "Safi");
        approve(profileId);

        // size=500: the shared Testcontainers DB (AbstractIntegrationTest, .logs/decisions.md
        // 2026-07-29) accumulates far more than the default page size (20) of APPROVED profiles
        // across a full test run, so this doctor can fall off page 1 without a wide enough page.
        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/search")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.specialty=='Rheumatology')]").exists());
    }

    @Test
    void getPublicProfile_rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/" + UUID.randomUUID() + "/public"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPublicProfile_returns404ForNonExistentProfile() throws Exception {
        String email = "doctor-search4@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);

        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/" + UUID.randomUUID() + "/public")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublicProfile_returns404ForPendingProfile() throws Exception {
        String email = "doctor-search5@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        String profileId = createProfile(token, "Ophthalmology", "Laayoune");

        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/" + profileId + "/public")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublicProfile_returnsProfileWithStubbedRatingForApprovedProfile() throws Exception {
        String doctorEmail = "doctor-search6@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String profileId = createProfile(doctorToken, "Pulmonology", "Nador");
        approve(profileId);

        String patientEmail = "patient-search6@example.com";
        registerAndLogin(patientEmail, "PATIENT");
        String patientToken = login(patientEmail);

        mockMvc.perform(get("/api/v1/clinic/doctor-profiles/" + profileId + "/public")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialty").value("Pulmonology"))
                .andExpect(jsonPath("$.firstName").value("A"))
                .andExpect(jsonPath("$.averageRating").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.reviewCount").value(0));
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
        String adminEmail = "platform-admin-search-" + profileId + "@example.com";
        User admin = new User(adminEmail, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        String adminToken = login(adminEmail);

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
