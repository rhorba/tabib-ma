package com.tabibma.clinic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabibma.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProfile_rejectsPatientRole() throws Exception {
        String email = "patient-onboarding1@example.com";
        registerAndLogin(email, "PATIENT");
        String token = login(email);

        mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"Cardiology","bio":"bio","consultationFeeMad":250.00,"city":"Rabat"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProfile_allowsDoctorAndDefaultsToPending() throws Exception {
        String email = "doctor-onboarding1@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);

        mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"Cardiology","bio":"Experienced cardiologist","consultationFeeMad":250.00,"city":"Rabat"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.specialty").value("Cardiology"));
    }

    @Test
    void createProfile_rejectsSecondProfileForSameDoctor() throws Exception {
        String email = "doctor-onboarding2@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        createProfile(token, "Dermatology", "Casablanca");

        mockMvc.perform(post("/api/v1/clinic/doctor-profiles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialty":"Dermatology","bio":"bio","consultationFeeMad":200.00,"city":"Casablanca"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void uploadDocument_ownerCanUploadAndDocumentStaysPending() throws Exception {
        String email = "doctor-onboarding3@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        String profileId = createProfile(token, "Pediatrics", "Fes");

        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", "dummy-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/clinic/doctor-profiles/" + profileId + "/documents")
                        .file(file)
                        .param("documentType", "MEDICAL_LICENSE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType").value("MEDICAL_LICENSE"));
    }

    @Test
    void uploadDocument_rejectsUploadToAnotherDoctorsProfile() throws Exception {
        String ownerEmail = "doctor-onboarding4@example.com";
        registerAndLogin(ownerEmail, "DOCTOR");
        String ownerToken = login(ownerEmail);
        String profileId = createProfile(ownerToken, "Neurology", "Marrakesh");

        String attackerEmail = "doctor-onboarding5@example.com";
        registerAndLogin(attackerEmail, "DOCTOR");
        String attackerToken = login(attackerEmail);

        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", "dummy-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/clinic/doctor-profiles/" + profileId + "/documents")
                        .file(file)
                        .param("documentType", "MEDICAL_LICENSE")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadDocument_rejectsDisallowedFileType() throws Exception {
        String email = "doctor-onboarding6@example.com";
        registerAndLogin(email, "DOCTOR");
        String token = login(email);
        String profileId = createProfile(token, "Oncology", "Tangier");

        MockMultipartFile file = new MockMultipartFile("file", "payload.exe", "application/x-msdownload", "dummy".getBytes());

        mockMvc.perform(multipart("/api/v1/clinic/doctor-profiles/" + profileId + "/documents")
                        .file(file)
                        .param("documentType", "MEDICAL_LICENSE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
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
