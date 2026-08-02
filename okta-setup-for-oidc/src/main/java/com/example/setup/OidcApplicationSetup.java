package com.example.setup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.example.Inputs;
import com.example.Setup;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.okta.sdk.resource.api.ApplicationApi;
import com.okta.sdk.resource.api.ApplicationGroupsApi;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.client.ApiException;
import com.okta.sdk.resource.model.Application;
import com.okta.sdk.resource.model.ApplicationAccessibility;
import com.okta.sdk.resource.model.ApplicationCredentialsOAuthClient;
import com.okta.sdk.resource.model.ApplicationSignOnMode;
import com.okta.sdk.resource.model.ApplicationUniversalLogout;
import com.okta.sdk.resource.model.ApplicationVisibility;
import com.okta.sdk.resource.model.ApplicationVisibilityHide;
import com.okta.sdk.resource.model.GrantType;
import com.okta.sdk.resource.model.Group;
import com.okta.sdk.resource.model.OAuthResponseType;
import com.okta.sdk.resource.model.OpenIdConnectApplication;
import com.okta.sdk.resource.model.OpenIdConnectApplicationConsentMethod;
import com.okta.sdk.resource.model.OpenIdConnectApplicationSettings;
import com.okta.sdk.resource.model.OpenIdConnectApplicationSettingsClient;
import com.okta.sdk.resource.model.OpenIdConnectApplicationType;
import com.okta.sdk.resource.model.OpenIdConnectRefreshTokenRotationType;
import com.okta.sdk.resource.model.OpenIdConnectApplication.NameEnum;
import com.okta.sdk.resource.model.OpenIdConnectApplicationIdpInitiatedLogin;
import com.okta.sdk.resource.model.OpenIdConnectApplicationIdpInitiatedLogin.ModeEnum;
import com.okta.sdk.resource.model.OpenIdConnectApplicationSettingsClient.WildcardRedirectEnum;
import com.okta.sdk.resource.model.ApplicationUniversalLogout.StatusEnum;
import com.okta.sdk.resource.model.ApplicationUniversalLogout.SupportTypeEnum;

