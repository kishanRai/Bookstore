package org.example.bookstore.services.authentication;

import lombok.RequiredArgsConstructor;
import org.example.bookstore.dtos.authentication.UserResponse;
import org.example.bookstore.repositories.authentication.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
	private final AppUserRepository appUserRepository;

	@Transactional(readOnly = true)
	public UserResponse get(Authentication authentication) {
		var user = appUserRepository.findByEmail( authentication.getName())
			.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		return new UserResponse(user.getId(), user.getEmail());
	}
}
