package org.example.bookstore.services.order;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.bookstore.application.order.StockReservationStrategy;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.example.bookstore.repositories.catalog.BookRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link StockReservationStrategy}: locks the book row for update, then reserves stock on
 * the same managed entity. Mirrors {@link org.example.bookstore.services.authentication.JpaCustomerLock}
 * so callers only ever coordinate through the caller's active transaction.
 */
@Component
@RequiredArgsConstructor
public class PessimisticStockReservationStrategy implements StockReservationStrategy {
	private final BookRepository books;
	private final EntityManager entityManager;

	/**
	 * Locks the book row before mutating it, so two concurrent checkouts cannot both observe
	 * sufficient stock and oversell the same book. The checkout's own cart lookup already loads
	 * each book unlocked for pricing, so the entity may already be managed with a stale quantity;
	 * once the row lock is held, refreshing discards that stale state before reserving against it.
	 *
	 * @param bookId catalog book ID
	 * @param quantity copies to reserve
	 */
	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void reserve(long bookId, int quantity) {
		var book = books.findByIdForUpdate(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
		entityManager.refresh(book);
		book.reserveStock(quantity);
	}
}
