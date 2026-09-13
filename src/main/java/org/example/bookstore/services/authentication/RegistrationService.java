package org.example.bookstore.services.authentication;

import java.util.Locale;
import org.example.bookstore.dtos.authentication.RegistrationRequest;
import org.example.bookstore.dtos.authentication.UserResponse;
import org.example.bookstore.entities.authentication.AppUser;
import org.example.bookstore.exceptions.EmailAlreadyRegisteredException;
import org.example.bookstore.repositories.authentication.AppUserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

    /**
     * Normalizes the email and stores an encoded password in one transaction.
     * The database unique constraint arbitrates concurrent registrations; only an
     * email-constraint violation is translated to a duplicate-account error.
     *
     * @param registrationRequest validated email and password; password is not normalized
     * @return public customer details, without password or hash
     * @throws EmailAlreadyRegisteredException when the normalized email already exists
     */
	@Transactional
	public UserResponse register( RegistrationRequest registrationRequest ){
		String email = registrationRequest.email().strip().toLowerCase( Locale.ROOT );

		AppUser appUser = new AppUser( email, passwordEncoder.encode( registrationRequest.password() ) );

		try{
			AppUser savedAppUser = appUserRepository.saveAndFlush( appUser );
			return new UserResponse( savedAppUser.getId(), savedAppUser.getEmail() );
		}
		catch( DataIntegrityViolationException e ){
			if (isDuplicateEmail(e)) {
				throw new EmailAlreadyRegisteredException(email);
			}

			throw e;
		}
	}

	private boolean isDuplicateEmail(Throwable exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException violation
				&& "uk_app_users_email".equals(
				violation.getConstraintName()
			)) {
				return true;
			}

			cause = cause.getCause();
		}

		return false;
	}

}
