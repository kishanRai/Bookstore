package org.example.bookstore.exceptions;

/**
 * Signals a cart capacity or checkout conflict that the HTTP adapter translates to status 409.
 */
public class CartConflictException extends RuntimeException {

	/**
	 * Creates a conflict with a client-safe business explanation.
	 *
	 * @param message client-safe explanation
	 */
	public CartConflictException( String message ) {
		super( message );
	}
}
