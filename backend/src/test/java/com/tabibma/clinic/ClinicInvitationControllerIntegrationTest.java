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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicInvitationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void acceptInvitation_rejectsWhenDoctorHasNoProfileYet() throws Exception {
        String adminToken = createClinicAdminAndLogin("clinic-admin5@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Test", "Rabat");

        String doctorEmail = "invited-doc2@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);

        String invitationId = invite(adminToken, clinicId, doctorEmail);

        mockMvc.perform(post("/api/v1/clinic/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isConflict());
    }

    @Test
    void doctorCanListSeeAndAcceptTheirOwnInvitation() throws Exception {
        String adminToken = createClinicAdminAndLogin("clinic-admin6@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Test", "Casablanca");

        String doctorEmail = "invited-doc3@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        createDoctorProfile(doctorToken, "Dermatology", "Casablanca");

        String invitationId = invite(adminToken, clinicId, doctorEmail);

        mockMvc.perform(get("/api/v1/clinic/invitations/me")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invitedEmail").value(doctorEmail))
                .andExpect(jsonPath("$[0].clinicName").value("Cabinet Test"));

        mockMvc.perform(post("/api/v1/clinic/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/clinic/invitations/me")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void acceptInvitation_rejectsWhenNotAddressedToCaller() throws Exception {
        String adminToken = createClinicAdminAndLogin("clinic-admin7@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Test", "Fes");

        String doctorEmail = "invited-doc4@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String invitationId = invite(adminToken, clinicId, doctorEmail);

        String attackerEmail = "attacker-doc1@example.com";
        registerAndLogin(attackerEmail, "DOCTOR");
        String attackerToken = login(attackerEmail);

        mockMvc.perform(post("/api/v1/clinic/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void declineInvitation_marksDeclinedAndRemovesFromPendingList() throws Exception {
        String adminToken = createClinicAdminAndLogin("clinic-admin8@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Test", "Tangier");

        String doctorEmail = "invited-doc5@example.com";
        registerAndLogin(doctorEmail, "DOCTOR");
        String doctorToken = login(doctorEmail);
        String invitationId = invite(adminToken, clinicId, doctorEmail);

        mockMvc.perform(post("/api/v1/clinic/invitations/" + invitationId + "/decline")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));

        mockMvc.perform(get("/api/v1/clinic/invitations/me")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
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

    private String invite(String adminToken, String clinicId, String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/invitations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createDoctorProfile(String token, String specialty, String city) throws Exception {
        mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"%s","bio":"bio","consultationFeeMad":150.00,"city":"%s"}
                                """.formatted(specialty, city)))
                .andExpect(status().isCreated());
    }
}
