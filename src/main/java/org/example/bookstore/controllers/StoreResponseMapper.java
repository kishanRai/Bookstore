package org.example.bookstore.controllers;

import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.dtos.cart.CartItemResponse;
import org.example.bookstore.dtos.cart.CartResponse;
import org.example.bookstore.dtos.order.OrderItemResponse;
import org.example.bookstore.dtos.order.OrderResponse;
import org.example.bookstore.entities.order.PurchaseOrder;

/** HTTP adapter mapping. Domain totals are read, never recalculated from transport DTOs. */
public final class StoreResponseMapper {
    /**
     * Prevents instantiation of the stateless HTTP mapping utility.
     */
    private StoreResponseMapper() { }
    /**
     * Maps cart lines and domain-calculated totals to the HTTP representation.
     *
     * @param cart validated domain cart
     * @return the cart response without persistence internals
     */
    public static CartResponse cart(Cart cart) {
        var items = cart.items().stream().map(item -> new CartItemResponse(item.getBook().getId(),
            item.getBook().getTitle(), item.getBook().getAuthor(), item.getBook().getPrice(),
            item.getQuantity(), item.lineTotal().amount())).toList();
        var total = cart.total();
        return new CartResponse(items, cart.totalQuantity(), total.amount(), total.currency().name());
    }
    /**
     * Maps saved historical lines and totals to the public order representation.
     *
     * @param order historical order aggregate
     * @return the order response without persistence internals
     */
    public static OrderResponse order(PurchaseOrder order) {
        var items = order.getItems().stream().map(item -> new OrderItemResponse(item.getBookId(),
            item.getTitle(), item.getAuthor(), item.getUnitPrice(), item.getQuantity(), item.lineTotal())).toList();
        return new OrderResponse(order.getId(), order.getCreatedAt(), items, order.getTotal(), order.getCurrency());
    }
}
