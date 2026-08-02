package com.example.demo;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);
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
	public OAuth2UserService<OidcUserRequest, OidcUser> customOidcUserService() {

	    OidcUserService delegate = new OidcUserService();

	    return (userRequest) -> {

	        OidcUser oidcUser = delegate.loadUser(userRequest);

	        Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());

	        // read Okta "groups" claim
	        Collection<String> groups = oidcUser.getAttribute("groups");
	        if (groups != null) {
	            for (String group : groups) {
	                mapped.add(new SimpleGrantedAuthority("ROLE_" + group));
	            }
	        }

	        return new DefaultOidcUser(
	                mapped,
	                oidcUser.getIdToken(),
	                oidcUser.getUserInfo()
	        );
	    };
	}
	@Autowired
	CustomAuthorizationRequestResolver customAuthorizationRequestResolver;
	
	@Bean
	public SecurityFilterChain security(HttpSecurity http) throws Exception {
		log.debug("feBaseUrl=[{}]", feBaseUrl);
	 
	   boolean feBaseUrlIsNotNull = feBaseUrl!=null;
	  
		if (feBaseUrlIsNotNull) {
					
					
			if(feBaseUrlIsNotNull) {
				log.debug("CORS enabled for febaseurl: {}", feBaseUrl);
			}
			
			
			Customizer<CorsConfigurer<HttpSecurity>> corsCustomizer=new Customizer<CorsConfigurer<HttpSecurity>>() {
				
				@Override
				public void customize(CorsConfigurer<HttpSecurity> http) {
					
					http.configurationSource(request->{
						CorsConfiguration cors=new CorsConfiguration();
						if(feBaseUrlIsNotNull) {
							cors.addAllowedOrigin(feBaseUrl);
						log.debug("added feBaseUrl to CORS:{} for request URL:{}", feBaseUrl, request.getRequestURL());
						}
						
							
						
						
						cors.addAllowedMethod("*");
						cors.addAllowedHeader("*");
						cors.setAllowCredentials(true);
						return cors;
					});
				}
			};
			
			http=http.cors(corsCustomizer);
			http=http
				    .csrf(csrf -> 
				    		csrf
				    		.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				    		.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()) 
				    	  );
			log.debug("CSRF protection is enabled");
		}
		else
		{
			log.debug("The application is self-contained, CORS remains deny-by-default and CSRF protection is enabled.");
			http=http
				    .csrf(csrf -> 
				    		csrf
				    		.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				    		.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()) 
				    	  );
		}
		
		
		

	    
	        http.authorizeHttpRequests(auth -> auth
	        		.requestMatchers("/secured/profile").authenticated()
	        		
	            .requestMatchers("/secured/admin").hasRole("myadmin")  // ROLE_myadmin
	            .requestMatchers("/secured/user").hasRole("myuser")    // ROLE_myuser
	            .requestMatchers("/secured/foo").hasAuthority("SCOPE_foo")
	            .requestMatchers("/secured/bar").hasAuthority("SCOPE_bar")
	            //.anyRequest().authenticated()
	            .anyRequest().permitAll()
	        )
	        .oauth2Login(oauth -> oauth
	        		.authorizationEndpoint(authEndpoint -> authEndpoint 
	        		        
	        		        .authorizationRequestResolver(customAuthorizationRequestResolver)
	        		    )
	            .userInfoEndpoint(userInfo -> userInfo
	                .oidcUserService(customOidcUserService())
	                
	            )
	            
	            .successHandler(customSuccessHandler()) 
	            
	        ).logout(cfg->
	    	cfg.logoutUrl("/logout")
	    	.logoutSuccessHandler((request, response, authentication) -> {
	            response.setStatus(HttpServletResponse.SC_NO_CONTENT);  // 204
	        })
	    	//.logoutSuccessUrl("/")  
	    	// Angular home page after logout
            .invalidateHttpSession(true)
            .clearAuthentication(true)
            .deleteCookies("JSESSIONID") 
	    );
	        
	        http=http.exceptionHandling(ex -> ex
	                .authenticationEntryPoint((request, response, authException) -> {
	                    //String accept = request.getHeader("Accept");
	                    //if (accept != null && accept.contains("application/json")) {
	                        // return 401 for API calls
	                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	                    //} else {
	                        // default redirect to login page
	                     //   response.sendRedirect("/login");
	                    //}
	                })
	            );

	    return http.build();
	}
	
	@Bean
	AuthenticationFailureHandler customFailureHandler() {
	    // Must match the key used in the resolver
	    

	    return (request, response, exception) -> {
	    	boolean toSwagger=false;
	    	
	        HttpSession session = request.getSession(false);
	        if (session != null) {
	        	String source = (String) session.getAttribute("source");
	    		if(source!=null )
	    		{
	    			log.debug("source on fail from session={}", source);
	    			
	    			if(source.equals("swagger"))
	    			{
	    				toSwagger=true;
	    			}
	    			
	    			session.removeAttribute("source");
	    		}
	        }
	        // Delegate to the default logic or redirect to an error page
	        //response.sendRedirect("/login?error=" + exception.getMessage());
	        if(toSwagger)
	        {
	        	
    		 	response.sendRedirect("/swagger-ui/index.html");	
    		 	
	        }
	        
	        else
	        {
	        	response.sendRedirect("/");
	        }
	    };
	}
	
	@Bean
	AuthenticationSuccessHandler customSuccessHandler() {
	    return (request, response, authentication) -> {
	    	boolean toSwagger=false;
	    	
	    	boolean toFrontEnd=false;
	    	HttpSession session = request.getSession();
	    	log.debug("session is not null: {}", session != null);
	    	if(session!=null)
	    	{
	    		
	    		Enumeration<String> attributeNames = session.getAttributeNames();
	    		
	    		while(attributeNames.hasMoreElements()) {
	    			String attributeName = attributeNames.nextElement();
	    			Object attribute = session.getAttribute(attributeName);
	    			String cn=null;
	    			if(attribute!=null)
	    			{
	    				cn=attribute.getClass().getName();
	    			}
				log.debug("Session Attribute: {} @{}", attributeName, cn);
	    		}
	    		
	    		String source = (String) session.getAttribute("source");
	    		if(source!=null )
	    		{
	    			log.debug("source from session={}", source);
	    			if(source.equals("swagger"))
	    			{
	    				toSwagger=true;
	    			}
	    			
	    			else if(source.equals("frontend"))
	    			{
	    				toFrontEnd=true;
	    			}
	    		}
	    		
	    	}
	    	Enumeration<String> parameterNames = request.getParameterNames();
	    	
	    	while(parameterNames.hasMoreElements()) {
	    		String paramName = parameterNames.nextElement();
	    		String[] paramValues = request.getParameterValues(paramName);
			log.debug("Request Parameter: {}", paramName);
	    	}
	    	String state= request.getParameter("state");
	    	if(state!=null)
	    	{
	    		Decoder urlDecoder = Base64.getUrlDecoder();
	    		byte[] decoded = urlDecoder.decode(state);
	    		
	    		log.debug("Decoded state: {}", new String(decoded));
	    	}
	      
	       
	      
	    	 if(toSwagger)
		        {
	    		 	
	    		 	response.sendRedirect("/swagger-ui/index.html");	
	    		 	
		        	
		        }
	    	
	    	
	    	 else if(toFrontEnd)
		        {
	    		 if(feBaseUrl!=null) {
	    			 response.sendRedirect(feBaseUrl);
		    		}
		    		else
		    		{
		    			response.sendRedirect("/");
		    		}
		        	
		        }
		        else
		        {
		        	response.sendRedirect("/");
		        }
	    };
	}
	
	
	
	final static class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
		private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			/*
			 * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
			 * the CsrfToken when it is rendered in the response body.
			 */
			this.xor.handle(request, response, csrfToken);
			/*
			 * Render the token value to a cookie by causing the deferred token to be loaded.
			 */
			csrfToken.get();
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String headerValue = request.getHeader(csrfToken.getHeaderName());
			/*
			 * If the request contains a request header, use CsrfTokenRequestAttributeHandler
			 * to resolve the CsrfToken. This applies when a single-page application includes
			 * the header value automatically, which was obtained via a cookie containing the
			 * raw CsrfToken.
			 *
			 * In all other cases (e.g. if the request contains a request parameter), use
			 * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
			 * when a server-side rendered form includes the _csrf request parameter as a
			 * hidden input.
			 */
			return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
		}
	}
	
	
}