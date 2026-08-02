package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /secured/profile endpoint.
 * This endpoint requires authentication (any authenticated user can access).
 * Authorization rule: .requestMatchers("/secured/profile").authenticated()
 * 
 * Note: This endpoint is defined in SecurityConfiguration but not implemented in SimpleBffApplication.
 * Tests verify security behavior - endpoint will return 404 when called, but security checks happen first.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecuredProfileEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSecuredProfile_Unauthenticated_Returns401() throws Exception {
        // Unauthenticated users should get 401 (security check happens before 404)
        mockMvc.perform(get("/secured/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testSecuredProfile_AsUser_PassesSecurity() throws Exception {
        // Authenticated user passes security check (will get 404 since endpoint not implemented)
        mockMvc.perform(get("/secured/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testSecuredProfile_AsAdmin_PassesSecurity() throws Exception {
        // Authenticated admin passes security check (will get 404 since endpoint not implemented)
        mockMvc.perform(get("/secured/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "anyuser", roles = {"anyrole"})
    void testSecuredProfile_WithAnyRole_PassesSecurity() throws Exception {
        // Any authenticated user passes security check
        mockMvc.perform(get("/secured/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "userWithoutRoles")
    void testSecuredProfile_AuthenticatedNoRoles_PassesSecurity() throws Exception {
        // Authenticated user without roles still passes security check
        mockMvc.perform(get("/secured/profile"))
                .andExpect(status().isNotFound());
    }
}
