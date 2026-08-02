package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /apilogout endpoint.
 * This endpoint is publicly accessible (permitAll) and clears the security context.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ApiLogoutEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApiLogout_Unauthenticated_Returns200() throws Exception {
        mockMvc.perform(get("/apilogout"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testApiLogout_AsUser_Returns200() throws Exception {
        mockMvc.perform(get("/apilogout"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testApiLogout_AsAdmin_Returns200() throws Exception {
        mockMvc.perform(get("/apilogout"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testApiLogout_WithSourceParameter_Returns200() throws Exception {
        mockMvc.perform(get("/apilogout")
                .param("source", "frontend"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testApiLogout_WithSwaggerSource_Returns200() throws Exception {
        mockMvc.perform(get("/apilogout")
                .param("source", "swagger"))
                .andExpect(status().isOk());
    }
}
