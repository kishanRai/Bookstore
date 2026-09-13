package org.example.bookstore.configurations.security;

import java.util.Locale;
import org.example.bookstore.repositories.authentication.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Connects normalized customer lookup and password verification to Spring Security.
 */
@Configuration(proxyBeanMethods = false)
public class AuthenticationConfiguration {

	/**
	 * Adapts normalized email lookup to Spring Security without exposing stored password hashes through the API.
	 *
	 * @param appUserRepository repository for persisted customer accounts
	 * @return the database-backed user lookup strategy
	 */
	@Bean
	UserDetailsService userDetailsService( AppUserRepository  appUserRepository ) {
		return email -> {
			var user = appUserRepository.findByEmail(email.strip().toLowerCase( Locale.ROOT))
				.orElseThrow(() -> new UsernameNotFoundException( "Invalid email or password"));

			return User.withUsername( user.getEmail())
				.password(user.getPasswordHash())
				.roles("CUSTOMER")
				.build();
		};
	}

	/**
	 * Configures DAO authentication and keeps unknown-user failures indistinguishable from wrong passwords.
	 *
	 * @param userDetailsService database-backed credential lookup
	 * @param passwordEncoder encoder used for stored password verification
	 * @return the authentication manager using the shared password encoder
	 */
	@Bean
	AuthenticationManager authenticationManager(
		UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		var provider = new DaoAuthenticationProvider( userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		// Keep the default: unknown users become BadCredentialsException too.
		return new ProviderManager( provider);
	}

}
