package com.example.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;

@SpringBootApplication
@RestController
public class SimpleBffApplication {
	private static final Logger log = LoggerFactory.getLogger(SimpleBffApplication.class);
	
	
	

	public static void main(String[] args) {
		SpringApplication.run(SimpleBffApplication.class, args);
	}
	
		
	
	
	@PostMapping("/checkpost")
	public ResponseEntity<Object> checkPost(HttpServletRequest request, @RequestBody SamplePayload abc) throws IOException {
		//takes anything
		HashMap<String, Object> body = new HashMap<String, Object>();
		body.put("message", "POST request received successfully");
		return new ResponseEntity<Object>(body, HttpStatus.OK);
	}

	@GetMapping("/reachability")
	public Map<String, Boolean> reachability() {
		Map<String, Boolean> result = new HashMap<>();
		result.put("reachabilitySummary", true);
		return result;
	}
	
	 
	 
	 
	 
	 
	/*
	 * GET is what the callers actually need. Both Swagger UI's BFF plugin and the Angular apps
	 * perform logout via full-page browser navigation (window.location.href / redirect), not an
	 * AJAX call carrying a CSRF header — so the endpoint has to be reachable by a plain
	 * navigation, which means GET.
	 *
	 * It matches the OIDC spec's own shape - https://openid.net/specs/openid-connect-rpinitiated-1_0.html#RPLogout.
	 * OpenID Connect RP-Initiated Logout is itself a redirect-based flow — the user agent is
	 * redirected to the IdP's end-session endpoint, not called via a protected API request.
	 * A GET-based local logout that then redirects onward to Okta's /v1/logout is consistent
	 * with that model rather than fighting it.
	 *
	 * The residual risk is bounded and understood. Without CSRF protection, an attacker could
	 * force a victim's browser to hit /apilogout (e.g. via an <img> tag) and log them out
	 * involuntarily. That's the entire blast radius — it clears the victim's own session,
	 * nothing more. It does not expose credentials, tokens, or let an attacker authenticate
	 * as the victim.
	 *
	 * Forced-logout is a nuisance, not a compromise. Given the ceiling on impact is "annoying,
	 * unrequested logout" rather than any confidentiality/integrity breach, the decision was to
	 * accept that risk rather than add CSRF protection to an endpoint whose only job is
	 * tearing down state.
	 * 
	 * Admittedly in simple.bff could  have used a post here for angular at least but did not seem worth the effort.
	 */
	
	 
	 @GetMapping("/apilogout")
	 public void apiLogout(HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal  User user) throws IOException {
		 String source = request.getParameter("source");
		 
		 
	
		   if (user != null )
		   {
			  
			   //can log which user we are logging out
		
		   }
		   
		    
		 
		 
		 SecurityContextHolder.clearContext();
		    HttpSession session = request.getSession(false);
		    if (session != null) {
		        session.invalidate();
		    }
		    
		    

		    
		   // response.sendRedirect(redirectUrl);
		    return;
		    }
		    


	 
	 
	 @GetMapping("/shortprofile")
	    
	    public Map<String, Object> shortProfile(UsernamePasswordAuthenticationToken authentication,
	            HttpServletRequest request) {
			 
			 //request.getSession(true);
			 Map<String, Object> profile= new java.util.HashMap<>();
			 
			 
			 
			
			 if(authentication!=null)
			 {
				 User principal = (User) authentication.getPrincipal();
				 log.debug("principal class: {}", (principal!=null? principal.getClass().getName():null));
				 if(principal!=null)
				 {
					 profile.put("loggedIn", true);
					 profile.put("name", principal.getUsername());
					 Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
					 
					 if(authorities!=null)
					 {
						 List<GrantedAuthority> authoritiesList = new ArrayList<>(authorities);
						 
						 List<String> rolesList = new ArrayList<>();
			
						 for (int i = 0; i < authoritiesList.size(); i++) {
							 GrantedAuthority grantedAuthority = authoritiesList.get(i);
							 log.debug("grantedAuthority: {}", grantedAuthority.getClass().getName());
			
							 String authorityName = grantedAuthority.getAuthority();
							 if(authorityName.startsWith("ROLE_"))
							 {
								 rolesList.add(grantedAuthority.getAuthority());
							 }
							 
						}
						

						 profile.put("roles", rolesList);

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
			 log.debug("returning Profile: {}", profile);
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
	 
	 
	 
	 
}
