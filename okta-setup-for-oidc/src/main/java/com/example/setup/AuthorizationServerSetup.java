package com.example.setup;

import com.okta.sdk.resource.api.AuthorizationServerApi;
import com.okta.sdk.resource.api.AuthorizationServerClaimsApi;
import com.okta.sdk.resource.api.AuthorizationServerPoliciesApi;
import com.okta.sdk.resource.api.AuthorizationServerRulesApi;
import com.okta.sdk.resource.api.AuthorizationServerScopesApi;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.client.ApiException;
import com.okta.sdk.resource.model.*;
import com.okta.sdk.resource.model.AuthorizationServerPolicyRuleRequest.TypeEnum;

import java.util.List;

/**
 * Finalized Authorization Server Setup Uses the distinct APIs for Servers,
 * Policies, and Rules.
 */
public class AuthorizationServerSetup {

	private final AuthorizationServerApi authApi;
	private final AuthorizationServerPoliciesApi policyApi;
	private final AuthorizationServerRulesApi rulesApi;
	private AuthorizationServerScopesApi scopesApi;
	private AuthorizationServerClaimsApi claimsApi;

	public AuthorizationServerSetup(ApiClient apiClient) {
		this.authApi = new AuthorizationServerApi(apiClient);
		this.policyApi = new AuthorizationServerPoliciesApi(apiClient);
		this.rulesApi = new AuthorizationServerRulesApi(apiClient);
		scopesApi = new AuthorizationServerScopesApi(apiClient);
		claimsApi = new AuthorizationServerClaimsApi(apiClient);
	}

	public void setup(String serverQuery) throws ApiException {
		// 1. Find the Server
		List<AuthorizationServer> servers = authApi.listAuthorizationServers(serverQuery, 10, null);
		AuthorizationServer server = servers.stream()
				.filter(s -> s.getName().equalsIgnoreCase(serverQuery) || "default".equalsIgnoreCase(serverQuery))
				.findFirst().orElseThrow(() -> new ApiException("Auth Server not found: " + serverQuery));

		String serverId = server.getId();
		System.out.println("Configuring Auth Server: " + server.getName());

		// 2. Requirement 5.1: Add Access Policy (This object HAS the .clients() method)
		AuthorizationServerPolicy policy = findOrCreatePolicy(serverId, "Allow web App");

		// 3. Requirement 5.2: Add Policy Rule (This object DOES NOT have .clients())
		findOrCreateRule(serverId, policy.getId(), "Allow Authorization Code");

		// 4. Requirement 6: Scopes
		configureScopes(serverId);

		// 5. Requirement 7: Claims
		configureClaims(serverId);
	}

