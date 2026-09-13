package org.example.bookstore.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.example.bookstore.exceptions.CartConflictException;
import org.junit.jupiter.api.Test;

/**
 * Verifies independent historical snapshots and rejects invalid checkout construction.
 */
class OrderFactoryTest {
    @Test void factoryCapturesIndependentHistoricalDetails() {
        var book = CartTest.book(1, "12.50");
        var cart = Cart.empty(7);
        cart.add(book, 2);
        var key = UUID.randomUUID();
        var time = Instant.parse("2026-09-13T12:00:00Z");
        var order = PurchaseOrder.fromCart(cart, key, time);
        cart.changeQuantity(1, 5);
        when(book.getPrice()).thenReturn(new BigDecimal("99.00"));
        when(book.getTitle()).thenReturn("Changed title");
        cart.remove(1);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(order.getItems().get(0).getTitle()).isEqualTo("Book 1");
        assertThat(order.getTotal()).isEqualByComparingTo("25.00");
        assertThat(order.getCreatedAt()).isEqualTo(time);
        assertThat(order.getIdempotencyKey()).isEqualTo(key);
        assertThat(order.getUserId()).isEqualTo(7);
    }
    @Test void factoryRejectsEmptyCartAndMissingCheckoutMetadata() {
        assertThatThrownBy(() -> PurchaseOrder.fromCart(Cart.empty(7), UUID.randomUUID(), Instant.EPOCH))
            .isInstanceOf(CartConflictException.class);
        var cart = Cart.empty(7);
        cart.add(CartTest.book(1, "1.00"), 1);
        assertThatThrownBy(() -> PurchaseOrder.fromCart(cart, null, Instant.EPOCH)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PurchaseOrder.fromCart(cart, UUID.randomUUID(), null)).isInstanceOf(NullPointerException.class);
    }
}
