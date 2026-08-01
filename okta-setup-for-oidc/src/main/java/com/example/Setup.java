package com.example;

import java.util.List;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import com.example.setup.AuthorizationServerSetup;
import com.example.setup.GroupsSetup;
import com.example.setup.OidcApplicationSetup;
import com.example.setup.UsersSetup;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.model.Group;
import com.okta.sdk.resource.model.User;

public class Setup {

	private ApiClient apiClient;

	public static void main(String[] args) {
	
		
		new Setup().setup();
			
		
		 

		

	}

	public  void setup() {
		apiClient = Clients.builder()
	    .setOrgUrl(Inputs.getOrgUrl())
	    .setClientCredentials(
	        new TokenClientCredentials(Inputs.getApiToken())
	    )
	    .build();
		
		
		String dummyPwd = Inputs.getDummyPwd();
		
		GroupsSetup groupsSetup = new GroupsSetup(apiClient);
		Group myGroup = groupsSetup.createGroupIfNotPresent( "mygroup", "Application users");
		Group myUserGroup = groupsSetup.createGroupIfNotPresent( "myuser", "User Role");
		Group myAdminGroup = groupsSetup.createGroupIfNotPresent( "myadmin", "Admin Role");
		
		
		UsersSetup usersSetup = new UsersSetup(apiClient);
		User user = usersSetup.createUserIfNotPresent("user@example.com", "User", "Example", dummyPwd);
		User admin = usersSetup.createUserIfNotPresent("admin@example.com", "Admin", "Example", dummyPwd);
		
		groupsSetup.assignUserToGroup(user, myGroup);
		groupsSetup.assignUserToGroup(user, myUserGroup);
		
		groupsSetup.assignUserToGroup(admin, myGroup);
		groupsSetup.assignUserToGroup(admin, myAdminGroup);
		
		
		OidcApplicationSetup oidcAppSetup = new OidcApplicationSetup(apiClient);
		oidcAppSetup.createAppIfNotPresent("My Web App X", List.of("http://localhost:8081/login/oauth2/code/okta"), 
				List.of(
					                "http://localhost:8081",
					                "http://localhost:8081/swagger-ui/index.html",
					                "http://localhost:3201",
					                "http://localhost:4201"
					            ), List.of(myGroup));
		System.out.println("Starting Authorization Server Configuration...");
        AuthorizationServerSetup authServerSetup = new AuthorizationServerSetup(apiClient);
        authServerSetup.setup("default"); 

		
		
		
				
		
	}

	

	

}