	private AuthorizationServerPolicy findOrCreatePolicy(String serverId, String policyName) throws ApiException {
	    List<AuthorizationServerPolicy> policies = policyApi.listAuthorizationServerPolicies(serverId);
	    for (AuthorizationServerPolicy p : policies) {
	        if (policyName.equalsIgnoreCase(p.getName())) return p;
	    }

	    // Define Conditions
	    AuthorizationServerPolicyConditions policyConditions = new AuthorizationServerPolicyConditions()
	            .clients(new ClientPolicyCondition().include(List.of("ALL_CLIENTS")));

	    // Define Policy and EXPLICITLY set the type and status
	    AuthorizationServerPolicy newPolicy = new AuthorizationServerPolicy()
	            .name(policyName)
	            .description(policyName)
	            .type(AuthorizationServerPolicy.TypeEnum.OAUTH_AUTHORIZATION_POLICY)
	            .status(AuthorizationServerPolicy.StatusEnum.ACTIVE)
	            .conditions(policyConditions);
	    
	    // Okta handles priority automatically (appends to end) if not specified
	    return policyApi.createAuthorizationServerPolicy(serverId, newPolicy);
	}
	private void findOrCreateRule(String serverId, String policyId, String ruleName) throws ApiException {
	    // 1. Check for existing rule
	    List<AuthorizationServerPolicyRule> rules = rulesApi.listAuthorizationServerPolicyRules(serverId, policyId);

	    for (AuthorizationServerPolicyRule r : rules) {
	        if (ruleName.equalsIgnoreCase(r.getName())) {
	            System.out.println("Rule '" + ruleName + "' already exists.");
	            return;
	        }
	    }

	    // 2. Define Conditions - Use groups("EVERYONE") instead of users("anyone")
	    AuthorizationServerPolicyRuleConditions ruleConditions = new AuthorizationServerPolicyRuleConditions()
	            .grantTypes(new GrantTypePolicyRuleCondition().include(List.of("authorization_code")))
	            .people(new AuthorizationServerPolicyPeopleCondition()
	            		.users(new AuthorizationServerPolicyRuleUserCondition().include(List.of()))
	                    .groups(new AuthorizationServerPolicyRuleGroupCondition().include(List.of("EVERYONE")))
	       ).scopes(new OAuth2ScopesMediationPolicyRuleCondition().include(List.of("*")));

	    // 3. Define Actions with your specific minute-based lifetimes
	    AuthorizationServerPolicyRuleActions ruleActions = new AuthorizationServerPolicyRuleActions()
	            .token(new TokenAuthorizationServerPolicyRuleAction()
	                    .accessTokenLifetimeMinutes(60)       // 1 hour
	                    .refreshTokenLifetimeMinutes(129600)  // 90 days
	                    .refreshTokenWindowMinutes(10080));   // 7 days

	    // 4. Wrap into the REQUEST object 
	    AuthorizationServerPolicyRuleRequest ruleRequest = new AuthorizationServerPolicyRuleRequest()
	            .name(ruleName)
	            .priority(1)
	            .conditions(ruleConditions)
	            .actions(ruleActions)
	            .type(AuthorizationServerPolicyRuleRequest.TypeEnum.RESOURCE_ACCESS);

	    // 5. Call the API
	    rulesApi.createAuthorizationServerPolicyRule(serverId, policyId, ruleRequest);
	    System.out.println("Created Rule: " + ruleName + " with specified lifetimes for 'EVERYONE'.");
	}

	private void configureScopes(String serverId) throws ApiException {

		// 1. Fetch current scopes (Level:
		// /api/v1/authorizationServers/{serverId}/scopes)
		List<OAuth2Scope> existingScopes = scopesApi.listOAuth2Scopes(serverId, null, null, null, 200);

		// 2. Add 'foo' and 'bar' idempotently (Optional Consent)
		for (String scopeName : List.of("foo", "bar")) {
			boolean exists = existingScopes.stream().anyMatch(s -> scopeName.equals(s.getName()));

			if (!exists) {
				OAuth2Scope newScope = new OAuth2Scope().name(scopeName).displayName(scopeName)
						.description("Custom scope " + scopeName).consent(OAuth2ScopeConsentType.FLEXIBLE)
						.optional(true)
						.metadataPublish(OAuth2ScopeMetadataPublish.ALL_CLIENTS);

				scopesApi.createOAuth2Scope(serverId, newScope);
				System.out.println("Created custom scope: " + scopeName);
			} else {
				System.out.println("Scope '" + scopeName + "' already exists.");
			}
		}

		// 3. Modify 'email' scope (Set to REQUIRED)
		existingScopes.stream().filter(s -> "email".equals(s.getName())).findFirst().ifPresent(emailScope -> {
			// Only update if it's not already set to REQUIRED
			if (emailScope.getConsent() != OAuth2ScopeConsentType.REQUIRED) {
				emailScope.setConsent(OAuth2ScopeConsentType.REQUIRED);

				try {
					// The 'replace' method requires serverId, scopeId, and the object
					scopesApi.replaceOAuth2Scope(serverId, emailScope.getId(), emailScope);
					System.out.println("Updated 'email' scope: User consent set to REQUIRED.");
				} catch (ApiException e) {
					System.err.println("Failed to update email scope: " + e.getResponseBody());
				}
			} else {
				System.out.println("'email' scope already has REQUIRED consent.");
			}
		});
	}