import com.okta.sdk.resource.model.OpenIdConnectApplicationSettings;
import com.okta.sdk.resource.model.ApplicationUniversalLogout.IdentityStackEnum;
import com.okta.sdk.resource.model.ApplicationUniversalLogout.ProtocolEnum;
import com.okta.sdk.resource.model.ApplicationSettingsNotifications;
import com.okta.sdk.resource.model.ApplicationSettingsNotificationsVpn;
import com.okta.sdk.resource.model.ApplicationSettingsNotificationsVpnNetwork;
import com.okta.sdk.resource.model.ApplicationSettingsNotificationsVpnNetwork.ConnectionEnum;
import com.okta.sdk.resource.model.ApplicationSettingsNotes;
import com.okta.sdk.resource.model.OpenIdConnectApplicationIssuerMode;
import com.okta.sdk.resource.model.OpenIdConnectApplicationSettingsRefreshToken;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class OidcApplicationSetup {
	private ApplicationApi appApi;
	private CloseableHttpClient httpClient;
	
	private ApiClient apiClient;
	private ApplicationGroupsApi applicationGroupsApi;

	public OidcApplicationSetup(ApiClient apiClient) {
		this.apiClient=apiClient;
		
		httpClient = apiClient.getHttpClient();
		// ... inside your setup method
		ObjectMapper customMapper = customMapper();

		// Apply it to your ApiClient
		apiClient.setObjectMapper(customMapper);
		
		appApi = new ApplicationApi(apiClient);
		applicationGroupsApi = new ApplicationGroupsApi(apiClient);
	}
	
	public void deactivateOidcAppsByName(String appName) throws ApiException {
	    List<OpenIdConnectApplication> apps = listOidcAppsByName(appName);
	    
	    for (OpenIdConnectApplication app : apps) {
	        // Only deactivate if it's currently ACTIVE
	        if (!"INACTIVE".equals(app.getStatus().getValue())) {
	            appApi.deactivateApplication(app.getId());
	            System.out.println("Deactivated App: " + app.getLabel() + " (ID: " + app.getId() + ")");
	        }
	    }
	}
	
	public void cleanupApplication(String appName) throws ApiException {
        
    
        deactivateOidcAppsByName(appName);
        deleteDeactivatedOidcAppsByName(appName);
    }

	
	
	public void deleteDeactivatedOidcAppsByName(String appName) throws ApiException {
	    List<OpenIdConnectApplication> apps = listDeactivatedOidcAppsByName(appName);

	    for (OpenIdConnectApplication app : apps) {
	        try {
	           

	            // Step 2: Delete
	            appApi.deleteApplication(app.getId());
	            System.out.println("Deleted App: " + app.getLabel() + " (ID: " + app.getId() + ")");
	            
	        } catch (ApiException e) {
	            System.err.println("Failed to delete " + app.getId() + ": " + e.getResponseBody());
	        }
	    }
	}
	
	public List<OpenIdConnectApplication> listDeactivatedOidcAppsByName(String appName) throws ApiException {
	    // 1. Search by 'q' to get potential matches (includeNonDeleted = true)
	    List<Application> apps = appApi.listApplications(appName, null, false, false, 20, null, null, true);

	    // 2. Filter for exact label, OIDC type, and INACTIVE status
	    return apps.stream()
	            .filter(app -> app.getLabel().equalsIgnoreCase(appName))
	            .filter(app -> app instanceof OpenIdConnectApplication)
	            // Okta status for deactivated apps is "INACTIVE"
	            .filter(app -> "INACTIVE".equals(app.getStatus().getValue()))
	            .map(app -> (OpenIdConnectApplication) app)
	            .collect(Collectors.toList());
	}
	
	
	public List<OpenIdConnectApplication> listOidcAppsByName(String appName) throws ApiException {
	    // 1. Search by 'q' to get potential matches
	    List<Application> apps = appApi.listApplications(appName, null, false, false, 20, null, null, true);

	    // 2. Filter for exact label match AND OIDC type
	    return apps.stream()
	            .filter(app -> app.getLabel().equalsIgnoreCase(appName))
	            .filter(app -> app instanceof OpenIdConnectApplication)
	            .map(app -> (OpenIdConnectApplication) app)
	            .collect(Collectors.toList());
	}
	

	
	public OpenIdConnectApplication createAppIfNotPresent(String appName, List<String> redirectUrls, 
			List<String> postLogoutRedirectUris, List<Group> groupsToAssign) 
	        throws IllegalStateException { // Or a custom exception

	    // 1. Search using 'q'
	    List<Application> apps = appApi.listApplications(
	        appName, 
	        null, 
	        false, 
	        false, 
	        50,      // Increased limit slightly to catch potential duplicates
	        null, 
	        null, 
	        true
	    );

	    // 2. Filter for EXACT matches that are OIDC apps
	    List<OpenIdConnectApplication> exactMatches = apps.stream()
	            .filter(app -> app.getLabel().equalsIgnoreCase(appName))
	            .filter(app -> app instanceof OpenIdConnectApplication)
	            .map(app -> (OpenIdConnectApplication) app)
	            .collect(Collectors.toList());

	    // 3. Handle the logic based on result size
	    if (exactMatches.size() > 1) {
	        // Found duplicates - Safety trigger!
	        throw new IllegalStateException(
	            "Conflict: Found " + exactMatches.size() + 
	            " OIDC applications with the name '" + appName + "'. " +
	            "Please clean up duplicates in Okta before proceeding."
	        );
	    }

	    if (exactMatches.size() == 1) {
	        // Exactly one exists - Update it
	        OpenIdConnectApplication existing = exactMatches.get(0);
	        System.out.println("Found existing app: " + appName + " (ID: " + existing.getId() + "). Updating...");
	        //return patchUrls(existing, redirectUrls, postLogoutRedirectUris);
	        //since patch via sdk is not working forced to do this
	        deactivateOidcAppsByName(appName);
	        deleteDeactivatedOidcAppsByName(appName);
	        return createFix(appName, redirectUrls, postLogoutRedirectUris, groupsToAssign);
	    } else {
	        // None exist - Create fresh
	        System.out.println("No existing app found for '" + appName + "'. Creating new one...");
	        return createFix(appName, redirectUrls, postLogoutRedirectUris, groupsToAssign);
	    }
	}
	
	public OpenIdConnectApplication patchUrls(OpenIdConnectApplication existing, List<String> redirectUrls, List<String> postLogoutRedirectUris) {
	    try {
	        // 1. GET the absolute current state from Okta 
	        // This ensures we have EVERY field (even hidden ones) the API expects
	        OpenIdConnectApplication app = (OpenIdConnectApplication) appApi.getApplication(existing.getId(), null);

	        // 2. Modify only the parts we care about
	        OpenIdConnectApplicationSettings settings = app.getSettings();
	        if (settings != null && settings.getOauthClient() != null) {
	            OpenIdConnectApplicationSettingsClient client = settings.getOauthClient();
	            
	            client.setRedirectUris(redirectUrls);
	            client.setPostLogoutRedirectUris(postLogoutRedirectUris);
	            
	            // Ensure grant types are present to keep the validator happy
	            if (client.getGrantTypes() == null || client.getGrantTypes().isEmpty()) {
	                client.setGrantTypes(List.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN));
	            }
	        }

	        // 3. Prepare the update
	        // We use the SDK's built-in update method which handles the JSON mapping perfectly
	        OpenIdConnectApplication updated = (OpenIdConnectApplication)  appApi.replaceApplication(app.getId(), app);
	        
	        System.out.println("SUCCESS: Application " + updated.getLabel() + " updated via SDK.");
	        return updated;

	    } catch (ApiException e) {
	        System.err.println("Failed to update application via SDK. Status: " + e.getCode());
	        System.err.println("Error Body: " + e.getResponseBody());
	        
	        // If the SDK still fails, it's usually because of a "read-only" field 
	        // conflict. In that case, we fall back to a surgical update.
	        return null;
	    }
	}
	

	private void createNotWorking(String appName, List<String> redirectUrls, List<String> postLogoutRedirectUris) {
		// Create new app
		OpenIdConnectApplication app = new OpenIdConnectApplication();
		app.setLabel(appName);
		app.setName(NameEnum.OIDC_CLIENT);
		app.setSignOnMode(ApplicationSignOnMode.OPENID_CONNECT);
		ApplicationAccessibility accessibility = new ApplicationAccessibility();
		accessibility.setSelfService(false);
		app.setAccessibility(accessibility);
		ApplicationVisibility visibility = new ApplicationVisibility();
		visibility.setAutoSubmitToolbar(false);
		visibility.setAutoLaunch(false);
		ApplicationVisibilityHide hide = new ApplicationVisibilityHide();
		hide.setiOS(true);
		hide.setWeb(true);
		visibility.setHide(hide);
		Map<String, Boolean> appLinks = new HashMap<>();
		appLinks.put("oidc_client_link", true);
		visibility.setAppLinks(appLinks);
		app.setVisibility(visibility);
		ApplicationUniversalLogout universalLogout = new ApplicationUniversalLogout();
		universalLogout.setStatus(StatusEnum.DISABLED);
		universalLogout.setSupportType(SupportTypeEnum.FULL);
		universalLogout.setIdentityStack(IdentityStackEnum.NOT_SHARED);
		universalLogout.setProtocol(ProtocolEnum.GLOBAL_TOKEN_REVOCATION);
		app.setUniversalLogout(universalLogout);
//		    ApplicationVisibility visibility = new ApplicationVisibility();
//		    
//
//		    visibility.setAutoSubmitToolbar(false);
//		    app.setVisibility(visibility);
		

		OpenIdConnectApplicationSettings settings = new OpenIdConnectApplicationSettings();
		ApplicationSettingsNotifications notifications = new ApplicationSettingsNotifications();
		ApplicationSettingsNotificationsVpn vpn = new ApplicationSettingsNotificationsVpn();
		ApplicationSettingsNotificationsVpnNetwork network = new ApplicationSettingsNotificationsVpnNetwork();
   
		network.setConnection(ConnectionEnum.DISABLED);
		vpn.setNetwork(network);
		vpn.setMessage(null);
		vpn.setHelpUrl(null);
		notifications.setVpn(vpn);
		
		settings.setNotifications(notifications);
		
		settings.setImplicitAssignment(false);
		ApplicationSettingsNotes notes = new ApplicationSettingsNotes();
		notes.setAdmin(null);
		notes.setEnduser(null);
		settings.setNotes(notes);
		
		
		OpenIdConnectApplicationSettingsClient client = new OpenIdConnectApplicationSettingsClient();

		client.setApplicationType(OpenIdConnectApplicationType.WEB);
		client.setGrantTypes(List.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN));
		client.setRedirectUris(redirectUrls);
		client.setPostLogoutRedirectUris(postLogoutRedirectUris);
		client.setConsentMethod(OpenIdConnectApplicationConsentMethod.REQUIRED);
		
		client.setResponseTypes(List.of(OAuthResponseType.CODE));
		client.setIssuerMode(OpenIdConnectApplicationIssuerMode.DYNAMIC);
		OpenIdConnectApplicationSettingsRefreshToken refreshToken = new OpenIdConnectApplicationSettingsRefreshToken();
		refreshToken.setRotationType(OpenIdConnectRefreshTokenRotationType.STATIC);
		client.setRefreshToken(refreshToken);
		
		/* Disable Federation Broker Mode */
