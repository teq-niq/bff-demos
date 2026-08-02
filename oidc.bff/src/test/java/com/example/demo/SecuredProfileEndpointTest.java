package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    void testSecuredProfile_AsUser_PassesSecurity() throws Exception {
        // Authenticated user passes security check (will get 404 since endpoint not implemented)
        mockMvc.perform(get("/secured/profile")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSecuredProfile_AsAdmin_PassesSecurity() throws Exception {
        // Authenticated admin passes security check (will get 404 since endpoint not implemented)
        mockMvc.perform(get("/secured/profile")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSecuredProfile_WithAnyRole_PassesSecurity() throws Exception {
        // Any authenticated user passes security check
        mockMvc.perform(get("/secured/profile")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_anyrole"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSecuredProfile_AuthenticatedNoRoles_PassesSecurity() throws Exception {
        // Authenticated user without roles still passes security check
        mockMvc.perform(get("/secured/profile")
                .with(oidcLogin()))
                .andExpect(status().isNotFound());
    }
}