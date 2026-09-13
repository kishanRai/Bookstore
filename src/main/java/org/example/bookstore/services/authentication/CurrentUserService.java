package org.example.bookstore.services.authentication;

import lombok.RequiredArgsConstructor;
import org.example.bookstore.dtos.authentication.UserResponse;
import org.example.bookstore.repositories.authentication.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    /**
     * Locks the authenticated customer's row until the caller's transaction ends.
     * All cart operations and checkout acquire this same lock, including an empty cart,
     * so concurrent requests for one customer cannot lose updates or create duplicate orders.
     * Different customers lock different rows. This does not lock catalog prices.
     *
     * @param authentication trusted principal supplied by Spring Security
     * @return persisted customer ID used for ownership checks
     * @throws org.springframework.transaction.IllegalTransactionStateException when no transaction exists
     */
	@Transactional(propagation = Propagation.MANDATORY)
	public Long lockCartOwner(Authentication authentication) {
		return appUserRepository.findByEmailForUpdate(authentication.getName())
			.orElseThrow(() -> new BadCredentialsException("Invalid email or password"))
			.getId();
	}
}
