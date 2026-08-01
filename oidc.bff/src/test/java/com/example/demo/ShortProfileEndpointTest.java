package com.example.demo;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /shortprofile endpoint
 * 
 * Authorization rule: anyRequest().permitAll() - public endpoint
 * Returns user profile information including roles and scopes when authenticated
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ShortProfileEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that unauthenticated users get a response with loggedIn=false
     */
    @Test
    void testShortProfile_Unauthenticated_ReturnsLoggedInFalse() throws Exception {
        mockMvc.perform(get("/shortprofile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(false));
    }

    /**
     * Test that authenticated user with ROLE_myuser gets profile with roles
     */
    @Test
    void testShortProfile_AsMyuser_ReturnsProfileWithRole() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "Test User").claim("email", "test@example.com"))
                    .authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles", hasItem("ROLE_myuser")))
            .andExpect(jsonPath("$.scopes").isArray())
            .andExpect(jsonPath("$.authorities").isArray());
    }

    /**
     * Test that authenticated user with ROLE_myadmin gets profile with admin role
     */
    @Test
    void testShortProfile_AsAdmin_ReturnsProfileWithAdminRole() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "Admin User").claim("email", "admin@example.com"))
                    .authorities(new SimpleGrantedAuthority("ROLE_myadmin"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.roles", hasItem("ROLE_myadmin")));
    }

    /**
     * Test that user with SCOPE_foo gets profile with scope information
     */
    @Test
    void testShortProfile_WithScopeFoo_ReturnsProfileWithScope() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "API User"))
                    .authorities(new SimpleGrantedAuthority("SCOPE_foo"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.scopes", hasItem("SCOPE_foo")));
    }

    /**
     * Test that user with multiple roles and scopes gets all authorities in profile
     */
    @Test
    void testShortProfile_WithMultipleAuthorities_ReturnsCompleteProfile() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "Power User").claim("email", "power@example.com"))
                    .authorities(
                        new SimpleGrantedAuthority("ROLE_myuser"),
                        new SimpleGrantedAuthority("ROLE_myadmin"),
                        new SimpleGrantedAuthority("SCOPE_foo"),
                        new SimpleGrantedAuthority("SCOPE_bar"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.roles", hasItem("ROLE_myuser")))
            .andExpect(jsonPath("$.roles", hasItem("ROLE_myadmin")))
            .andExpect(jsonPath("$.scopes", hasItem("SCOPE_foo")))
            .andExpect(jsonPath("$.scopes", hasItem("SCOPE_bar")))
            .andExpect(jsonPath("$.authorities").isArray());
    }

    /**
     * Test that authenticated user without specific roles/scopes still gets basic profile
     */
    @Test
    void testShortProfile_WithNoAuthorities_ReturnsBasicProfile() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .with(oidcLogin()
                    .idToken(token -> token.claim("name", "Basic User"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.name").exists());
    }
}
