package org.example.bookstore.controllers.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Authentication", description = "Registration, session login, current user and logout.")
@RestController
@RequestMapping( "/api/v1/authentication" )
@RequiredArgsConstructor
public class RegistrationController {

	private final RegistrationService registrationService;

	@Operation(summary = "Register a customer",
        description = "Creates an account without logging in. Email is normalized to lowercase and must be unique. Password is 12 to 128 characters and is never returned. Requires a session-bound CSRF token.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Customer registered", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Error403"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Error409")
        })
    @PostMapping( "/register" )
	@ResponseStatus( HttpStatus.CREATED )
	public UserResponse register( @Valid @RequestBody RegistrationRequest registrationRequest ) {
		return registrationService.register( registrationRequest );
	}

}
