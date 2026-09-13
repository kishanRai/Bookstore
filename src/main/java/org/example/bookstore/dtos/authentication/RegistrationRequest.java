package org.example.bookstore.dtos.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
	@NotBlank(message = "Email must not be blank")
	@Email(message = "Email must be a valid email address")
	@Size(max = 254, message = "Email must not exceed 254 characters")
	String email,

	@NotBlank(message = "Password must not be blank")
	@Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters long")
	String password
) {

	@Override
	public String toString() {
		return "RegistrationRequest[REDACTED]";
	}
}
