package com.example;

import java.util.List;
import java.util.Scanner;

import com.example.setup.OidcApplicationSetup;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.model.OpenIdConnectApplication;

public class AppCleanup {

    public static void main(String[] args) {
        
        String appName = "My Web AppX";
        ApiClient apiClient = Clients.builder()
            .setOrgUrl(Inputs.getOrgUrl())
            .setClientCredentials(
                new TokenClientCredentials(Inputs.getApiToken())
            )
            .build();
        
        OidcApplicationSetup oidcAppSetup = new OidcApplicationSetup(apiClient);
        Scanner scanner = new Scanner(System.in);

        try {
            // --- STEP 1: DEACTIVATION ---
            List<OpenIdConnectApplication> activeApps = oidcAppSetup.listOidcAppsByName(appName);
            
            // Note: listOidcAppsByName currently returns ALL apps. 
            // We filter for only the ones that aren't already INACTIVE for the deactivation prompt.
            long actuallyActiveCount = activeApps.stream()
                .filter(app -> !"INACTIVE".equals(app.getStatus().getValue()))
                .count();

            if (actuallyActiveCount > 0) {
                System.out.println("Found " + actuallyActiveCount + " ACTIVE apps named '" + appName + "'");
                System.out.print("Are you sure you want to DEACTIVATE these? (y/n): ");
                if (scanner.nextLine().equalsIgnoreCase("y")) {
                    oidcAppSetup.deactivateOidcAppsByName(appName);
                    System.out.println("Deactivation complete.");
                }
            } else {
                System.out.println("No active apps found to deactivate.");
            }

            // --- STEP 2: DELETION ---
            List<OpenIdConnectApplication> deactivatedApps = oidcAppSetup.listDeactivatedOidcAppsByName(appName);
            
            if (!deactivatedApps.isEmpty()) {
                System.out.println("\nFound " + deactivatedApps.size() + " DEACTIVATED apps named '" + appName + "' ready for deletion:");
                for (OpenIdConnectApplication app : deactivatedApps) {
                    System.out.println(" - ID: " + app.getId());
                }

                System.out.print("\nAre you sure you want to PERMANENTLY DELETE these? (y/n): ");
                if (scanner.nextLine().equalsIgnoreCase("y")) {
                    System.out.println("Deleting...");
                    oidcAppSetup.deleteDeactivatedOidcAppsByName(appName);
                    System.out.println("Deletion complete.");
                } else {
                    System.out.println("Deletion cancelled.");
                }
            } else {
                System.out.println("\nNo deactivated apps found to delete.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}