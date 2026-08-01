package com.example.demo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /checkpost endpoint.
 * This endpoint is publicly accessible (permitAll) and accepts POST requests with JSON body.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class CheckPostEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCheckPost_Unauthenticated_WithoutCsrf_Returns403() throws Exception {
        mockMvc.perform(post("/checkpost")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"abc\":\"value\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCheckPost_Unauthenticated_WithCsrf_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"abc\":\"value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testCheckPost_AsUser_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"abc\":\"testvalue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testCheckPost_AsAdmin_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"abc\":\"adminvalue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }

    @Test
    void testCheckPost_WithEmptyJson_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testCheckPost_WithComplexJson_Returns200() throws Exception {
        mockMvc.perform(post("/checkpost")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"abc\":\"complex\",\"nested\":{\"field\":\"value\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("POST request received successfully"));
    }
}
