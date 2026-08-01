package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CORS tests for the "split deployment" mode where febaseurl is configured.
 *
 * When febaseurl=http://localhost:4200 is set, SecurityConfiguration enters the
 * if-branch and explicitly wires a CorsConfigurationSource that allows that origin.
 * All other origins must be rejected by the CORS filter.
 */
@SpringBootTest(properties = "febaseurl=http://localhost:4200")
@AutoConfigureMockMvc
public class CorsEnabledModeTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Preflight (OPTIONS) ──────────────────────────────────────────────────

    /**
     * Browser preflight from the configured origin must get 200 with the
     * Access-Control-Allow-Origin header echoed back.
     */
    @Test
    void preflight_FromConfiguredOrigin_Returns200WithAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    /**
     * Preflight from an origin NOT in the allowed list must be rejected (403).
     */
    @Test
    void preflight_FromUnknownOrigin_Returns403() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    /**
     * A preflight with no Origin header is not a CORS request at all —
     * it must not be rejected by the CORS filter.
     */
    @Test
    void preflight_WithoutOriginHeader_IsNotRejectedByCors() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ── Actual requests ──────────────────────────────────────────────────────

    /**
     * GET from the configured origin must receive Access-Control-Allow-Origin
     * so the browser permits the response.
     */
    @Test
    void get_FromConfiguredOrigin_ResponseContainsAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .header("Origin", "http://localhost:4200"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    /**
     * GET from a disallowed origin must NOT get the Allow-Origin header —
     * the browser will then block the response.
     */
    @Test
    void get_FromUnknownOrigin_ResponseMissingAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .header("Origin", "http://evil.example.com"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /**
     * setAllowCredentials(true) must be reflected in the preflight response
     * so cookies are sent with cross-origin requests.
     */
    @Test
    void preflight_FromConfiguredOrigin_ResponseAllowsCredentials() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    
}