	private void configureClaims(String serverId) throws ApiException {

		// 1. Fetch current claims
		List<OAuth2Claim> existingClaims = claimsApi.listOAuth2Claims(serverId);

		// Configure the 'groups' claim for Access Token (RESOURCE)
		ensureGroupsClaim(serverId, claimsApi, existingClaims, OAuth2ClaimType.RESOURCE, "Access Token");

		// Configure the 'groups' claim for ID Token (IDENTITY)
		ensureGroupsClaim(serverId, claimsApi, existingClaims, OAuth2ClaimType.IDENTITY, "ID Token");
	}

	private void ensureGroupsClaim(String serverId, AuthorizationServerClaimsApi claimsApi, List<OAuth2Claim> existing,
			OAuth2ClaimType type, String debugLabel) throws ApiException {

// Check if a claim with the same name AND same token type already exists
		boolean exists = existing.stream().anyMatch(c -> "groups".equals(c.getName()) && type.equals(c.getClaimType()));

		if (!exists) {
// Create the conditions object explicitly
			OAuth2ClaimConditions claimConditions = new OAuth2ClaimConditions();
// Overwrite any defaults with JUST the "ANY" keyword
			//claimConditions.setScopes(List.of("ANY"));

			OAuth2Claim groupsClaim = new OAuth2Claim().name("groups").status(LifecycleStatus.ACTIVE).claimType(type)
					.valueType(OAuth2ClaimValueType.GROUPS).groupFilterType(OAuth2ClaimGroupFilterType.STARTS_WITH)
					.value("my").alwaysIncludeInToken(true).conditions(claimConditions);

			claimsApi.createOAuth2Claim(serverId, groupsClaim);
			System.out.println("Created 'groups' claim for " + debugLabel);
		} else {
			System.out.println("'groups' claim for " + debugLabel + " already exists.");
		}
	}
	
	
	public void cleanupAuthServer(String serverQuery) throws ApiException {
       

        List<AuthorizationServer> servers = authApi.listAuthorizationServers(serverQuery, 1, null);
        if (servers.isEmpty()) {
            System.out.println("Skipping Auth Server cleanup: Server '" + serverQuery + "' not found.");
            return;
        }
        String serverId = servers.get(0).getId();

        // Remove Claims
        List<OAuth2Claim> claims = claimsApi.listOAuth2Claims(serverId);
        boolean claimFound = false;
        for (OAuth2Claim c : claims) {
            if ("groups".equals(c.getName())) {
                claimsApi.deleteOAuth2Claim(serverId, c.getId());
                System.out.println("Deleted claim: groups (" + c.getClaimType() + ")");
                claimFound = true;
            }
        }
        if (!claimFound) System.out.println("Skipping Claim 'groups': Not found.");

        // Remove Scopes & Revert Email
        List<OAuth2Scope> scopes = scopesApi.listOAuth2Scopes(serverId, null, null, null, 100);
        for (String customScope : List.of("foo", "bar")) {
            scopes.stream().filter(s -> customScope.equals(s.getName())).findFirst()
                .ifPresentOrElse(s -> {
                    try {
                        scopesApi.deleteOAuth2Scope(serverId, s.getId());
                        System.out.println("Deleted scope: " + s.getName());
                    } catch (ApiException e) { e.printStackTrace(); }
                }, () -> System.out.println("Skipping Scope '" + customScope + "': Not found."));
        }

        scopes.stream().filter(s -> "email".equals(s.getName())).findFirst().ifPresent(s -> {
            try {
                s.setConsent(OAuth2ScopeConsentType.FLEXIBLE);
                scopesApi.replaceOAuth2Scope(serverId, s.getId(), s);
                System.out.println("Reverted 'email' scope to FLEXIBLE consent.");
            } catch (ApiException e) { e.printStackTrace(); }
        });

        // Remove Policy
        List<AuthorizationServerPolicy> policies = policyApi.listAuthorizationServerPolicies(serverId);
        policies.stream().filter(p -> "Allow web App".equalsIgnoreCase(p.getName())).findFirst()
            .ifPresentOrElse(p -> {
                try {
                    policyApi.deleteAuthorizationServerPolicy(serverId, p.getId());
                    System.out.println("Deleted Authorization Policy: " + p.getName());
                } catch (ApiException e) { e.printStackTrace(); }
            }, () -> System.out.println("Skipping Policy 'Allow web App': Not found."));
    }

}