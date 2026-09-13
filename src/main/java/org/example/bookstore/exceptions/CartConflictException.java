package org.example.bookstore.exceptions;

public class CartConflictException extends RuntimeException {

	public CartConflictException( String message ) {
		super( message );
	}
}
