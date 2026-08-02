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
	public ResponseEntity<Object> checkPost(HttpServletRequest request, @RequestBody Abc abc) throws IOException {
		//takes anything
		HashMap<String, Object> body = new HashMap<String, Object>();
		body.put("message", "POST request received successfully");
		return new ResponseEntity<Object>(body, HttpStatus.OK);
	}
	
	 
	 
	 
	 
	 
	
	
	 
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
