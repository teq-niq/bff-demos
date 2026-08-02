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
 * Comprehensive authorization tests verifying the security configuration
 * 
 * SecurityConfiguration maps Okta groups to ROLE_* authorities:
 * - Okta group "myuser" → ROLE_myuser
 * - Okta group "myadmin" → ROLE_myadmin
 * 
 * OAuth2 scopes are mapped to SCOPE_* authorities:
 * - scope "foo" → SCOPE_foo
 * - scope "bar" → SCOPE_bar
 * 
 * Authorization rules:
 * - /secured/profile: authenticated() - any logged-in user
 * - /secured/admin: hasRole("myadmin") - requires ROLE_myadmin
 * - /secured/user: hasRole("myuser") - requires ROLE_myuser
 * - /secured/foo: hasAuthority("SCOPE_foo") - requires SCOPE_foo
 * - /secured/bar: hasAuthority("SCOPE_bar") - requires SCOPE_bar
 * - anyRequest().permitAll() - all other endpoints are public
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== ROLE-BASED AUTHORIZATION TESTS ==========

    /**
     * Test matrix: User with ROLE_myuser accessing various endpoints
     */
    @Test
    void testMyuserRole_AccessMatrix() throws Exception {
        // Should succeed
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isOk());
        
        // Should be denied (403 Forbidden) - wrong role
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isForbidden());
        
        // Should be denied (403 Forbidden) - role, not scope
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test matrix: User with ROLE_myadmin accessing various endpoints
     */
    @Test
    void testMyadminRole_AccessMatrix() throws Exception {
        // Should succeed
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isOk());
        
        // Should be denied (403 Forbidden) - wrong role
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isForbidden());
        
        // Should be denied (403 Forbidden) - role, not scope
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isForbidden());
    }

    // ========== SCOPE-BASED AUTHORIZATION TESTS ==========

    /**
     * Test matrix: User with SCOPE_foo accessing various endpoints
     */
    @Test
    void testScopeFoo_AccessMatrix() throws Exception {
        // Should succeed
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isOk());
        
        // Should be denied (403 Forbidden) - wrong scope
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isForbidden());
        
        // Should be denied (403 Forbidden) - scope, not role
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isForbidden());
    }

    /**
     * Test matrix: User with SCOPE_bar accessing various endpoints
     */
    @Test
    void testScopeBar_AccessMatrix() throws Exception {
        // Should succeed
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
        
        // Should be denied (403 Forbidden) - wrong scope
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isForbidden());
        
        // Should be denied (403 Forbidden) - scope, not role
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isForbidden());
    }

    // ========== COMBINED AUTHORITIES TESTS ==========

    /**
     * Test user with both ROLE_myuser and ROLE_myadmin
     */
    @Test
    void testMultipleRoles_AccessMatrix() throws Exception {
        // Should succeed - has both roles
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("ROLE_myuser"),
                    new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("ROLE_myuser"),
                    new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isOk());
    }

    /**
     * Test user with both SCOPE_foo and SCOPE_bar
     */
    @Test
    void testMultipleScopes_AccessMatrix() throws Exception {
        // Should succeed - has both scopes
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
    }

    /**
     * Test power user with all roles and scopes
     */
    @Test
    void testPowerUser_FullAccess() throws Exception {
        // Should succeed on all secured endpoints
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("ROLE_myuser"),
                    new SimpleGrantedAuthority("ROLE_myadmin"),
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("ROLE_myuser"),
                    new SimpleGrantedAuthority("ROLE_myadmin"),
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("ROLE_myuser"),
                    new SimpleGrantedAuthority("ROLE_myadmin"),
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin().authorities(
                    new SimpleGrantedAuthority("ROLE_myuser"),
                    new SimpleGrantedAuthority("ROLE_myadmin"),
                    new SimpleGrantedAuthority("SCOPE_foo"),
                    new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk());
    }

    // ========== UNAUTHENTICATED ACCESS TESTS ==========

    /**
     * Test that all secured endpoints require authentication
     */
    @Test
    void testUnauthenticated_AllSecuredEndpointsReturn401() throws Exception {
        mockMvc.perform(get("/secured/user")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/secured/admin")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/secured/foo")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/secured/bar")).andExpect(status().isUnauthorized());
    }

    /**
     * Test that public endpoints are accessible without authentication
     */
    @Test
    void testUnauthenticated_PublicEndpointsReturn200() throws Exception {
        mockMvc.perform(get("/shortprofile")).andExpect(status().isOk());
    }

    // ========== AUTHENTICATED WITHOUT SPECIFIC AUTHORITIES TESTS ==========

    /**
     * Test authenticated user with no specific roles/scopes
     */
    @Test
    void testAuthenticatedNoAuthorities_OnlyProfileAccessible() throws Exception {
        // Should be denied (403 Forbidden) - requires specific authorities
        mockMvc.perform(get("/secured/user")
                .with(oidcLogin()))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/admin")
                .with(oidcLogin()))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/foo")
                .with(oidcLogin()))
            .andExpect(status().isForbidden());
        
        mockMvc.perform(get("/secured/bar")
                .with(oidcLogin()))
            .andExpect(status().isForbidden());
    }
}
