package org.example.bookstore.dtos.authentication;

public record CsrfResponse(
	String headerName,
	String token) {

}
