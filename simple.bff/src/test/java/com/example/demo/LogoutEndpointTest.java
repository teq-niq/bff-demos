package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for POST /logout.
 *
 * SecurityConfiguration wires:
 *   .logoutUrl("/logout")
 *   .logoutSuccessHandler((req, res, auth) -> res.setStatus(200))  // NOT a redirect
 *
 * Unlike spring.oidc.bff (which returns 204), this app returns 200 on logout.
 * CSRF protection is active — no .ignoringRequestMatchers("/logout") is configured.
 * Note: no .deleteCookies() is configured here, so no JSESSIONID cookie assertion.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class LogoutEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Custom 200 success handler ────────────────────────────────────────────

    /**
     * Authenticated POST /logout with CSRF → custom handler returns 200 (not a redirect).
     */
    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void logout_Authenticated_Returns200() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().isOk());
    }

    /**
     * Unauthenticated POST /logout with CSRF → still 200.
     * Spring Security allows /logout for anonymous users; the success handler
     * fires regardless of whether there was a principal.
     */
    @Test
    void logout_Unauthenticated_Returns200() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().isOk());
    }

    // ── CSRF guard on /logout ─────────────────────────────────────────────────

    /**
     * POST /logout WITHOUT a CSRF token must be blocked (403).
     */
    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void logout_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/logout"))
            .andExpect(status().isForbidden());
    }

    // ── Session invalidation ──────────────────────────────────────────────────

    /**
     * After logout the session is invalidated. A secured endpoint accessed
     * using the same (now-invalid) session must return 401.
     *
     * Uses HTTP Basic to create a real authenticated session first, exercising
     * the actual InMemoryUserDetailsManager stack.
     */
    //TODO will remove this test and basic 	auth support in SecurityConfiguration later. retaining temprarily..
    @Test
    void logout_Authenticated_ViaHttpBasic_InvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Step 1: authenticate via HTTP Basic to establish a real session
        mockMvc.perform(get("/secured/user")
                .session(session)
                .with(httpBasic("user", "password")))
            .andExpect(status().isOk());

        // Step 2: logout using that session
        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
            .andExpect(status().isOk());

        // Step 3: the same session must no longer grant access
        mockMvc.perform(get("/secured/user")
                .session(session))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Same session-invalidation guarantee as above, but the session is
     * established via form login (POST /login) instead of HTTP Basic.
     *
     * This exercises the full UsernamePasswordAuthenticationFilter →
     * InMemoryUserDetailsManager → BCryptPasswordEncoder → successHandler
     * stack, exactly as the Angular SPA would use it.
     *
     * Step 1: POST /login with real credentials → 200 + session cookie created.
     * Step 2: POST /logout with that session → 200.
     * Step 3: GET /secured/user with the same (now-invalid) session → 401.
     */
    @Test
    void logout_Authenticated_ViaFormLogin_InvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Step 1: establish a real session via form login
        mockMvc.perform(post("/login")
                .session(session)
                .with(csrf())
                .param("username", "user")
                .param("password", "password"))
            .andExpect(status().isOk());

        // Step 2: logout using that session
        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
            .andExpect(status().isOk());

        // Step 3: the same session must no longer grant access
        mockMvc.perform(get("/secured/user")
                .session(session))
            .andExpect(status().isUnauthorized());
    }
}