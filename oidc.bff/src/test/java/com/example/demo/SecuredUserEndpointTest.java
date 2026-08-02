package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /secured/user endpoint
 * 
 * Authorization rule: .requestMatchers("/secured/user").hasRole("myuser")
 * This requires ROLE_myuser authority (mapped from Okta group "myuser")
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecuredUserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that unauthenticated requests return 401 Unauthorized
     */
    @Test
    void testSecuredUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/secured/user"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Test that user with ROLE_myuser can access the endpoint
     * This simulates an Okta user in the "myuser" group
     */
    @Test
    void testSecuredUser_AsMyuser_Returns200() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    /**
     * Test that user with ROLE_myadmin (admin role) cannot access user endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredUser_AsAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that user with SCOPE_foo (scope, not role) cannot access role-based endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredUser_WithScopeFoo_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that authenticated user without required role cannot access endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredUser_WithoutRequiredRole_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin()))  // No authorities
            .andExpect(status().isForbidden());
    }
}
