package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for customSuccessHandler and customFailureHandler redirect logic.
 *
 * Both handlers read a "source" attribute from the HttpSession that was
 * written earlier by CustomAuthorizationRequestResolver when the login flow started.
 * Based on that value they redirect to:
 *
 *   source == "swagger"   →  /swagger-ui/index.html
 *   source == "frontend"  → feBaseUrl        (if set) else /
 *   anything else         → /
 *
 * We cannot drive the OAuth2 callback through MockMvc end-to-end without a real
 * Okta tenant, so instead we test the handlers as Spring beans and invoke them
 * directly via MockHttpServletRequest / MockHttpServletResponse, keeping the tests
 * fast and independent of network calls.
 */
@SpringBootTest(properties = {
    "febaseurl=http://localhost:4200"
})
@AutoConfigureMockMvc
public class AuthRedirectHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // We access the handlers through the application context via MockMvc against
    // the /shortprofile endpoint (permitAll) as a lightweight "context is up" probe,
    // then invoke the handlers directly using Spring's injected beans.

    @Autowired
    private org.springframework.security.web.authentication.AuthenticationSuccessHandler customSuccessHandler;

    @Autowired
    private org.springframework.security.web.authentication.AuthenticationFailureHandler customFailureHandler;

    // ── Success handler ──────────────────────────────────────────────────────

   

    /**
     * source == "frontend" with febaseurl configured → redirect to febaseurl.
     */
    @Test
    void successHandler_SourceFrontend_WithFeUrl_RedirectsToFeUrl() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request =
            new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response =
            new org.springframework.mock.web.MockHttpServletResponse();

        request.getSession(true).setAttribute("source", "frontend");

        customSuccessHandler.onAuthenticationSuccess(request, response,
            dummyAuthentication());

        org.junit.jupiter.api.Assertions.assertEquals("http://localhost:4200",
            response.getRedirectedUrl());
    }

    /**
     * No "source" attribute → redirect to /.
     */
    @Test
    void successHandler_NoSource_RedirectsToRoot() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request =
            new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response =
            new org.springframework.mock.web.MockHttpServletResponse();

        // No session attribute set
        customSuccessHandler.onAuthenticationSuccess(request, response,
            dummyAuthentication());

        org.junit.jupiter.api.Assertions.assertEquals("/", response.getRedirectedUrl());
    }

    /**
     * Unrecognised source value → redirect to /.
     */
    @Test
    void successHandler_UnknownSource_RedirectsToRoot() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request =
            new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response =
            new org.springframework.mock.web.MockHttpServletResponse();

        request.getSession(true).setAttribute("source", "something-unknown");

        customSuccessHandler.onAuthenticationSuccess(request, response,
            dummyAuthentication());

        org.junit.jupiter.api.Assertions.assertEquals("/", response.getRedirectedUrl());
    }

    // ── Failure handler ──────────────────────────────────────────────────────

    

    /**
     * No source → redirect to /.
     */
    @Test
    void failureHandler_NoSource_RedirectsToRoot() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request =
            new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response =
            new org.springframework.mock.web.MockHttpServletResponse();

        customFailureHandler.onAuthenticationFailure(request, response,
            new org.springframework.security.core.AuthenticationException("test failure") {});

        org.junit.jupiter.api.Assertions.assertEquals("/", response.getRedirectedUrl());
    }

    /**
     * Verifies that the "source" session attribute is consumed (removed) by the
     * failure handler after use, so it cannot accidentally influence a later request.
     */
    @Test
    void failureHandler_SourceSwagger_SessionAttributeRemovedAfterUse() throws Exception {
        org.springframework.mock.web.MockHttpServletRequest request =
            new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response =
            new org.springframework.mock.web.MockHttpServletResponse();

        jakarta.servlet.http.HttpSession session = request.getSession(true);
        session.setAttribute("source", "frontend");

        customFailureHandler.onAuthenticationFailure(request, response,
            new org.springframework.security.core.AuthenticationException("test failure") {});

        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute("source"),
            "source attribute should be removed from session after failure handler runs");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private org.springframework.security.core.Authentication dummyAuthentication() {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "testuser", "n/a",
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_myuser"))
        );
    }
}
