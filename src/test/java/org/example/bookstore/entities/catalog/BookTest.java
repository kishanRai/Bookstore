package org.example.bookstore.entities.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.example.bookstore.exceptions.InsufficientStockException;
import org.junit.jupiter.api.Test;

/**
 * Exercises the stock-reservation invariant directly on the entity, without JPA or mocks.
 */
class BookTest {
    private Book book(int stockQuantity) {
        return new Book(1L, "Clean Code", "Robert C. Martin", new BigDecimal("35.00"), "EUR", stockQuantity);
    }

    @Test void reservingFewerCopiesThanAvailableLeavesTheRemainder() {
        var book = book(10);
        book.reserveStock(3);
        assertThat(book.getStockQuantity()).isEqualTo(7);
    }

    @Test void reservingExactlyTheAvailableStockLeavesZero() {
        var book = book(5);
        book.reserveStock(5);
        assertThat(book.getStockQuantity()).isZero();
    }

    @Test void reservingOneMoreThanAvailableIsRejectedAndLeavesStockUnchanged() {
        var book = book(2);
        assertThatThrownBy(() -> book.reserveStock(3)).isInstanceOf(InsufficientStockException.class)
            .hasMessage("Insufficient stock for \"Clean Code\": 2 available, 3 requested");
        assertThat(book.getStockQuantity()).isEqualTo(2);
    }

    @Test void reservingFromZeroStockIsRejected() {
        var book = book(0);
        assertThatThrownBy(() -> book.reserveStock(1)).isInstanceOf(InsufficientStockException.class);
        assertThat(book.getStockQuantity()).isZero();
    }

    @Test void reservingANonPositiveQuantityIsRejected() {
        var book = book(10);
        assertThatThrownBy(() -> book.reserveStock(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> book.reserveStock(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(book.getStockQuantity()).isEqualTo(10);
    }
}
