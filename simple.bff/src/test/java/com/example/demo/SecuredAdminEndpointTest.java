package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /secured/admin endpoint.
 * This endpoint requires the 'myadmin' role (ROLE_myadmin).
 * Authorization rule: .requestMatchers("/secured/admin").hasRole("myadmin")
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecuredAdminEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSecuredAdmin_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/secured/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testSecuredAdmin_AsUser_Returns403() throws Exception {
        mockMvc.perform(get("/secured/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testSecuredAdmin_AsAdmin_Returns200() throws Exception {
        mockMvc.perform(get("/secured/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser(username = "otheruser", roles = {"otherrole"})
    // "otheruser" and "otherrole" are fabricated — they do not exist in InMemoryUserDetailsManager
    // and would be rejected at login in a real app. This is a security-rule isolation test:
    // it confirms the rule rejects any authenticated user who lacks ROLE_myadmin,
    // regardless of what role they hold.
    void testSecuredAdmin_WithWrongRole_Returns403() throws Exception {
        mockMvc.perform(get("/secured/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "multiRoleUser", roles = {"myuser", "myadmin"})
    void testSecuredAdmin_WithBothRoles_Returns200() throws Exception {
        mockMvc.perform(get("/secured/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser(username = "adminNoRoles")
    void testSecuredAdmin_AuthenticatedWithoutRole_Returns403() throws Exception {
        mockMvc.perform(get("/secured/admin"))
                .andExpect(status().isForbidden());
    }
}