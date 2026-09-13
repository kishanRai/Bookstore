package org.example.bookstore.services.authentication;

import lombok.RequiredArgsConstructor;
import org.example.bookstore.dtos.authentication.UserResponse;
import org.example.bookstore.repositories.authentication.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a trusted authenticated principal to a public customer identity.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {
	private final AppUserRepository appUserRepository;

	/**
	 * Resolves the authenticated principal to a persisted customer without trusting a client-supplied customer ID.
	 *
	 * @param authentication principal established by Spring Security
	 * @return the public customer identity
	 */
	@Transactional(readOnly = true)
	public UserResponse get(Authentication authentication) {
		var user = appUserRepository.findByEmail( authentication.getName())
			.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		return new UserResponse(user.getId(), user.getEmail());
	}

}
