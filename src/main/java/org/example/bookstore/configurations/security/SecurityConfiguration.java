package org.example.bookstore.configurations.security;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
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

@Configuration( proxyBeanMethods = false )
public class SecurityConfiguration {

	@Bean
	SecurityContextRepository securityContextRepository() {
		var repository = new HttpSessionSecurityContextRepository();
		repository.setDisableUrlRewriting( true );
		return repository;
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy( CsrfTokenRepository csrfTokenRepository ) {
		return new CompositeSessionAuthenticationStrategy( List.of( new ChangeSessionIdAuthenticationStrategy(),
																	new CsrfAuthenticationStrategy( csrfTokenRepository ) ) );
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		var cors = new CorsConfiguration();
		cors.setAllowedOrigins( List.of( "http://localhost:5173", "http://localhost:3000" ) );
		cors.setAllowedMethods( List.of( "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" ) );
		cors.setAllowedHeaders( List.of( "Content-Type", "Accept", "X-CSRF-TOKEN" ) );
		cors.setAllowCredentials( true );
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration( "/**", cors );
		return source;
	}

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
	private static void writeProblem( HttpServletResponse response, int status, String title, String detail ) throws IOException {
		response.setStatus( status );
		response.setContentType( "application/problem+json" );
		response.setCharacterEncoding( "UTF-8" );
		response.getWriter().write( """
										{"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
										""".formatted( title, status, detail ) );
	}
}
