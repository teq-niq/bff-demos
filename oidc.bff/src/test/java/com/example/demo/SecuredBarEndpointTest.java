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
 * Integration tests for /secured/bar endpoint
 * 
 * Authorization rule: .requestMatchers("/secured/bar").hasAuthority("SCOPE_bar")
 * This requires SCOPE_bar authority (OAuth2 scope)
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecuredBarEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that unauthenticated requests return 401 Unauthorized
     */
    @Test
    void testSecuredBar_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/secured/bar"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Test that user with SCOPE_bar can access the endpoint
     */
    @Test
    void testSecuredBar_WithScopeBar_Returns200() throws Exception {
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    /**
     * Test that user with SCOPE_foo (different scope) cannot access endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredBar_WithScopeFoo_Returns403() throws Exception {
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that user with ROLE_myadmin (role, not scope) cannot access scope-based endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredBar_WithRoleAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that authenticated user without required scope cannot access endpoint
     * Authorization should be denied (403 Forbidden)
     */
    @Test
    void testSecuredBar_WithoutRequiredScope_Returns403() throws Exception {
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin()))  // No authorities
            .andExpect(status().isForbidden());
    }

    /**
     * Test that user with multiple scopes including SCOPE_bar can access endpoint
     */
    @Test
    void testSecuredBar_WithMultipleScopes_Returns200() throws Exception {
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }
}
