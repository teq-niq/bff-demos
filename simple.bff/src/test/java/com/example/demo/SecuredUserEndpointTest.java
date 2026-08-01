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
 * Integration tests for /secured/user endpoint.
 * This endpoint requires the 'myuser' role (ROLE_myuser).
 * Authorization rule: .requestMatchers("/secured/user").hasRole("myuser")
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecuredUserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSecuredUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testSecuredUser_AsUser_Returns200() throws Exception {
        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testSecuredUser_AsAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = {"otherrole"})
    // "otheruser" and "otherrole" are fabricated — they do not exist in InMemoryUserDetailsManager
    // and would be rejected at login in a real app. This is a security-rule isolation test:
    // it confirms the rule rejects any authenticated user who lacks ROLE_myuser,
    // regardless of what role they hold.
    void testSecuredUser_WithWrongRole_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "multiRoleUser", roles = {"myuser", "myadmin"})
    void testSecuredUser_WithBothRoles_Returns200() throws Exception {
        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser(username = "userNoRoles")
    void testSecuredUser_AuthenticatedWithoutRole_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user"))
                .andExpect(status().isForbidden());
    }
}