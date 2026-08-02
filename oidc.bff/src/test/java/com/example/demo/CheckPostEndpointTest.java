package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /checkpost endpoint
 * 
 * Authorization rule: anyRequest().permitAll() - public endpoint
 * Accepts POST requests with JSON body
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CheckPostEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that POST request with CSRF token succeeds
     */
    @Test
    void testCheckPost_WithCsrf_Returns200() throws Exception {
        String requestBody = "{\"field1\":\"value1\",\"field2\":\"value2\"}";
        
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }

    /**
     * Test that POST request without CSRF token is rejected (403 Forbidden)
     */
    @Test
    void testCheckPost_WithoutCsrf_Returns403() throws Exception {
        String requestBody = "{\"field1\":\"value1\",\"field2\":\"value2\"}";
        
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }

    /**
     * Test that authenticated user can POST with CSRF token
     */
    @Test
    void testCheckPost_Authenticated_Returns200() throws Exception {
        String requestBody = "{\"field1\":\"value1\",\"field2\":\"value2\"}";
        
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_myuser"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }

    /**
     * Test that POST with empty JSON body succeeds
     */
    @Test
    void testCheckPost_EmptyBody_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }
}
