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

@Configuration(proxyBeanMethods = false)
public class AuthenticationConfiguration {

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

	@Bean
	AuthenticationManager authenticationManager(
		UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		var provider = new DaoAuthenticationProvider( userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		// Keep the default: unknown users become BadCredentialsException too.
		return new ProviderManager( provider);
	}

}
