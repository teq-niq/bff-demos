package com.example.setup;

import java.util.List;

import com.okta.sdk.resource.api.UserApi;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.client.ApiException;
import com.okta.sdk.resource.model.CreateUserRequest;
import com.okta.sdk.resource.model.PasswordCredential;
import com.okta.sdk.resource.model.User;
import com.okta.sdk.resource.model.UserCredentialsWritable;
import com.okta.sdk.resource.model.UserProfile;

public class UsersSetup {
	private final UserApi userApi;

	public UsersSetup(ApiClient apiClient) {
		this.userApi = new UserApi(apiClient);
	}
	
	  
    public void cleanupUser(String login) throws ApiException {
        
        try {
            // We use the 'login' string directly as the ID. 
            // Okta's /api/v1/users/{id} endpoint accepts login, email, or ID.
            
            System.out.println("Attempting to deactivate user: " + login);
            // First tap: Transitions to DEPROVISIONED
            userApi.deleteUser(login, false, null);

            System.out.println("Attempting to permanently delete user: " + login);
            // Second tap: Deletes permanently
            userApi.deleteUser(login, false, null);
            
            System.out.println("Successfully removed User: " + login);

        } catch (ApiException e) {
            if (e.getCode() == 404) {
                System.out.println("Skipping User '" + login + "': Not found.");
            } else {
                // If the user is already deprovisioned, the first call might throw a 400 or 403 
                // depending on SDK version. We handle that here.
                System.err.println("Note: Cleanup for " + login + " hit an API response: " + e.getCode());
            }
        }
    }

	public User createUserIfNotPresent(
            String login,
            String firstName,
            String lastName,
            String password
    ) {

		 try {
		        User existing = userApi.getUser(login, null, null);
		        System.out.println(
		            "User '" + login + "' already exists with ID: " + existing.getId()
		        );
		        return existing;
		    }
		    catch (ApiException e) {
		        if (e.getCode() != 404) {
		            // real error (auth, rate limit, network, etc.)
		            throw e;
		        }
		        // 404 = user does not exist → create
		    }
		    
        // 2. Build user profile
        UserProfile profile = new UserProfile();
        profile.setLogin(login);
        profile.setEmail(login);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);

        // 3. Password credential
        PasswordCredential passwordCred = new PasswordCredential();
        passwordCred.setValue(password);

        UserCredentialsWritable credentials = new UserCredentialsWritable();
        credentials.setPassword(passwordCred);
        

        // 4. Create user request
        CreateUserRequest request = new CreateUserRequest();
        request.setProfile(profile);
        request.setCredentials(credentials);

        // 5. Create user (activate = true, sendEmail = false)
        User user = userApi.createUser(
            request,
            true,   // activate
            false,  // sendEmail
            null
        );

        System.out.println(
            "Created User '" + login + "' with ID: " + user.getId()
        );
        return user;
    }

}
