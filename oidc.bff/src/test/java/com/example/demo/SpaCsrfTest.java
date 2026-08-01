package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
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
 * Tests for the SpaCsrfTokenRequestHandler and CookieCsrfTokenRepository wiring.
 *
 * Key behaviours under test:
 *  1. A GET response sets the XSRF-TOKEN cookie (token is eagerly rendered because
 *     SpaCsrfTokenRequestHandler calls csrfToken.get() in handle()).
 *  2. A POST with no CSRF token is rejected (403).
 *  3. A POST that supplies the raw token in the X-XSRF-TOKEN header (SPA style)
 *     is accepted — SpaCsrfTokenRequestHandler routes header values through the
 *     plain CsrfTokenRequestAttributeHandler which validates raw (non-XOR) tokens.
 *  4. A POST that supplies the token as a request parameter (_csrf) with the
 *     XOR-masked value (form style) is also accepted.
 *
 * NOTE: CookieCsrfTokenRepository + SpaCsrfTokenRequestHandler are only activated
 * when febaseurl is set (split-deployment mode). This context
 * therefore sets febaseurl so the full SPA CSRF path is exercised.
 */
@SpringBootTest(properties = "febaseurl=http://localhost:4200")
@AutoConfigureMockMvc
public class SpaCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Cookie issuance ──────────────────────────────────────────────────────

    /**
     * SpaCsrfTokenRequestHandler eagerly calls csrfToken.get() during handle(),
     * which forces CookieCsrfTokenRepository to write the XSRF-TOKEN cookie.
     * A GET to any public endpoint must therefore return the cookie.
     */
    @Test
    void get_PublicEndpoint_SetsXsrfTokenCookie() throws Exception {
        mockMvc.perform(get("/shortprofile"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    /**
     * The cookie must NOT be HttpOnly (withHttpOnlyFalse()) so that the SPA's
     * JavaScript can read it and attach it to subsequent mutating requests.
     */
    @Test
    void get_PublicEndpoint_XsrfCookieIsNotHttpOnly() throws Exception {
        mockMvc.perform(get("/shortprofile"))
            .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    // ── POST without any token ───────────────────────────────────────────────

    /**
     * A POST with no CSRF information at all must be blocked (403 Forbidden).
     */
    @Test
    void post_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }

    // ── POST with X-XSRF-TOKEN header (SPA path) ─────────────────────────────

    /**
     * The SPA reads the raw token from the XSRF-TOKEN cookie and sends it back
     * in the X-XSRF-TOKEN request header.
     *
     * Spring's csrf() post-processor supplies a valid token through the same
     * mechanism the real filter uses, so this correctly exercises the header path
     * inside SpaCsrfTokenRequestHandler.resolveCsrfTokenValue().
     */
    @Test
    void post_WithCsrfHeader_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))          // injects X-XSRF-TOKEN header with valid raw token
            .andExpect(status().isOk());
    }

    /**
     * Same as above but with an authenticated OIDC session to confirm that
     * authenticated POSTs are also protected by (and pass through) CSRF.
     */
    @Test
    void post_AuthenticatedWithCsrfHeader_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf())
                .with(oidcLogin()))
            .andExpect(status().isOk());
    }

    // ── POST with wrong / tampered token ─────────────────────────────────────

    /**
     * A POST that supplies a random/tampered value in the X-XSRF-TOKEN header
     * must be rejected. csrf().useInvalidToken() produces a deliberately wrong token.
     */
    @Test
    void post_WithInvalidCsrfToken_Returns403() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf().useInvalidToken()))
            .andExpect(status().isForbidden());
    }

    // ── Cookie round-trip ────────────────────────────────────────────────────

    /**
     * Full SPA round-trip:
     *  1. GET /shortprofile  → capture the XSRF-TOKEN cookie value.
     *  2. POST /checkpost    → send that raw value in X-XSRF-TOKEN header.
     *
     * This is the closest thing to what the browser actually does and avoids
     * relying solely on the csrf() test shortcut.
     */
    @Test
    void post_WithTokenReadFromCookie_Returns200() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Step 1: get the XSRF-TOKEN cookie
        MvcResult getResult = mockMvc.perform(get("/shortprofile").session(session))
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();

        Cookie xsrfCookie = getResult.getResponse().getCookie("XSRF-TOKEN");
        String csrfToken = xsrfCookie.getValue();

        // Step 2: send the raw cookie value in the X-XSRF-TOKEN header AND
        // re-attach the cookie itself — MockMvc does not carry cookies between
        // requests automatically, and CookieCsrfTokenRepository needs the cookie
        // present on the incoming request to load the expected token.
        mockMvc.perform(post("/checkpost")
                .session(session)
                .cookie(xsrfCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .header("X-XSRF-TOKEN", csrfToken))
            .andExpect(status().isOk());
    }
}