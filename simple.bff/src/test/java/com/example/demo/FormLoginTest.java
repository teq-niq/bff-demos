package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for POST /login (form login).
 *
 * SecurityConfiguration wires:
 *   .loginProcessingUrl("/login")
 *   .successHandler((req, res, auth) -> res.setStatus(200))   // SPA-friendly, no redirect
 *   .failureHandler((req, res, ex)  -> res.sendError(401))
 *
 * InMemoryUserDetailsManager has:
 *   user  / password  → ROLE_myuser
 *   admin / password  → ROLE_myadmin
 *
 * These tests exercise the REAL authentication stack — UserDetailsService,
 * BCryptPasswordEncoder, form login filter — not @WithMockUser shortcuts.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class FormLoginTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Success cases ────────────────────────────────────────────────────────

    /**
     * Valid user credentials → custom successHandler returns 200 (not a redirect).
     */
    @Test
    void login_ValidUserCredentials_Returns200() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "user")
                .param("password", "password"))
            .andExpect(status().isOk());
    }

    /**
     * Valid admin credentials → custom successHandler returns 200.
     */
    @Test
    void login_ValidAdminCredentials_Returns200() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "admin")
                .param("password", "password"))
            .andExpect(status().isOk());
    }

    // ── Failure cases ────────────────────────────────────────────────────────

    /**
     * Wrong password → custom failureHandler returns 401.
     */
    @Test
    void login_WrongPassword_Returns401() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "user")
                .param("password", "wrongpassword"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Non-existent username → custom failureHandler returns 401.
     */
    @Test
    void login_UnknownUser_Returns401() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "nobody")
                .param("password", "password"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Empty credentials → custom failureHandler returns 401.
     */
    @Test
    void login_EmptyCredentials_Returns401() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "")
                .param("password", ""))
            .andExpect(status().isUnauthorized());
    }

    // ── CSRF guard on /login ─────────────────────────────────────────────────

    /**
     * POST /login without a CSRF token must be blocked (403) even with valid credentials.
     * CSRF is intentionally NOT ignored on /login — the Angular SPA already has the
     * XSRF-TOKEN cookie from a prior GET, so it can always supply the token.
     * Ignoring CSRF on /login would weaken the posture for no practical gain.
     */
    @Test
    void login_ValidCredentials_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "user")
                .param("password", "password"))
            .andExpect(status().isForbidden());
    }
}