package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
	private static final Logger log = LoggerFactory.getLogger(CustomAuthorizationRequestResolver.class);

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        // 1. Let the default resolver do its work (find the client, build the base request)
        OAuth2AuthorizationRequest requestToOkta = this.defaultResolver.resolve(request);

        if (requestToOkta != null) {
            // 2. Perform your extra work here (e.g., adding custom parameters, logging, or setting headers)
            log.debug("Extra work: Request to Okta is being processed for client: {}", requestToOkta.getClientId());

            // Example Extra Work: Add a custom parameter (like 'source') to the request.
            // This parameter will be available for you to retrieve in the success handler.
            String source = request.getParameter("source");
            log.debug("Source parameter from incoming request: {}", source);
            return OAuth2AuthorizationRequest.from(requestToOkta)
                .additionalParameters(params -> {
                    // This is where you inject data into the request that goes to Okta
                	if(source!=null && !source.isEmpty()) {
                		 params.put("source", source);	
                		 
                		 HttpSession session = request.getSession(true);
						 session.setAttribute("source", source);
                	}
                	
                   
                })
                .build();
        }
        return null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return this.defaultResolver.resolve(request, clientRegistrationId);
    }
}