//		    OpenIdConnectApplicationIdpInitiatedLogin idpLogin = new OpenIdConnectApplicationIdpInitiatedLogin();
//		    idpLogin.setMode(ModeEnum.DISABLED);
//		   
//		    client.setIdpInitiatedLogin(idpLogin);
//		    
//		    client.setInitiateLoginUri(null);
		settings.setOauthClient(client);
		app.setSettings(settings);
		OpenIdConnectApplicationIdpInitiatedLogin idpLogin = new OpenIdConnectApplicationIdpInitiatedLogin();
		idpLogin.setMode(ModeEnum.DISABLED);
		idpLogin.setDefaultScope(new ArrayList<String>());
		client.setIdpInitiatedLogin(idpLogin);
		
   client.setWildcardRedirect(WildcardRedirectEnum.DISABLED);
   client.setDpopBoundAccessTokens(false);
		
   
		
		
		ObjectMapper mapper = customObjectMapper();
		

		try {
			System.out.println(
			    mapper.writeValueAsString(app)
			);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		OpenIdConnectApplication created =
		        (OpenIdConnectApplication) appApi.createApplication(app, true, null);

		System.out.println("Created new app: " + created.getLabel());
		System.out.println("Client ID: " + created.getCredentials().getOauthClient().getClientId());
		System.out.println("Client Secret: " + created.getCredentials().getOauthClient().getClientSecret());
	}
	
	
	
	
	private OpenIdConnectApplication createFix(String appName, List<String> redirectUrls, 
			List<String> postLogoutRedirectUris, List<Group> groupsToAssign) {
	    // 1. Start with a completely clean object
	    OpenIdConnectApplication app = new OpenIdConnectApplication();
	    
	    // Core attributes
	    app.setLabel(appName);
	    app.setName(NameEnum.OIDC_CLIENT); // Use string literal
	    app.setSignOnMode(ApplicationSignOnMode.OPENID_CONNECT);
	    
	    
	    // 2. Settings & Client (Only set what you NEED)
	    OpenIdConnectApplicationSettings settings = new OpenIdConnectApplicationSettings();
	    OpenIdConnectApplicationSettingsClient client = new OpenIdConnectApplicationSettingsClient();

	    client.setApplicationType(OpenIdConnectApplicationType.WEB);
	    client.setGrantTypes(List.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN));
	    client.setResponseTypes(List.of(OAuthResponseType.CODE));
	    client.setRedirectUris(redirectUrls);
	    client.setPostLogoutRedirectUris(postLogoutRedirectUris);
	    client.setConsentMethod(OpenIdConnectApplicationConsentMethod.REQUIRED);
	    client.setIssuerMode(OpenIdConnectApplicationIssuerMode.DYNAMIC);
	 // 3. IDP Initiated Login (Match Postman exactly)
	    OpenIdConnectApplicationIdpInitiatedLogin idpLogin = new OpenIdConnectApplicationIdpInitiatedLogin();
	    idpLogin.setMode(ModeEnum.DISABLED);
	    idpLogin.setDefaultScope(new ArrayList<>());
	    client.setIdpInitiatedLogin(idpLogin);
	    client.setDpopBoundAccessTokens(null);
	    

	    
	    

	    // 4. Visibility (Match Postman exactly)
	    ApplicationVisibility visibility = new ApplicationVisibility();
	    visibility.setAutoLaunch(false);
	    visibility.setAutoSubmitToolbar(false);
	    ApplicationVisibilityHide hide = new ApplicationVisibilityHide();
	    hide.setiOS(true);
	    hide.setWeb(true);
	    visibility.setHide(hide);
	    app.setVisibility(visibility);

	    // NESTING: Attach client to settings, then settings to app
	    settings.setOauthClient(client);
	    app.setSettings(settings);

	    // CRITICAL: Remove UniversalLogout and Accessibility for now. 
	    // They were sending many "null" sub-properties in your previous logs.
	    
	    ObjectMapper mapper = customMapper();
	    
	    String finalString=null;
		try {
			String string = mapper.writeValueAsString(app);
			
			
			
			ObjectNode objectNode = mapper.readValue(string, ObjectNode.class);
			 objectNode.remove("credentials");
			finalString = mapper.writeValueAsString(objectNode);
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 OpenIdConnectApplication createdApp=null;
		if(finalString!=null)
		{
			HttpPost request = new HttpPost(apiClient.getBasePath() + "/api/v1/apps?activate=true");
	        request.setEntity(new StringEntity(finalString, ContentType.APPLICATION_JSON));
	        
	      
	        String apiToken = Inputs.getApiToken();
	        request.setHeader("Authorization", "SSWS " + apiToken);
	        request.setHeader("Accept", "application/json");
	       
			try {
					createdApp = httpClient.execute(request, response -> {
				    int status = response.getCode();
				    String responseBody = EntityUtils.toString(response.getEntity());
				    
				    if (status >= 200 && status < 300) {
				        return mapper.readValue(responseBody, OpenIdConnectApplication.class);
				    } else {
				        //throw new IOException("Okta API Error " + status + ": " + responseBody);
				    	return null;
				    }
				});
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(createdApp!=null)
			{
				
				
				
				
				
				ApplicationCredentialsOAuthClient oauthClient = createdApp.getCredentials().getOauthClient();
				
				Properties props = new Properties();
				String label = createdApp.getLabel();
				String appId = createdApp.getId();
				
				String clientId = oauthClient.getClientId();
				String clientSecret = oauthClient.getClientSecret();
				props.setProperty("label", label);
				props.setProperty("tenantId", Inputs.getTenantId());
				props.setProperty("appId", appId);
				props.setProperty("clientId", clientId);
				props.setProperty("clientSecret", clientSecret);
				props.setProperty("dummyPwd", Inputs.getDummyPwd());
				props.setProperty("oidcappruncommand", "mvn -pl oidc.bff spring-boot:run -P berun -Dokta.tenant.id="+Inputs.getTenantId()+" -Dokta.oauth2.client-id="+clientId+" -Dokta.oauth2.client-secret="+clientSecret);
				props.setProperty("e2eruncommand", "mvn -pl oidc.bff verify -Pe2e -Dokta.tenant.id="+Inputs.getTenantId()+" -Dokta.oauth2.client-id="+clientId+" -Dokta.oauth2.client-secret="+clientSecret+" -Dokta.test.user.email=user@example.com -Dokta.test.user.password="+Inputs.getDummyPwd());
				
				
				
				try {
					storeCreds( props);
				} catch (IOException e) {
					throw new RuntimeException("Unable to store credentials. Please note from sysouts", e);
				}
				
				System.out.println("Application "+label+" having ID of "+appId+" created with client ID: "+clientId+", secret="+clientSecret);
			}
			
	        
		}
		if(createdApp!=null && groupsToAssign!=null && !groupsToAssign.isEmpty())
		{
			for(Group g:groupsToAssign)
			{
				applicationGroupsApi.assignGroupToApplication(createdApp.getId(), g.getId(), null);
				System.out.println("Assigned group "+g.getProfile().getName()+",group.id="+g.getId()+" to application "+createdApp.getLabel()+", app.id="+createdApp.getId());
			}
		}
		

		return createdApp;
	    
		


	    
	    
	    
	}


	private void storeCreds( Properties props) throws IOException {
		File file = new File("credentials.txt");
		try(FileWriter fw=new FileWriter(file);)
		{
			fw.write("# credentials.txt is generated during automated okta setup.\n");
			fw.write("# Its a .gitignored file.\n");
			fw.write("# Its for reference and should not get committed.\n");
			props.stringPropertyNames().stream()
		     .sorted()
		     .forEach(key -> {
		         try {
		             fw.write(key + ":" + props.getProperty(key) + "\n");
		             fw.write( "\n");
		         } catch (IOException e) {
		             throw new RuntimeException("Unable to write credentials to file", e);
		         }
		     });
			
		}
		System.out.println("Credentials stored in "+file.getAbsolutePath());
	}
	
	

	private ObjectMapper customMapper() {
		ObjectMapper customMapper = customObjectMapper();

		customMapper.registerModule(new JavaTimeModule());
		customMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		
		customMapper.enable(SerializationFeature.INDENT_OUTPUT);
		customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return customMapper;
	}


	private ObjectMapper customObjectMapper() {
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper;
	}

}
