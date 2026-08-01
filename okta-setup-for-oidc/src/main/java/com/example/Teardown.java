package com.example;

import com.example.setup.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.api.*;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.client.ApiException;
import com.okta.sdk.resource.model.*;
import org.openapitools.jackson.nullable.JsonNullableModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.util.List;

public class Teardown {

    private ApiClient apiClient;

    public static void main(String[] args) {
        new Teardown().teardown();
    }

    public void teardown() {
        apiClient = Clients.builder()
            .setOrgUrl(Inputs.getOrgUrl())
            .setClientCredentials(new TokenClientCredentials(Inputs.getApiToken()))
            .build();
     // FIX: Configure mapper to handle JsonNullable and Timestamps
        configureObjectMapper();
        
        AuthorizationServerSetup authServerSetup = new AuthorizationServerSetup(apiClient);
        OidcApplicationSetup oidcAppSetup = new OidcApplicationSetup(apiClient);
        UsersSetup usersSetup = new UsersSetup(apiClient);
        
        GroupsSetup groupsSetup = new GroupsSetup(apiClient);

        

        try {
            System.out.println("=== STARTING TEARDOWN ===");

            // 1. Cleanup Authorization Server (Claims, Scopes, Policies)
            authServerSetup.cleanupAuthServer("default");

            // 2. Cleanup OIDC Application
            oidcAppSetup.cleanupApplication("My Web App X");

            // 3. Cleanup Users (Double-tap delete)
            usersSetup.cleanupUser("user@example.com");
            usersSetup.cleanupUser("admin@example.com");

            // 4. Cleanup Groups
            groupsSetup.cleanupGroup("mygroup");
            groupsSetup.cleanupGroup("myuser");
            groupsSetup.cleanupGroup("myadmin");

            // 5. Cleanup local files
            /*File creds = new File("credentials.txt");
            if (creds.exists()) {
                if (creds.delete()) System.out.println("Deleted local credentials.properties");
            } else {
                System.out.println("Skipping File 'credentials.properties': Not found.");
            }*/

            System.out.println("=== TEARDOWN COMPLETE ===");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new JsonNullableModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        apiClient.setObjectMapper(mapper);
    }

    

  
   
}