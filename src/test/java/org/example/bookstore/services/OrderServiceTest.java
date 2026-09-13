package org.example.bookstore.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.bookstore.application.CustomerLock;
import org.example.bookstore.application.events.BusinessEvents;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.entities.cart.CartItem;
import org.example.bookstore.entities.catalog.Book;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.example.bookstore.exceptions.CartConflictException;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.example.bookstore.repositories.cart.CartItemRepository;
import org.example.bookstore.repositories.order.PurchaseOrderRepository;
import org.example.bookstore.services.order.OrderService;
import org.junit.jupiter.api.Test;

/**
 * Checks checkout orchestration, replay, ownership and failure behavior using isolated collaborators.
 */
class OrderServiceTest {
    private final PurchaseOrderRepository orders = mock(PurchaseOrderRepository.class);
    private final CartItemRepository carts = mock(CartItemRepository.class);
    private final CustomerLock lock = mock(CustomerLock.class);
    private final BusinessEvents events = mock(BusinessEvents.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC);
    private final OrderService service = new OrderService(orders, carts, lock, clock, events);

    private CartItem item() {
        var book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(book.getPrice()).thenReturn(new BigDecimal("12.50"));
        when(book.getCurrency()).thenReturn("EUR");
        return new CartItem(7L, book, 2);
    }

    @Test void replayDoesNotReadOrClearANewCart() {
        var key = UUID.randomUUID();
        var order = PurchaseOrder.fromCart(Cart.restore(7, List.of(item())), key, clock.instant());
        when(orders.findByUserIdAndIdempotencyKey(7L, key)).thenReturn(Optional.of(order));
        var result = service.checkout(7, key);
        assertThat(result.created()).isFalse();
        assertThat(result.order()).isSameAs(order);
        verify(lock).acquire(7);
        verifyNoInteractions(carts);
        verify(orders, never()).saveAndFlush(any());
        verify(events).afterCommit(BusinessEvents.Type.ORDER_REPLAYED, 7, null);
    }

    @Test void checkoutUsesTheClockAndCoordinatesPersistenceInOrder() {
        var key = UUID.randomUUID();
        var item = item();
        when(carts.findCart(7L)).thenReturn(List.of(item));
        var result = service.checkout(7, key);
        assertThat(result.created()).isTrue();
        assertThat(result.order().getCreatedAt()).isEqualTo(clock.instant());
        var sequence = inOrder(lock, orders, carts, events);
        sequence.verify(lock).acquire(7);
        sequence.verify(orders).findByUserIdAndIdempotencyKey(7L, key);
        sequence.verify(carts).findCart(7L);
        sequence.verify(orders).saveAndFlush(result.order());
        sequence.verify(carts).deleteCart(7L);
        sequence.verify(events).afterCommit(BusinessEvents.Type.ORDER_CREATED, 7, null);
    }

    @Test void emptyCartDoesNotCreateAnOrderOrEmitSuccess() {
        assertThatThrownBy(() -> service.checkout(7, UUID.randomUUID())).isInstanceOf(CartConflictException.class);
        verify(orders, never()).saveAndFlush(any());
        verify(carts, never()).deleteCart(anyLong());
        verifyNoInteractions(events);
    }

    @Test void failedCartDeletionDoesNotPublishSuccess() {
        var item = item();
        when(carts.findCart(7L)).thenReturn(List.of(item));
        doThrow(new IllegalStateException("Injected delete failure")).when(carts).deleteCart(7L);
        assertThatThrownBy(() -> service.checkout(7, UUID.randomUUID())).hasMessage("Injected delete failure");
        verifyNoInteractions(events);
        // Database rollback itself is verified by CartCheckoutIntegrationTest, not by mocks.
    }

    @Test void lookupIncludesCustomerOwnership() {
        assertThatThrownBy(() -> service.get(7, 123)).isInstanceOf(ResourceNotFoundException.class);
        verify(orders).findByIdAndUserId(123L, 7L);
    }
}
