package org.example.bookstore.controllers.authentication;

import org.example.bookstore.dtos.authentication.RegistrationRequest;
import org.example.bookstore.dtos.authentication.UserResponse;
import org.example.bookstore.services.authentication.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping( "/api/v1/authentication" )
@RequiredArgsConstructor
public class RegistrationController {

	private final RegistrationService registrationService;

	@PostMapping( "/register" )
	@ResponseStatus( HttpStatus.CREATED )
	public UserResponse register( @Valid @RequestBody RegistrationRequest registrationRequest ) {
		return registrationService.register( registrationRequest );
	}

}
