package com.example.demo;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SpringdocConfig {
	private static final Logger log = LoggerFactory.getLogger(SpringdocConfig.class);
	
	@Bean
	public OpenApiCustomizer myCustomiser() {
		return openApi -> extracted(openApi);
	}
	
	private OpenAPI extracted(OpenAPI openApi) {
		Paths paths = openApi.getPaths();
		if (paths != null) {
			Set<Entry<String, PathItem>> entrySet = paths.entrySet();
			for (Entry<String, PathItem> entry : entrySet) {
				
				String key = entry.getKey();
				if(key.startsWith("/secured/")) {
					
					PathItem pathItem = entry.getValue();
					log.debug("Processing path: {}", key);
					Map<HttpMethod, Operation> operationsMap = pathItem.readOperationsMap();
					Set<Entry<HttpMethod, Operation>> operationEntrySet = operationsMap.entrySet();
					for (Entry<HttpMethod, Operation> operationEntry : operationEntrySet) {

						Operation operation = operationEntry.getValue();
						SecurityRequirement securityRequirement = new SecurityRequirement();
						securityRequirement.addList("BffAuth");
							    // List the scopes you want the user to authorize for this operation
							   // ,java.util.Arrays.asList("openid", "profile", "email", "offline_access"));
						
						operation.addSecurityItem(securityRequirement);
						
					}

				}
							}
		}
		return openApi;
	}
	
	

	@Bean

	public OpenAPI customOpenAPI(@Value("${application-description}") String appDesciption,
			@Value("${application-version}") String appVersion) {
		
		
		
		Components components = new Components();
		// Non-standard HTTP scheme name; "bff" (our innovation)  is a new custom contract read by the swagger-ui-bff webjar, not yet an IANA-registered auth scheme.
		// IANA's registry doesn't list Form login either.
		SecurityScheme bffScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bff");
		bffScheme.name("BffAuth");
		Map<String, Object> extensions = bffScheme.getExtensions();
		if(extensions == null) {
			extensions = new java.util.HashMap<>();
			
			bffScheme.setExtensions(extensions);
		}
		// There are many ways to eliminate this hardcoding (relative URLs, @Value, etc.).
		// Left as literals here so this method alone shows exactly how Swagger UI is
		// wired up for BFF — feel free to use whatever approach fits in your code.
		extensions.put("x-bff-profilecheck", "http://localhost:8081/shortprofile");
		extensions.put("x-bff-reachability", "http://localhost:8081/reachability");
		extensions.put("x-bff-login", "http://localhost:8081/oauth2/authorization/okta?source=swagger&prompt=consent");
		extensions.put("x-bff-redirectforlogin", true);
		extensions.put("x-bff-logout", "http://localhost:8081/apilogout?source=swagger");
		extensions.put("x-bff-redirectforlogout", true);
		
		
	

		components = components.addSecuritySchemes(bffScheme.getName(), bffScheme);
		
		
		OpenAPI openapi = new OpenAPI().components( components)
				//.addSecurityItem(new SecurityRequirement().addList("oidc")) // IMPORT
				
				.info(new Info()

				.title("demo API")

				.version(appVersion)

				.description(appDesciption)

		
		);
		
		

		return openapi;
	}

}
