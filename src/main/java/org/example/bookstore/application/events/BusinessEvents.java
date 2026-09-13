package org.example.bookstore.application.events;

/** Publishes successful business changes only after the enclosing transaction commits. */
public interface BusinessEvents {
    /**
     * Identifies successful operations that may be observed after transaction commit.
     */
    enum Type { CUSTOMER_REGISTERED, CART_ITEM_ADDED, CART_QUANTITY_CHANGED, CART_ITEM_REMOVED, ORDER_CREATED, ORDER_REPLAYED }
    /**
     * Registers a successful operation for publication only after the active transaction commits.
     *
     * @param type successful business operation to observe
     * @param customerId customer ID resolved by a trusted caller
     * @param orderId saved order ID, or null for non-order operations
     */
    void afterCommit(Type type, long customerId, Long orderId);
}
