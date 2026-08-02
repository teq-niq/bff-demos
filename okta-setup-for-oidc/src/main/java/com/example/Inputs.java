package com.example;

public class Inputs {
	
	public static String getOrgUrl() {
		return "https://"+System.getProperty("okta.tenant.id")+".okta.com";
	}
	
	public static String getTenantId() {
		return System.getProperty("okta.tenant.id");
	}
	
	public static String getApiToken() {
		return System.getProperty("okta.api.token");
	}
	
	public static String getDummyPwd() {
		return System.getProperty("dummy.pwd","P@ssword123!");
	}
	
	//

}
