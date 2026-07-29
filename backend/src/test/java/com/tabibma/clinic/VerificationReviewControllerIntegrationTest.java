package com.tabibma.clinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VerificationReviewControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void listPending_rejectsNonPlatformAdmin() throws Exception {
        registerAndLogin("doctor-review1@example.com", "DOCTOR");
        String doctorToken = login("doctor-review1@example.com");

        mockMvc.perform(get("/api/v1/admin/platform/verification-queue")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listPending_returnsPendingProfilesForPlatformAdmin() throws Exception {
        registerAndLogin("doctor-review2@example.com", "DOCTOR");
        String doctorToken = login("doctor-review2@example.com");
        createProfile(doctorToken, "Cardiology", "Rabat");
        String adminToken = createPlatformAdminAndLogin("platform-admin-review1@example.com");

        mockMvc.perform(get("/api/v1/admin/platform/verification-queue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.specialty=='Cardiology')]").exists());
    }

    @Test
    void approve_marksProfileApprovedAndDocumentReviewed() throws Exception {
        registerAndLogin("doctor-review3@example.com", "DOCTOR");
        String doctorToken = login("doctor-review3@example.com");
        String profileId = createProfile(doctorToken, "Dermatology", "Casablanca");
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", "dummy-bytes".getBytes());
        mockMvc.perform(multipart("/api/v1/clinic/doctor-profiles/" + profileId + "/documents")
                        .file(file)
                        .param("documentType", "MEDICAL_LICENSE")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isCreated());

        String adminToken = createPlatformAdminAndLogin("platform-admin-review2@example.com");

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("APPROVED"));

        mockMvc.perform(get("/api/v1/admin/platform/verification-queue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + profileId + "')]").doesNotExist());
    }

    @Test
    void reject_marksProfileRejected() throws Exception {
        registerAndLogin("doctor-review4@example.com", "DOCTOR");
        String doctorToken = login("doctor-review4@example.com");
        String profileId = createProfile(doctorToken, "Neurology", "Fes");
        String adminToken = createPlatformAdminAndLogin("platform-admin-review3@example.com");

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("REJECTED"));
    }

    @Test
    void approve_rejectsAlreadyReviewedProfile() throws Exception {
        registerAndLogin("doctor-review5@example.com", "DOCTOR");
        String doctorToken = login("doctor-review5@example.com");
        String profileId = createProfile(doctorToken, "Oncology", "Tangier");
        String adminToken = createPlatformAdminAndLogin("platform-admin-review4@example.com");
        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/platform/verification-queue/" + profileId + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
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

    private String createPlatformAdminAndLogin(String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("correcthorsebattery"), Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(admin);
        return login(email);
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
