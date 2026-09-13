package org.example.bookstore.configurations.security;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines session authentication, CSRF, credentialed CORS and JSON security error responses.
 */
@Configuration( proxyBeanMethods = false )
public class SecurityConfiguration {

	/**
	 * Stores authentication in the HTTP session with URL-based session rewriting disabled.
	 *
	 * @return the session security-context repository
	 */
	@Bean
	SecurityContextRepository securityContextRepository() {
		var repository = new HttpSessionSecurityContextRepository();
		repository.setDisableUrlRewriting( true );
		return repository;
	}

	/**
	 * Stores CSRF token state in the same HTTP session used by the client.
	 *
	 * @return the session-backed CSRF repository
	 */
	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}

	/**
	 * Combines session-ID rotation with CSRF-token invalidation after successful login.
	 *
	 * @param csrfTokenRepository session-bound CSRF storage
	 * @return the strategies invoked by JSON login
	 */
	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy( CsrfTokenRepository csrfTokenRepository ) {
		return new CompositeSessionAuthenticationStrategy( List.of( new ChangeSessionIdAuthenticationStrategy(),
																	new CsrfAuthenticationStrategy( csrfTokenRepository ) ) );
	}

	/**
	 * Allows credentialed requests from configured browser origins and exposes order/correlation headers.
	 *
	 * @param allowedOrigins explicit browser origins permitted to send credentialed requests
	 * @return the CORS policy for application paths
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
        @Value("${bookstore.cors.allowed-origins}") List<String> allowedOrigins) {
		var cors = new CorsConfiguration();
		cors.setAllowedOrigins(allowedOrigins);
        cors.setExposedHeaders(List.of("Location", "X-Correlation-ID"));
		cors.setAllowedMethods( List.of( "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" ) );
		cors.setAllowedHeaders( List.of( "Content-Type", "Accept", "X-CSRF-TOKEN", "Idempotency-Key") );
		cors.setAllowCredentials( true );
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration( "/**", cors );
		return source;
	}

	/**
	 * Configures public routes, protected operations, CSRF, session logout and JSON security failures.
	 *
	 * @param http Spring Security filter builder
	 * @param contextRepository session-backed security context storage
	 * @param csrfRepository session-bound CSRF token storage
	 * @param corsSource credentialed browser-origin policy
	 * @return the application security filter chain
	 * @throws Exception if Spring Security cannot build the configured filter chain
	 */
	@Bean
	SecurityFilterChain securityFilterChain( HttpSecurity http, SecurityContextRepository contextRepository, CsrfTokenRepository csrfRepository,
											 @Qualifier( "corsConfigurationSource" ) CorsConfigurationSource corsSource ) throws Exception {

		http.cors( cors -> cors.configurationSource( corsSource ) )
			.csrf( csrf -> csrf.csrfTokenRepository( csrfRepository ) )
			.securityContext( context -> context.securityContextRepository( contextRepository ) )
			.sessionManagement( session -> session.sessionCreationPolicy( SessionCreationPolicy.IF_REQUIRED ) )
			.requestCache( AbstractHttpConfigurer::disable )
			.formLogin( AbstractHttpConfigurer::disable )
			.httpBasic( AbstractHttpConfigurer::disable )
			.authorizeHttpRequests( authorize -> authorize.dispatcherTypeMatchers( DispatcherType.ERROR )
				.permitAll()
				.requestMatchers( HttpMethod.GET, "/docs/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml" )
				.permitAll()
				.requestMatchers( HttpMethod.GET, "/api/v1/books", "/actuator/health", "/actuator/health/**", "/api/v1/authentication/csrf" )
				.permitAll()
				.requestMatchers( HttpMethod.POST, "/api/v1/authentication/register", "/api/v1/authentication/login" )
				.permitAll()
				.anyRequest()
				.authenticated() )
			.exceptionHandling( errors -> errors.authenticationEntryPoint( ( request, response, exception ) -> writeProblem( response,
																															 401,
																															 "Unauthorized",
																															 "Authentication required" ) )
				.accessDeniedHandler( ( request, response, exception ) -> writeProblem( response,
																						403,
																						"Forbidden",
																						"Access denied or invalid CSRF token" ) ) )
			.logout( logout -> logout.logoutUrl( "/api/v1/authentication/logout" )
				.invalidateHttpSession( true )
				.clearAuthentication( true )
				.deleteCookies( "JSESSIONID" )
				.logoutSuccessHandler( ( request, response, authentication ) -> response.setStatus( HttpServletResponse.SC_NO_CONTENT ) ) );

		return http.build();
	}

	// All arguments are fixed application constants, never request or exception text.
	/**
	 * Writes a minimal security-layer Problem Details response using fixed application messages.
	 *
	 * @param response outgoing HTTP response
	 * @param status HTTP status code
	 * @param title client-safe response title
	 * @param detail fixed client-safe error detail
	 * @throws IOException if the response body cannot be written
	 */
	private static void writeProblem( HttpServletResponse response, int status, String title, String detail ) throws IOException {
		response.setStatus( status );
		response.setContentType( "application/problem+json" );
		response.setCharacterEncoding( "UTF-8" );
		response.getWriter().write( """
										{"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
										""".formatted( title, status, detail ) );
	}
}
