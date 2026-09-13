package org.example.bookstore.controllers.authentication;

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

@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository securityContextRepository;
	private final CurrentUserService currentUserService;

	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

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

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		return currentUserService.get(authentication);
	}

	// POST /logout is handled by Spring Security's LogoutFilter.
}
