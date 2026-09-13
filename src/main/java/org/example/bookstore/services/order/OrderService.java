package org.example.bookstore.services.order;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.bookstore.application.CustomerLock;
import org.example.bookstore.application.events.BusinessEvents;
import org.example.bookstore.application.order.CheckoutUseCase;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.example.bookstore.repositories.cart.CartItemRepository;
import org.example.bookstore.repositories.order.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates customer locking, idempotent checkout and atomic order/cart persistence.
 */
@Service
@RequiredArgsConstructor
public class OrderService implements CheckoutUseCase {
    private final PurchaseOrderRepository orders;
    private final CartItemRepository carts;
    private final CustomerLock customerLock;
    private final Clock clock;
    private final BusinessEvents events;

    /**
     * Locks the customer, checks for a prior key, then persists a snapshot and clears the cart atomically.
     * A retry returns the prior order before inspecting a newly filled cart. Keys last as long as orders.
     * Any persistence failure rolls back both changes; success observers run only after commit.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result checkout(long customerId, UUID idempotencyKey) {
        customerLock.acquire(customerId);
        var previous = orders.findByUserIdAndIdempotencyKey(customerId, idempotencyKey);
        if (previous.isPresent()) {
            events.afterCommit(BusinessEvents.Type.ORDER_REPLAYED, customerId, previous.get().getId());
            return new Result(previous.get(), false);
        }
        Cart cart = Cart.restore(customerId, carts.findCart(customerId));
        var order = PurchaseOrder.fromCart(cart, idempotencyKey, Instant.now(clock));
        orders.saveAndFlush(order);
        carts.deleteCart(customerId);
        events.afterCommit(BusinessEvents.Type.ORDER_CREATED, customerId, order.getId());
        return new Result(order, true);
    }

    /** Missing orders and orders belonging to another customer have the same not-found result. */
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder get(long customerId, long orderId) {
        return orders.findByIdAndUserId(orderId, customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
