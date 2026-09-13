package org.example.bookstore.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates controller and business failures into consistent HTTP Problem Details responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Maps domain quantity violations to status 400 Problem Details.
     *
     * @param exception failure being inspected or translated
     * @return the invalid-quantity response
     */
    @ExceptionHandler(InvalidCartQuantityException.class)
    public ProblemDetail invalidQuantity(InvalidCartQuantityException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }


	/**
	 * Maps authentication failures to the same generic status 401 response.
	 *
	 * @param exception failure being inspected or translated
	 * @return Problem Details without credential or account-existence information
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthentication(AuthenticationException exception) {
		return ProblemDetail.forStatusAndDetail(
			HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	/**
	 * Maps duplicate normalized email registration to status 409.
	 *
	 * @param p_emailAlreadyRegisteredException duplicate registration failure
	 * @return the duplicate-registration Problem Details response
	 */
	@ExceptionHandler( EmailAlreadyRegisteredException.class )
	public ProblemDetail handleEmailAlreadyRegisteredException( EmailAlreadyRegisteredException p_emailAlreadyRegisteredException ) {
		return ProblemDetail.forStatusAndDetail( HttpStatus.CONFLICT, p_emailAlreadyRegisteredException.getMessage() );
	}

	/**
	 * Maps missing and inaccessible resources to status 404.
	 *
	 * @param exception failure being inspected or translated
	 * @return the not-found Problem Details response
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	/**
	 * Maps every business-rule violation (cart capacity, empty checkout, insufficient stock, ...)
	 * to status 409 through their common supertype, rather than one handler per concrete subtype.
	 *
	 * @param exception failure being inspected or translated
	 * @return the business-rule-conflict Problem Details response
	 */
	@ExceptionHandler(BusinessRuleViolationException.class)
	public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}

}
