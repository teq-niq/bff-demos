package com.example.demo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(OidcBffApplication.class);
	
	@Value("${okta.oauth2.issuer:#{null}}")
    private String issuer; // (null if not set)
	
	@Value("${febaseurl:#{null}}")
    private String feBaseUrl; // (null if not set)

	@Value("${app.idp.reachability-url:#{null}}")
    private String idpReachabilityUrl; // optional explicit override
	
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
	public ResponseEntity<Object> checkPost(HttpServletRequest request, @RequestBody SamplePayload abc) throws IOException {
		//takes anything
		HashMap<String, Object> body = new HashMap<String, Object>();
		body.put("message", "POST request received successfully");
		return new ResponseEntity<Object>(body, HttpStatus.OK);
	}
	
	 
	 
	 
	 
	
	 
	 
	 // Dual purpose: if this endpoint responds, BFF is reachable; payload confirms IdP reachability from BFF.
	 @GetMapping("/reachability")
	 public Map<String, Boolean> reachability() {
		 log.debug("/reachability invoked");
		 Map<String, Boolean> result = new HashMap<>();
		 result.put("idpReachableByBff", false);
		 result.put("reachabilitySummary", false);

		 String targetUrl = resolveIdpReachabilityTarget();
		 log.debug("IdP reachability target URL resolved to: {}", targetUrl);
		 if (targetUrl == null || targetUrl.isBlank()) {
			 log.debug("IdP reachability target URL is blank; returning idpReachableByBff=false");
			 return result;
		 }

		 try {
			 HttpClient client = HttpClient.newBuilder()
					 .connectTimeout(Duration.ofSeconds(2))
					 .followRedirects(HttpClient.Redirect.NORMAL)
					 .build();

			 HttpRequest request = HttpRequest.newBuilder()
					 .uri(URI.create(targetUrl))
					 .timeout(Duration.ofSeconds(2))
					 .GET()
					 .build();

			 HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
			 boolean idpReachable = response.statusCode() >= 200 && response.statusCode() < 300;
			 result.put("idpReachableByBff", idpReachable);
			 result.put("reachabilitySummary", idpReachable);
			 log.debug("IdP reachability probe status={} reachable={}", response.statusCode(), result.get("idpReachableByBff"));
		 } catch (Exception ex) {
			 log.warn("IdP reachability probe failed for URL [{}]: {}", targetUrl, ex.toString());
		 }

		 return result;
	 }

	 private String resolveIdpReachabilityTarget() {
		 if (idpReachabilityUrl != null && !idpReachabilityUrl.isBlank()) {
			 log.debug("Using configured app.idp.reachability-url");
			 return idpReachabilityUrl.trim();
		 }

		 if (issuer == null || issuer.isBlank()) {
			 return null;
		 }

		 String normalizedIssuer = issuer.endsWith("/")
				 ? issuer.substring(0, issuer.length() - 1)
				 : issuer;
		 log.debug("Using issuer-derived well-known endpoint for reachability");
		 return normalizedIssuer + "/.well-known/openid-configuration";
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
			   log.debug("oidcUser hashCode: {}", oidcUser.hashCode());
		        	if (idToken.getExpiresAt().isBefore(Instant.now())) {
		        	
		        	
		        		log.debug("ID token expired at: {}", idToken.getExpiresAt());
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
		    
		    
		    //if idTokenValue is null it also implies that oidcUser is null which possibly means that 
		    // server was down for some reason and came up and now user is trying to logout from the application.
		    //which is why we are not able to get the idTokenValue from the non existent oidcUser object.
		    // In that case we will redirect to the base url of the application.
		    // its better to redirect to the base url of the application rather than redirecting to the idp logout url with null idTokenValue.
		    // the only issue is that the user will not be logged out from the idp but will be logged out from the application.
		    // this is a trade off we have to make because we cannot get the idTokenValue from the oidcUser object if it is null.
		    // if a user were to subsequently log in again, they would be logged in automatically because the idp session is still active.
		    // tahts the only downside of this approach.
		    // of course idTokenValue of null could also mean that the user is logged out properly earlier
		    // and yet this logout was again invoked.
		    //if so no harm done, we will just redirect to the base url of the application.
		    	
		    	
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
		    	
		    	
		    	String redirectUrl = idTokenValue!=null?issuer+"/v1/logout"+
		    	//String redirectUrl = "https://trial-8520257.okta.com/oauth2/v1/logout" +
	                    "?id_token_hint=" + URLEncoder.encode(idTokenValue, StandardCharsets.UTF_8) +
	                    "&post_logout_redirect_uri="+URLEncoder.encode(baseUrl, StandardCharsets.UTF_8):baseUrl;//"http://localhost:9080";
	    		//sendRequest(redirectUrl);
		    	log.debug("logout base url: {}", baseUrl);
		    	response.sendRedirect(redirectUrl);
		    	
		    	
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
	             log.debug("oidcUser hashCode: {}", oidcUser.hashCode());
		             
		        	 
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
		 log.debug("returning profile: {}", profile);
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
