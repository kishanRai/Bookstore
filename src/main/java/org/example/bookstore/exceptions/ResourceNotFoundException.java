package org.example.bookstore.exceptions;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException( String message ) {
		super( message );
	}
}
