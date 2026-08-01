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
 * CORS tests for the "all-in-one" deployment mode —  febaseurl nor
 *  is set, so SecurityConfiguration takes the else-branch:
 *   "The application is self-contained, CORS remains deny-by-default."
 *
 * No CORS configuration is registered, so Spring's default behaviour kicks in:
 * every cross-origin preflight is rejected and no Access-Control-Allow-Origin
 * header is ever returned on actual requests.
 *
 * Plain @SpringBootTest is used — the default test context has no febaseurl
 *  which is exactly the all-in-one scenario.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CorsDisabledModeTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Without CORS config a preflight from any origin must be rejected (403).
     * This confirms the "deny-by-default" promise in the log message.
     */
    @Test
    void preflight_AnyOrigin_IsRejected() throws Exception {
        mockMvc.perform(options("/shortprofile")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    /**
     * A regular GET carrying an Origin header must NOT receive an
     * Access-Control-Allow-Origin header — the browser would block the response.
     */
    @Test
    void get_WithAnyOriginHeader_NoAllowOriginHeaderInResponse() throws Exception {
        mockMvc.perform(get("/shortprofile")
                .header("Origin", "http://localhost:4200"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /**
     * A plain GET without an Origin header (same-origin or server-to-server)
     * must still work normally — CORS restrictions must not break same-origin traffic.
     */
    @Test
    void get_WithoutOriginHeader_Returns200() throws Exception {
        mockMvc.perform(get("/shortprofile"))
            .andExpect(status().isOk());
    }
}
