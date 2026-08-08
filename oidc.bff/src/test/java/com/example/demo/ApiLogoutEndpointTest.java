package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the GET /apilogout endpoint.
 *
 * <p>/apilogout is a custom controller method — not an OIDC flow. It is publicly
 * accessible (permitAll) and its job is to clear the security context and
 * invalidate the session on behalf of any caller.
 *
 * <p><strong>Why @WithMockUser is used here (exception to the oidcLogin() rule):</strong><br>
 * Most tests in spring.oidc.bff use oidcLogin() because endpoints inject
 * @AuthenticationPrincipal OidcUser and @WithMockUser produces the wrong
 * principal type (UsernamePasswordAuthenticationToken instead of
 * OAuth2AuthenticationToken / OidcUser).
 *
 * <p>/apilogout is different: it accepts ANY authenticated principal, not just
 * an OidcUser. The OIDC-specific branch (building the Okta end-session redirect
 * with id_token_hint) fires only when oidcUser != null. @WithMockUser produces
 * a non-OidcUser principal, so oidcUser is always null in these tests.
 *
 * <p>With the current fallback behavior, the endpoint clears local state and
 * redirects to a landing page instead of attempting IdP logout. That fallback
 * redirect behavior is what these tests cover.
 *
 * <p><strong>What is NOT tested here (covered later in real integration tests):</strong><br>
 * The redirect branch — where a genuine OidcUser with a live ID token triggers
 * a 302 redirect to Okta's /v1/logout endpoint — requires a real OidcUser and
 * a valid idTokenValue. That path will be exercised in the real OIDC integration
 * test suite against a live Okta tenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ApiLogoutEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiLogout_UnauthenticatedFallback_RedirectsToDefaultLanding() throws Exception {
        mockMvc.perform(get("/apilogout"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost"));
    }

    // @WithMockUser used intentionally — see class-level Javadoc.
    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void apiLogout_WithMockUserFallback_RedirectsToDefaultLanding() throws Exception {
        mockMvc.perform(get("/apilogout"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost"));
    }

    // @WithMockUser used intentionally — see class-level Javadoc.
    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void apiLogout_WithMockAdminFallback_RedirectsToDefaultLanding() throws Exception {
        mockMvc.perform(get("/apilogout"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost"));
    }

    // @WithMockUser used intentionally — see class-level Javadoc.
    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void apiLogout_WithFrontendSourceAndMockUserFallback_RedirectsToFrontendLanding() throws Exception {
        mockMvc.perform(get("/apilogout")
                .param("source", "frontend"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/"));
    }

    // @WithMockUser used intentionally — see class-level Javadoc.
    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void apiLogout_WithSwaggerSourceAndMockAdminFallback_RedirectsToSwaggerLanding() throws Exception {
        mockMvc.perform(get("/apilogout")
                .param("source", "swagger"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/swagger-ui/index.html"));
    }
}
