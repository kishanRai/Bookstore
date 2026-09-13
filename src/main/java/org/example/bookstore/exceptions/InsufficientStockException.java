package org.example.bookstore.exceptions;

/**
 * Signals that fewer copies of a book remain in stock than a checkout line requires.
 */
public class InsufficientStockException extends BusinessRuleViolationException {

	/**
	 * Creates an insufficient-stock failure naming the book and the available/requested copies.
	 *
	 * @param title catalog book title
	 * @param available copies currently in stock
	 * @param requested copies the checkout line asked to reserve
	 */
	public InsufficientStockException( String title, int available, int requested ) {
		super( "Insufficient stock for \"" + title + "\": " + available + " available, " + requested + " requested" );
	}
}
