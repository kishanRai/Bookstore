package org.example.bookstore.controllers.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Locale;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.bookstore.dtos.authentication.CsrfResponse;
import org.example.bookstore.dtos.authentication.LoginRequest;
import org.example.bookstore.dtos.authentication.UserResponse;
import org.example.bookstore.services.authentication.CurrentUserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Registration, session login, current user and logout.")
@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository securityContextRepository;
	private final CurrentUserService currentUserService;

	@Operation(summary = "Get a CSRF token",
        description = "Retain the session cookie and send the returned headerName and token with mutations. Fetch again after login or logout; the bookstore Swagger page does this automatically.",
        responses = {
            @ApiResponse(responseCode = "200", description = "CSRF header name and token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CsrfResponse.class)))
        })
    @GetMapping("/csrf")
	public CsrfResponse csrf(@Parameter(hidden = true) CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	@Operation(summary = "Log in",
        description = "Authenticates using email and password, rotates the session ID and clears the previous CSRF token. The browser retains JSESSIONID automatically. Unknown users and incorrect passwords return the same generic 401 response.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authenticated customer; session cookie updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Error403")
        })
    @PostMapping("/login")
	public UserResponse login(@Valid @RequestBody LoginRequest body,
							  HttpServletRequest request, HttpServletResponse response) {
		var token = UsernamePasswordAuthenticationToken.unauthenticated(
			body.email().strip().toLowerCase(Locale.ROOT), body.password());
		Authentication authentication = authenticationManager.authenticate(token);
		UserResponse user = currentUserService.get(authentication);

		// A controller-based login must perform these steps explicitly.
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		return user;
	}

	@Operation(summary = "Get the current customer",
        description = "Returns the customer associated with the current session.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authenticated customer", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401")
        })
    @SecurityRequirement(name = "sessionAuth")
    @GetMapping("/me")
	public UserResponse me(@Parameter(hidden = true) Authentication authentication) {
		return currentUserService.get(authentication);
	}

	// POST /logout is handled by Spring Security's LogoutFilter.
}
