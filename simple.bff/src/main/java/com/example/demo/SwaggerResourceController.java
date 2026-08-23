package com.example.demo;



import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.annotation.PostConstruct;

@Controller
public class SwaggerResourceController {
	@GetMapping("swagger-ui")
    public String redirectToIndex() {
        return "redirect:/swagger-ui/index.html";
    }
	
	
	
	@GetMapping("swagger-ui/")
    public String redirectToIndex1() {
        return "redirect:/swagger-ui/index.html";
    }
	

	
	@Value("classpath:config/swagger-initializer.js")
    private Resource swaggerInitializerResource;

	private String swaggerInitializerJsContent;
	
	 @PostConstruct
	    void init() throws IOException {
		 swaggerInitializerJsContent=loadJs(swaggerInitializerResource, "swagger-initializer.js");
	    	
	    	
	    	
	    }

		private String loadJs(Resource resource, String fileName) throws IOException {
			try (Reader reader = new InputStreamReader(resource.getInputStream())) {
	            String jsContent = FileCopyUtils.copyToString(reader);
	            
	            return jsContent;
	        } catch (IOException e) {
	            // Handle error: return a 404 or log the error
	            throw new IOException("Could not read custom "+fileName+" file", e);
	        }
		}
		
		@GetMapping("/swagger-ui/swagger-initializer.js")
	    public ResponseEntity<String> getModifiedSwaggerInitializer() throws IOException {
	    	
	    	
	    	
	        // Return the custom JavaScript content with the correct MIME type
	        return ResponseEntity.ok()
	                .contentType(MediaType.parseMediaType("application/javascript"))
	                // Optional: set cache headers to prevent browser caching during development
	                .header("Cache-Control", "no-cache, no-store, must-revalidate")
	                .header("Pragma", "no-cache")
	                .body(swaggerInitializerJsContent);
	    }
		
		
		
	
	
}