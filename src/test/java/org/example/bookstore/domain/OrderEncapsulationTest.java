package org.example.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.bookstore.entities.cart.CartItem;
import org.example.bookstore.entities.catalog.Book;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.junit.jupiter.api.Test;

/**
 * Protects saved order membership from mutations through collection getters.
 */
class OrderEncapsulationTest {
    @Test
    void callersCannotRemoveSavedOrderLinesThroughTheGetter() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getTitle()).thenReturn("A book");
        when(book.getAuthor()).thenReturn("An author");
        when(book.getPrice()).thenReturn(new BigDecimal("12.50"));
        when(book.getCurrency()).thenReturn("EUR");
        var order = new PurchaseOrder(7L, UUID.randomUUID(), Instant.EPOCH,
            List.of(new CartItem(7L, book, 2)));

        assertThatThrownBy(() -> order.getItems().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo("25.00");
    }
}
