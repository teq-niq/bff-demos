package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for HTTP Basic authentication (.httpBasic(withDefaults())).
 *
 * HTTP Basic is wired as a second authentication path alongside form login —
 * useful for API tools (curl, Swagger, Postman). These tests exercise the REAL
 * InMemoryUserDetailsManager + BCryptPasswordEncoder stack, not @WithMockUser.
 *
 * Note: GET requests carry no CSRF token but CSRF protection only applies to
 * state-changing methods (POST/PUT/DELETE/PATCH), so GETs are safe here.
 * 
 * 
 */
//TODO: remove — not needed here. The BFF Swagger plugin handles login via
// form login, and curl/Postman/API tools can do the same. The only thing
// httpBasic() adds is support for vanilla Swagger UI's "Authorize" button,
// which this project does not use.
// BUT can use in vanilla swagger behaviour examples.
//so retaining for now.
@SpringBootTest
@AutoConfigureMockMvc
public class HttpBasicAuthTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Correct credentials ──────────────────────────────────────────────────

    @Test
    void securedUser_WithValidUserCredentials_Returns200() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(httpBasic("user", "password")))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Test
    void securedAdmin_WithValidAdminCredentials_Returns200() throws Exception {
        mockMvc.perform(get("/secured/admin")
                .with(httpBasic("admin", "password")))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    // ── Role enforcement over HTTP Basic ─────────────────────────────────────

    /**
     * 'user' (ROLE_myuser) must be denied access to /secured/admin (requires ROLE_myadmin).
     */
    @Test
    void securedAdmin_WithUserCredentials_Returns403() throws Exception {
        mockMvc.perform(get("/secured/admin")
                .with(httpBasic("user", "password")))
            .andExpect(status().isForbidden());
    }

    /**
     * 'admin' (ROLE_myadmin) must be denied access to /secured/user (requires ROLE_myuser).
     */
    @Test
    void securedUser_WithAdminCredentials_Returns403() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(httpBasic("admin", "password")))
            .andExpect(status().isForbidden());
    }

    // ── Wrong credentials → 401 ───────────────────────────────────────────────

    @Test
    void securedUser_WithWrongPassword_Returns401() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(httpBasic("user", "wrongpassword")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void securedUser_WithUnknownUser_Returns401() throws Exception {
        mockMvc.perform(get("/secured/user")
                .with(httpBasic("nobody", "password")))
            .andExpect(status().isUnauthorized());
    }

    // ── No credentials on secured endpoint → 401 ─────────────────────────────

    @Test
    void securedUser_WithNoCredentials_Returns401() throws Exception {
        mockMvc.perform(get("/secured/user"))
            .andExpect(status().isUnauthorized());
    }

   
}
