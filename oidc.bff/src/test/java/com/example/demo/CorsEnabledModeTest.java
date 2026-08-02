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
 * When febaseurl=http://localhost:4200 is set, SecurityConfiguration enables CORS
 * and allows requests originating from that URL. All other origins must be rejected.
 *
 * These tests run in their own Spring application context (separate from the default
 * @SpringBootTest context used by other test classes) because the security filter chain
 * is built differently based on whether febaseurl is present.
 */
@SpringBootTest(properties = "febaseurl=http://localhost:4200")
@AutoConfigureMockMvc
public class CorsEnabledModeTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Preflight (OPTIONS) ──────────────────────────────────────────────────

    /**
     * Browser sends a CORS preflight before a credentialed cross-origin request.
     * The configured origin must receive a 200 with the correct CORS response headers.
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
     * Preflight from an origin that is NOT in the allowed list must be rejected (403).
     * This guards against any origin being able to make credentialed cross-origin calls.
     */
    @Test
    void preflight_FromUnknownOrigin_Returns403() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    /**
     * A preflight with no Origin header at all (same-origin or non-browser call)
     * must not be rejected by the CORS filter — it is simply not a CORS request.
     */
    @Test
    void preflight_WithoutOriginHeader_IsNotRejectedByCors() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ── Actual requests ──────────────────────────────────────────────────────

    /**
     * A GET from the configured origin must receive the Access-Control-Allow-Origin
     * response header so the browser permits the response.
     */
    @Test
    void get_FromConfiguredOrigin_ResponseContainsAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .header("Origin", "http://localhost:4200"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    /**
     * A GET from a disallowed origin must NOT get an Access-Control-Allow-Origin header.
     * Without that header the browser blocks the response.
     */
    @Test
    void get_FromUnknownOrigin_ResponseMissingAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .header("Origin", "http://evil.example.com"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /**
     * The CORS configuration sets credentials=true.
     * The preflight response must reflect this so cookies are sent with cross-origin requests.
     */
    @Test
    void preflight_FromConfiguredOrigin_ResponseAllowsCredentials() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    
}
