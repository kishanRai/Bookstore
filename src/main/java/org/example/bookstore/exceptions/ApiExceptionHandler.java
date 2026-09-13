package org.example.bookstore.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthentication(AuthenticationException exception) {
		return ProblemDetail.forStatusAndDetail(
			HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	@ExceptionHandler( EmailAlreadyRegisteredException.class )
	public ProblemDetail handleEmailAlreadyRegisteredException( EmailAlreadyRegisteredException p_emailAlreadyRegisteredException ) {
		return ProblemDetail.forStatusAndDetail( HttpStatus.CONFLICT, p_emailAlreadyRegisteredException.getMessage() );
	}

}
