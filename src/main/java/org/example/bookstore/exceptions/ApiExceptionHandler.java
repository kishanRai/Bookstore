package org.example.bookstore.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler( EmailAlreadyRegisteredException.class )
	public ProblemDetail handleEmailAlreadyRegisteredException( EmailAlreadyRegisteredException p_emailAlreadyRegisteredException ) {
		return ProblemDetail.forStatusAndDetail( HttpStatus.CONFLICT, p_emailAlreadyRegisteredException.getMessage() );
	}

}
