package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

/**
 * Tests for the SpaCsrfTokenRequestHandler + CookieCsrfTokenRepository wiring.
 *
 * Unlike spring.oidc.bff, CSRF is active in BOTH deployment modes here
 * (the same block appears in both branches of the if/else), so no special
 * property is needed — plain @SpringBootTest is sufficient.
 *
 * Behaviours under test:
 *  1. GET response always sets XSRF-TOKEN cookie (SpaCsrfTokenRequestHandler
 *     calls csrfToken.get() eagerly in handle(), forcing the cookie to be written).
 *  2. XSRF-TOKEN cookie must NOT be HttpOnly so the SPA can read it from JS.
 *  3. POST without any CSRF token → 403.
 *  4. POST with valid X-XSRF-TOKEN header (SPA path, raw token) → 200.
 *  5. POST with tampered token → 403.
 *  6. Full browser round-trip: read cookie value from GET response, send it back
 *     as X-XSRF-TOKEN header on POST → 200.
 *
 * NOTE: Even though CSRF is configured in both deployment branches, the
 * XSRF-TOKEN cookie is only reliably written in the test environment when the
 * if-branch is active. We set febaseurl to trigger it — the CSRF behaviour
 * under test is identical in both branches.
 */
@SpringBootTest(properties = "febaseurl=http://localhost:4200")
@AutoConfigureMockMvc
public class SpaCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Cookie issuance ──────────────────────────────────────────────────────

    @Test
    void get_PublicEndpoint_SetsXsrfTokenCookie() throws Exception {
        mockMvc.perform(get("/shortprofile"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void get_PublicEndpoint_XsrfCookieIsNotHttpOnly() throws Exception {
        mockMvc.perform(get("/shortprofile"))
            .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    // ── POST without token ───────────────────────────────────────────────────

    @Test
    void post_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    // ── POST with valid token (csrf() shortcut) ──────────────────────────────

    @Test
    void post_WithCsrfHeader_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    // ── POST with tampered token ─────────────────────────────────────────────

    @Test
    void post_WithInvalidCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf().useInvalidToken()))
            .andExpect(status().isForbidden());
    }

    // ── Full cookie round-trip ───────────────────────────────────────────────

    /**
     * Simulates what a browser SPA actually does:
     *  1. GET any page → read XSRF-TOKEN cookie value.
     *  2. POST → send that raw value back in X-XSRF-TOKEN header
     *            AND re-attach the cookie (MockMvc doesn't carry cookies automatically).
     */
    @Test
    void post_WithTokenReadFromCookie_Returns200() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MvcResult getResult = mockMvc.perform(get("/shortprofile").session(session))
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();

        Cookie xsrfCookie = getResult.getResponse().getCookie("XSRF-TOKEN");
        String csrfToken = xsrfCookie.getValue();

        mockMvc.perform(post("/checkpost")
                .session(session)
                .cookie(xsrfCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .header("X-XSRF-TOKEN", csrfToken))
            .andExpect(status().isOk());
    }
}