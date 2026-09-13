package org.example.bookstore.domain.cart;

/** Fixed business invariants, also enforced by the existing database constraints. */
public final class CartLimits {
    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 99;
    public static final int MAX_DISTINCT_BOOKS = 100;
    /**
     * Prevents instantiation of the shared cart-limit constants.
     */
    private CartLimits() { }
}
