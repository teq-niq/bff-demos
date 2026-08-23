package com.example.demo;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
	private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);
	// This flexibility (runtime system property of febaseurl) was chosen for this
	// demo app,
	// so it can show both split-origin dev mode and single-origin deployment.
	// Do whatever makes sense for your own app.
	@Value("${febaseurl:#{null}}")
	private String feBaseUrl; // (null if not set)

	@PostConstruct
	private void init() {
		if (feBaseUrl != null && feBaseUrl.trim().length() == 0) {
			feBaseUrl = null;
		}

	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService(PasswordEncoder encoder) {
		UserDetails user = User.builder().username("user").password(encoder.encode("password")).roles("myuser").build();
		UserDetails admin = User.builder().username("admin").password(encoder.encode("password")).roles("myadmin")
				.build();

		return new InMemoryUserDetailsManager(user, admin);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		log.debug("feBaseUrl=[{}]", feBaseUrl);

		boolean feBaseUrlIsNotNull = feBaseUrl != null;

		if (feBaseUrlIsNotNull) {

			log.debug("CORS enabled for febaseurl: {}", feBaseUrl);

			Customizer<CorsConfigurer<HttpSecurity>> corsCustomizer = new Customizer<CorsConfigurer<HttpSecurity>>() {

				@Override
				public void customize(CorsConfigurer<HttpSecurity> http) {

					http.configurationSource(request -> {
						CorsConfiguration cors = new org.springframework.web.cors.CorsConfiguration();
						if (feBaseUrlIsNotNull) {
							cors.addAllowedOrigin(feBaseUrl);
							log.debug("added feBaseUrl to CORS:{} for request URL:{}", feBaseUrl,
									request.getRequestURL());
						}

						// Broad for demo simplicity; tighten to only the methods/headers you actually
						// use in your code if so needed.
						cors.addAllowedMethod("*");
						cors.addAllowedHeader("*");
						cors.setAllowCredentials(true);
						return cors;
					});
				}
			};

			http = http.cors(corsCustomizer);
			http = http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
					.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()));
			log.debug("CSRF protection is enabled");
		} else {
			log.debug(
					"The application is self-contained, CORS remains deny-by-default and CSRF protection is enabled.");

			http = http

					.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

							.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())

					);

		}

		http = http.authorizeHttpRequests(auth -> auth.requestMatchers("/secured/profile").authenticated()

				.requestMatchers("/secured/admin").hasRole("myadmin") // ROLE_myadmin
				.requestMatchers("/secured/user").hasRole("myuser") // ROLE_myuser

				.anyRequest().permitAll() // everything else allowed
		).formLogin(form -> form.loginProcessingUrl("/login").successHandler((req, res, auth) -> res.setStatus(200))
				.failureHandler((req, res, ex) -> res.sendError(401)))
				.logout(logout -> logout.logoutUrl("/logout")
						.logoutSuccessHandler((req, res, auth) -> res.setStatus(200))
				// logoutFailureHandler is not needed here. What could really go wrong in our
				// simple scenario. default behaviour is fine.
				).httpBasic(Customizer.withDefaults()); // TODO: httpBasic remove — not needed here. The BFF Swagger
														// plugin handles login via
														// form login, and curl/Postman/API tools can do the same. The
														// only thing
														// httpBasic() adds is support for vanilla Swagger UI's
														// "Authorize" button,
														// which this project does not use.
														// can use in vanilla swagger behaviour examples.
														// retaining. will be removed in future versions of this demo.

		http = http.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {

			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

		}));

		return http.build();
	}

	final static class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
		private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			/*
			 * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection
			 * of the CsrfToken when it is rendered in the response body.
			 */
			this.xor.handle(request, response, csrfToken);
			/*
			 * Render the token value to a cookie by causing the deferred token to be
			 * loaded.
			 */
			csrfToken.get();
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String headerValue = request.getHeader(csrfToken.getHeaderName());
			/*
			 * If the request contains a request header, use
			 * CsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies when
			 * a single-page application includes the header value automatically, which was
			 * obtained via a cookie containing the raw CsrfToken.
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
