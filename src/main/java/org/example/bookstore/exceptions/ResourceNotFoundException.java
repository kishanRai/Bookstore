package org.example.bookstore.exceptions;

/**
 * Signals a missing or inaccessible resource using the same not-found HTTP outcome.
 */
public class ResourceNotFoundException extends RuntimeException {

	/**
	 * Creates a not-found failure with a client-safe resource description.
	 *
	 * @param message client-safe explanation
	 */
	public ResourceNotFoundException( String message ) {
		super( message );
	}
}
