package com.tabibma.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerThenLogin_issuesTokenPair() throws Exception {
        String email = "patient1@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery","role":"PATIENT","firstName":"Amina","lastName":"El Amrani"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void register_rejectsPrivilegedRoleSelfRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wannabe-admin@example.com","password":"correcthorsebattery","role":"PLATFORM_ADMIN","firstName":"A","lastName":"B"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void login_withWrongPassword_returnsGenericUnauthorized() throws Exception {
        User user = new User("patient2@example.com", passwordEncoder.encode("correctpassword"), Role.PATIENT, "A", "B");
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"patient2@example.com","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Invalid email or password."));
    }

    @Test
    void login_withUnknownEmail_returnsSameGenericMessageAsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"whatever123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Invalid email or password."));
    }

    @Test
    void meEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meEndpoint_returnsAuthenticatedUser() throws Exception {
        String email = "patient3@example.com";
        registerPatient(email);
        String accessToken = loginAndGetAccessToken(email);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void refresh_rotatesToken_andRejectsReplayOfOldToken() throws Exception {
        String email = "patient4@example.com";
        registerPatient(email);
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(loginResponse);
        String originalRefreshToken = json.get("refreshToken").asText();

        // First refresh: succeeds, rotates to a new token.
        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(originalRefreshToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String rotatedRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);

        // Replaying the original (now-revoked) token must be rejected...
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(originalRefreshToken)))
                .andExpect(status().isUnauthorized());

        // ...and must have revoked the rotated token too (all sessions revoked on replay detection).
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoints_enforceRoleSeparation() throws Exception {
        String platformAdminEmail = "platform-admin1@example.com";
        User platformAdmin = new User(platformAdminEmail, passwordEncoder.encode("correcthorsebattery"),
                Role.PLATFORM_ADMIN, "P", "A");
        userRepository.save(platformAdmin);
        String platformAdminToken = loginAndGetAccessToken(platformAdminEmail);

        String patientEmail = "patient5@example.com";
        registerPatient(patientEmail);
        String patientToken = loginAndGetAccessToken(patientEmail);

        mockMvc.perform(get("/api/v1/admin/platform/me")
                        .header("Authorization", "Bearer " + platformAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/platform/me")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    private void registerPatient(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"correcthorsebattery","role":"PATIENT","firstName":"A","lastName":"B"}
                        """.formatted(email)));
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
