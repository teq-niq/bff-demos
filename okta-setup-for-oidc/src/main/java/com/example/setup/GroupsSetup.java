package com.example.setup;

import java.util.List;

import com.okta.sdk.resource.api.GroupApi;
import com.okta.sdk.resource.client.ApiClient;
import com.okta.sdk.resource.client.ApiException;
import com.okta.sdk.resource.group.GroupBuilder;
import com.okta.sdk.resource.model.Group;
import com.okta.sdk.resource.model.User;

public class GroupsSetup {
	
	private GroupApi groupsApi;

	public GroupsSetup(ApiClient apiClient) {
		groupsApi = new GroupApi(apiClient);
	}
	
	public void assignUserToGroup(String userId, String groupId) {
	    groupsApi.assignUserToGroup(groupId, userId);
	    System.out.println("Assigned user ID '" + userId + "' to group ID '" + groupId + "'");
	}
	public void assignUserToGroup(User user, Group group) {
		String groupId = group.getId();
		String userId = user.getId();
	    groupsApi.assignUserToGroup(groupId, userId);
	  
	    System.out.println("Assigned user ID '" + userId + "' ("+user.getProfile().getLogin()+") to group ID '" + groupId + "'("+group.getProfile().getName()+")");
	}
	
	public Group createGroupIfNotPresent(String groupName, String description) {

	    List<Group> groups = groupsApi.listGroups(
	        "profile.name eq \"" + groupName + "\"",
	        null, null, null, null, null, null, null
	    );

	    if (!groups.isEmpty()) {
	        Group g = groups.get(0);
	        System.out.println(
	            "Group '" + groupName + "' already exists with ID: " + g.getId()
	        );
	        return g;
	    }

	    Group group = GroupBuilder.instance()
	        .setName(groupName)
	        .setDescription(description)
	        .buildAndCreate(groupsApi);

	    System.out.println(
	        "Created Group '" + groupName + "' with ID: " + group.getId()
	    );
	    return group;
	}
	
	 public void cleanupGroup(String groupName) throws ApiException {
	       
	        List<Group> groups = groupsApi.listGroups("profile.name eq \"" + groupName + "\"", null, null, null, null, null, null, null);
	        if (groups.isEmpty()) {
	            System.out.println("Skipping Group '" + groupName + "': Not found.");
	        } else {
	            for (Group g : groups) {
	            	groupsApi.deleteGroup(g.getId());
	                System.out.println("Deleted Group: " + groupName);
	            }
	        }
	    }

}
