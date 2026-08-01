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
 * Integration tests for /secured/foo endpoint
 * 
 * Authorization rule: .requestMatchers("/secured/foo").hasAuthority("SCOPE_foo")
 * This requires SCOPE_foo authority (OAuth2 scope)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecuredFooEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that unauthenticated requests return 401 Unauthorized
     */
    @Test
    void testSecuredFoo_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/secured/foo"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Test that user with SCOPE_foo can access the endpoint
     */
    @Test
    void testSecuredFoo_WithScopeFoo_Returns200() throws Exception {
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    /**
     * Test that user with SCOPE_bar (different scope) cannot access endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredFoo_WithScopeBar_Returns403() throws Exception {
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that user with ROLE_myuser (role, not scope) cannot access scope-based endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredFoo_WithRoleMyuser_Returns403() throws Exception {
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that authenticated user without required scope cannot access endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredFoo_WithoutRequiredScope_Returns403() throws Exception {
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin()))  // No authorities
            .andExpect(status().isForbidden());
    }

    /**
     * Test that user with multiple scopes including SCOPE_foo can access endpoint
     */
    @Test
    void testSecuredFoo_WithMultipleScopes_Returns200() throws Exception {
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }
}
