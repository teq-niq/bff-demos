package com.example.demo;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@SpringBootApplication
@RestController
public class OidcBffApplication {
	
	@Value("${okta.oauth2.issuer:#{null}}")
    private String issuer; // (null if not set)
	
	@Value("${febaseurl:#{null}}")
    private String feBaseUrl; // (null if not set)
	
	@PostConstruct
	private void init()
	{
		if(feBaseUrl!=null && feBaseUrl.trim().length()==0)
		{
			feBaseUrl=null;
		}
		
	}
	
	@Bean
	CustomAuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository)
	{
		String authorizationRequestBaseUri = "/oauth2/authorization";
		return new CustomAuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
	}
	
	

	public static void main(String[] args) {
		SpringApplication.run(OidcBffApplication.class, args);
	}
	
	
	
	
	
	
	@PostMapping("/checkpost")
	public ResponseEntity<Object> checkPost(HttpServletRequest request, @RequestBody Abc abc) throws IOException {
		//takes anything
		HashMap<String, Object> body = new HashMap<String, Object>();
		body.put("message", "POST request received successfully");
		return new ResponseEntity<Object>(body, HttpStatus.OK);
	}
	
	 
	 
	 
	 
	
	 
	 
	 @GetMapping("/apilogout")
	 public void apiLogout(HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal  OidcUser oidcUser) throws IOException {
		 String source = request.getParameter("source");
		 
		 
		 String idTokenValue = null;
		   if (oidcUser != null )
		   {
			   OidcIdToken idToken = oidcUser.getIdToken();
			   
			   if (idToken != null) 
			   {
				   idTokenValue=idToken.getTokenValue();
				   System.out.println("idToken1="+idTokenValue);
		             System.out.println("oidcUser1="+oidcUser.hashCode());
			        	if (idToken.getExpiresAt().isBefore(Instant.now())) {
			        		
			        	
			        		System.out.println("ID Token already expired at: " + idToken.getExpiresAt());
			        		// Normally, Spring Security refreshes expired ID tokens automatically,
			        		// so oidcUser.getIdToken() should not be expired.
			        		// But if refresh tokens are disabled or unavailable, ID token may expire.
			        		// If that happens, we may need to track refresh operations manually.
			        		// This stays true whether or not you include below scopes in your configuration:
			        		// okta.oauth2.scopes=openid
			        		// okta.oauth2.scopes=openid, offline_access
			        		
			        		
			        	}
		        	
		        		
		        }
		   }
		   
		    
		 
		 
		 SecurityContextHolder.clearContext();
		    HttpSession session = request.getSession(false);
		    if (session != null) {
		        session.invalidate();
		    }
		    
		    
		    if(idTokenValue!=null)
		    	
		    	
		    {   
		    	int serverPort = request.getServerPort();
		    	String baseUrl = request.getScheme() + "://" +
		                 request.getServerName() +
		                 ((serverPort==80||serverPort==443)?"": (":" + serverPort)) +
		                 request.getContextPath();
		    	
		    	if(source!=null && source.equals("swagger")) {
		    		
		    			baseUrl=baseUrl+"/swagger-ui/index.html";
		    		
		    		
		    	}
		    	
		    	else if(source!=null && source.equals("frontend") ) {
		    		if(feBaseUrl!=null) {
		    			baseUrl=feBaseUrl;
		    		}
		    		else
		    		{
		    			baseUrl=baseUrl+"/";
		    		}
		    		
		    	}
		    	
		    	
		    	String redirectUrl = issuer+"/v1/logout"+
		    	//String redirectUrl = "https://trial-8520257.okta.com/oauth2/v1/logout" +
	                    "?id_token_hint=" + URLEncoder.encode(idTokenValue, StandardCharsets.UTF_8) +
	                    "&post_logout_redirect_uri="+URLEncoder.encode(baseUrl, StandardCharsets.UTF_8);//"http://localhost:9080";
	    		//sendRequest(redirectUrl);
		    	System.out.println("base url="+baseUrl);
		    	System.out.println("Redirecting to Okta logout URL: " + redirectUrl);
		    	response.sendRedirect(redirectUrl);
		    	System.out.println("Redirected to Okta logout URL: " + redirectUrl);
		    	
		    	
		    }

		    
		   // response.sendRedirect(redirectUrl);
		    return;
		    }
		    


	 
	 
	 @GetMapping("/shortprofile")
	    
     public Map<String, Object> shortProfile(OAuth2AuthenticationToken authentication,
             HttpServletRequest request) {
		 
		 //request.getSession(true);
		 Map<String, Object> profile= new java.util.HashMap<>();
		 
		 
		 
		 //profile.put("csrfToken", token.getToken()); // Spring will also set XSRF-TOKEN cookie
		 if(authentication!=null)
		 {
			 OAuth2User principal = authentication.getPrincipal();
			 if(principal!=null)
			 {
				 profile.put("loggedIn", true);
				 profile.put("name", principal.getName());
				 Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
				 
				 if(authorities!=null)
				 {
					 List<GrantedAuthority> authoritiesList = new ArrayList<>(authorities);
					 List<String> authList = new ArrayList<>();
					 
					 List<String> rolesList = new ArrayList<>();
					 List<String> scopesList = new ArrayList<>();
					 for (int i = 0; i < authoritiesList.size(); i++) {
						 GrantedAuthority grantedAuthority = authoritiesList.get(i);
						 authList.add(grantedAuthority.getAuthority());
						 
						 if(grantedAuthority instanceof OidcUserAuthority)
						{
							
							 
						}
						 else
						 {
							 String authorityName = grantedAuthority.getAuthority();
							 if(authorityName.startsWith("ROLE_"))
							 {
								 rolesList.add(grantedAuthority.getAuthority());
							 }
							 else if(authorityName.startsWith("SCOPE_"))
							 {
								 scopesList.add(grantedAuthority.getAuthority());
							 }
						 }
					}
					
					 profile.put("authorities", authList);
					 profile.put("roles", rolesList);
					 profile.put("scopes", scopesList);
				 }
				 
		         if(principal instanceof DefaultOidcUser) {
		        	 DefaultOidcUser oidcUser = (DefaultOidcUser) principal;
		        	 profile.put("email", oidcUser.getEmail());
		             profile.put("subject", oidcUser.getSubject());
		             String fullName = oidcUser.getFullName();
		             if(fullName!=null)
		             {
		            	 
		            	 profile.put("name", fullName);
		             }
		             
		             OidcIdToken idToken = oidcUser.getIdToken();
		             System.out.println("idToken="+idToken.getTokenValue());
		             System.out.println("oidcUser="+oidcUser.hashCode());
		            
		             
		        	 
				 }
				 
			 }
			 else
			 {
				 profile.put("loggedIn", false);
			 }
	         
		 }
		 else
		 {
			 profile.put("loggedIn", false);
		 }
		 System.out.println("returning Profile: " + profile);
		 return profile;
        
     }
	 
	 @GetMapping("/secured/user")
	 public String user()
	 {
		 	     return "ok";
	 }
	 
	 @GetMapping("/secured/admin")
	 public String admin()
	 {
		 	     return "ok";
	 }
	 
	 @GetMapping("/secured/foo")
	 public String foo()
	 {
		 	     return "ok";
	 }
	 @GetMapping("/secured/bar")
	 public String bar()
	 {
		 	     return "ok";
	 }
	 
	 
}
