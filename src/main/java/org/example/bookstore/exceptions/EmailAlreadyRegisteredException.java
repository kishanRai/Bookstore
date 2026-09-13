package org.example.bookstore.exceptions;

/**
 * Signals that the normalized registration email already belongs to an account.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

	/**
	 * Creates a duplicate-registration failure naming the submitted email.
	 *
	 * @param email normalized customer email
	 */
	public EmailAlreadyRegisteredException(String email) {
		super("Email already registered: " + email);
	}

}
