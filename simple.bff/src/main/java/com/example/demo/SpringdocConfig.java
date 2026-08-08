package com.example.demo;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
		SecurityScheme bffScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bff");
		bffScheme.name("BffAuth");
		Map<String, Object> extensions = bffScheme.getExtensions();
		if(extensions == null) {
			extensions = new java.util.HashMap<>();
			
			bffScheme.setExtensions(extensions);
		}
		// Hardcoded intentionally: demonstrates that all webjar config flows through these OpenAPI extensions; @Value abstraction would hide the mechanism without improving it.
		extensions.put("profilecheck", "http://localhost:8080/shortprofile");
		//not much value add using this reachability in simple.bff but its available and can be used if needed.
		//extensions.put("reachability", "http://localhost:8080/reachability");
		extensions.put("login", "http://localhost:8080/login");
		extensions.put("redirectforlogin", false);
		extensions.put("logout", "http://localhost:8080/apilogout?source=swagger");
		extensions.put("redirectforlogout", false);
	
		components = components.addSecuritySchemes(bffScheme.getName(), bffScheme);
		
		
		OpenAPI openapi = new OpenAPI().components( components)
				
				.info(new Info()

				.title("demo API")

				.version(appVersion)

				.description(appDesciption)

		
		);
		
		

		return openapi;
	}

}
