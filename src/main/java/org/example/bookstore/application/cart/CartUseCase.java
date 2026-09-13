package org.example.bookstore.application.cart;

import org.example.bookstore.domain.cart.Cart;

/** Application boundary. Customer IDs must be resolved from a trusted authenticated principal. */
public interface CartUseCase {
    /**
     * Loads the current customer cart, including an empty cart when it has no lines.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @return the customer cart and current catalog prices
     */
    Cart get(long customerId);
    /**
     * Adds copies of a book, merging an existing line within quantity and capacity limits.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param bookId catalog book ID
     * @param quantity requested copy count
     * @return the cart after the addition
     */
    Cart add(long customerId, long bookId, int quantity);
    /**
     * Replaces the quantity of an existing cart line within the domain limits.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param bookId catalog book ID
     * @param quantity requested copy count
     * @return the cart after replacement
     */
    Cart changeQuantity(long customerId, long bookId, int quantity);
    /**
     * Removes a book from the customer cart; an absent line is treated as already removed.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param bookId catalog book ID
     */
    void remove(long customerId, long bookId);
}
