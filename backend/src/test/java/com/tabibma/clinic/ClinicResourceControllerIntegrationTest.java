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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClinicResourceControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanCreateAndListResourcesForTheirOwnClinic() throws Exception {
        String adminToken = createClinicAdminAndLogin("resource-admin1@example.com");
        String clinicId = createClinic(adminToken, "Cabinet Test", "Rabat");

        mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ROOM","name":"Salle 1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ROOM"))
                .andExpect(jsonPath("$.name").value("Salle 1"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EQUIPMENT","name":"Echographe"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void createResource_rejectsWhenCallerDoesNotOwnClinic() throws Exception {
        String ownerToken = createClinicAdminAndLogin("resource-admin2@example.com");
        String clinicId = createClinic(ownerToken, "Cabinet Test", "Casablanca");

        String attackerToken = createClinicAdminAndLogin("resource-attacker1@example.com");

        mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ROOM","name":"Salle Attaquant"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void listResources_rejectsWhenCallerDoesNotOwnClinic() throws Exception {
        String ownerToken = createClinicAdminAndLogin("resource-admin3@example.com");
        String clinicId = createClinic(ownerToken, "Cabinet Test", "Fes");

        String attackerToken = createClinicAdminAndLogin("resource-attacker2@example.com");

        mockMvc.perform(get("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateResource_marksInactiveAndOwnerOnlyCanDoIt() throws Exception {
        String ownerToken = createClinicAdminAndLogin("resource-admin4@example.com");
        String clinicId = createClinic(ownerToken, "Cabinet Test", "Tangier");
        String resourceId = createResource(ownerToken, clinicId, "ROOM", "Salle 1");

        String attackerToken = createClinicAdminAndLogin("resource-attacker3@example.com");
        mockMvc.perform(patch("/api/v1/clinic/clinics/" + clinicId + "/resources/" + resourceId + "/deactivate")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/clinic/clinics/" + clinicId + "/resources/" + resourceId + "/deactivate")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateResource_rejectsWhenResourceBelongsToAnotherClinic() throws Exception {
        String ownerToken = createClinicAdminAndLogin("resource-admin5@example.com");
        String clinicId = createClinic(ownerToken, "Cabinet Test", "Marrakech");
        String resourceId = createResource(ownerToken, clinicId, "ROOM", "Salle 1");

        String otherOwnerToken = createClinicAdminAndLogin("resource-admin6@example.com");
        String otherClinicId = createClinic(otherOwnerToken, "Autre Cabinet", "Agadir");

        mockMvc.perform(patch("/api/v1/clinic/clinics/" + otherClinicId + "/resources/" + resourceId + "/deactivate")
                        .header("Authorization", "Bearer " + otherOwnerToken))
                .andExpect(status().isNotFound());
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

    private String createResource(String token, String clinicId, String type, String name) throws Exception {
        var result = mockMvc.perform(post("/api/v1/clinic/clinics/" + clinicId + "/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"%s","name":"%s"}
                                """.formatted(type, name)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
