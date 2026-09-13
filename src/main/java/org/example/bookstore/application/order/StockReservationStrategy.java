package org.example.bookstore.application.order;

import org.example.bookstore.exceptions.InsufficientStockException;

/**
 * Strategy for reserving catalog stock during checkout. Concrete implementations decide how
 * concurrent reservations for the same book are serialized (for example, a pessimistic database
 * lock); {@link org.example.bookstore.services.order.OrderService} depends only on this contract.
 */
public interface StockReservationStrategy {

	/**
	 * Reserves copies of a book, decrementing available stock for a single checkout line.
	 *
	 * @param bookId catalog book ID
	 * @param quantity copies to reserve
	 * @throws InsufficientStockException if fewer copies remain than requested
	 */
	void reserve(long bookId, int quantity);
}
