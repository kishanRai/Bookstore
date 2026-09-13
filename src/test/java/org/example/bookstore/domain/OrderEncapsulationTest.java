package org.example.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.example.bookstore.entities.cart.CartItem;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.example.bookstore.support.BookTestDataBuilder;
import org.junit.jupiter.api.Test;

/**
 * Protects saved order membership from mutations through collection getters.
 */
class OrderEncapsulationTest {
    @Test
    void callersCannotRemoveSavedOrderLinesThroughTheGetter() {
        var book = BookTestDataBuilder.aBook().withId(1).withTitle("A book").withPrice("12.50").build();
        var order = new PurchaseOrder(7L, UUID.randomUUID(), Instant.EPOCH,
            List.of(new CartItem(7L, book, 2)));

        assertThatThrownBy(() -> order.getItems().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo("25.00");
    }
}
