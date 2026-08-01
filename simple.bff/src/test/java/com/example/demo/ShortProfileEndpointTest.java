package com.example.demo;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for /shortprofile endpoint.
 * This endpoint is publicly accessible (permitAll) but returns different data based on authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ShortProfileEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShortProfile_Unauthenticated_Returns200WithLoggedInFalse() throws Exception {
        mockMvc.perform(get("/shortprofile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(false));
    }

    @Test
    @WithMockUser(username = "user", roles = {"myuser"})
    void testShortProfile_AsUser_ReturnsProfileWithUserRole() throws Exception {
        mockMvc.perform(get("/shortprofile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true))
                .andExpect(jsonPath("$.name").value("user"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_myuser")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"myadmin"})
    void testShortProfile_AsAdmin_ReturnsProfileWithAdminRole() throws Exception {
        mockMvc.perform(get("/shortprofile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true))
                .andExpect(jsonPath("$.name").value("admin"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_myadmin")));
    }

    @Test
    @WithMockUser(username = "multiRoleUser", roles = {"myuser", "myadmin"})
    void testShortProfile_WithMultipleRoles_ReturnsAllRoles() throws Exception {
        mockMvc.perform(get("/shortprofile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true))
                .andExpect(jsonPath("$.name").value("multiRoleUser"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_myuser")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_myadmin")));
    }

    @Test
    @WithMockUser(username = "customuser", roles = {"customrole"})
    void testShortProfile_WithCustomRole_ReturnsCustomRole() throws Exception {
        mockMvc.perform(get("/shortprofile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(true))
                .andExpect(jsonPath("$.name").value("customuser"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_customrole")));
    }
}
