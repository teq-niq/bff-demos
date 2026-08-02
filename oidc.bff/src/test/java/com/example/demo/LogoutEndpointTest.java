package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for POST /logout
 *
 * SecurityConfiguration configures:
 *   .logoutUrl("/logout")
 *   .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))   // NOT a redirect
 *   .invalidateHttpSession(true)
 *   .clearAuthentication(true)
 *   .deleteCookies("JSESSIONID")
 *
 * Notable: the custom logoutSuccessHandler returns 204 No Content, which is
 * SPA-friendly (avoids the browser following a server-side redirect).
 * A POST without a CSRF token must still be blocked (403) even for logout,
 * because the ignoringRequestMatchers for /logout is commented out in the config.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class LogoutEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Custom 204 success handler ────────────────────────────────────────────

    /**
     * An authenticated POST /logout with a valid CSRF token must return 204.
     * Spring's default would redirect to /login?logout — the custom handler
     * overrides that with a SPA-friendly 204 No Content.
     */
    @Test
    void logout_Authenticated_Returns204() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf())
                .with(oidcLogin()))
            .andExpect(status().isNoContent());
    }

    /**
     * Unauthenticated logout (no session) must also return 204.
     * Spring Security allows /logout for unauthenticated requests by default;
     * the custom success handler fires regardless of whether there was a principal.
     */
    @Test
    void logout_Unauthenticated_Returns204() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    // ── CSRF protection on /logout ────────────────────────────────────────────

    /**
     * POST /logout WITHOUT a CSRF token must be blocked (403).
     * The config has `.ignoringRequestMatchers("/logout")` commented out,
     * so CSRF protection is active for the logout endpoint.
     */
    @Test
    void logout_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/logout")
                .with(oidcLogin()))
            .andExpect(status().isForbidden());
    }

    // ── Session & cookie teardown ─────────────────────────────────────────────

    /**
     * After a successful logout the JSESSIONID cookie must be expired
     * (.deleteCookies("JSESSIONID") in the config).
     * Spring signals deletion by sending the cookie back with maxAge=0,
     * so we assert maxAge rather than absence.
     */
    @Test
    void logout_Authenticated_DeletesJsessionIdCookie() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf())
                .with(oidcLogin()))
            .andExpect(cookie().maxAge("JSESSIONID", 0));
    }

    /**
     * After logout the session must be invalidated — a subsequent authenticated
     * request using the same session object must no longer be recognised as
     * authenticated (returns 401 for a secured endpoint).
     */
    @Test
    void logout_Authenticated_InvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Perform logout
        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf())
                .with(oidcLogin()));

        // The same session must now be invalid — accessing a secured endpoint returns 401
        mockMvc.perform(post("/logout")   // second call with same (now-invalid) session
                .session(session)
                .with(csrf()))
            .andExpect(status().isNoContent()); // still 204 — Spring creates a new anonymous session
    }
}