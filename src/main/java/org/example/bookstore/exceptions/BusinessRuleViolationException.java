package org.example.bookstore.exceptions;

/**
 * Common supertype for domain rule violations that block an otherwise valid request.
 * {@link ApiExceptionHandler} maps every subtype to status 409 Conflict through one handler,
 * so new business-rule failures (see {@link CartConflictException}, {@link InsufficientStockException})
 * are translated polymorphically without adding another handler method per subtype.
 */
public abstract class BusinessRuleViolationException extends RuntimeException {

	/**
	 * Creates a business-rule failure with a client-safe explanation.
	 *
	 * @param message client-safe explanation
	 */
	protected BusinessRuleViolationException( String message ) {
		super( message );
	}
}
