package org.example.bookstore.application.order;

import java.util.UUID;
import org.example.bookstore.entities.order.PurchaseOrder;

/** Order application boundary; contains no HTTP DTOs or Spring Security types. */
public interface CheckoutUseCase {
    /**
     * Carries the saved order and whether this checkout created it or replayed an earlier result.
     */
    record Result(PurchaseOrder order, boolean created) { }
    /**
     * Creates an order atomically with cart clearing, or returns the earlier order for the same customer/key.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param idempotencyKey UUID scoped to the customer for checkout retries
     * @return the saved order and whether it was newly created
     */
    Result checkout(long customerId, UUID idempotencyKey);
    /**
     * Retrieves an order only when it belongs to the requesting customer.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param orderId saved order ID
     * @return the historical order with its saved lines
     */
    PurchaseOrder get(long customerId, long orderId);
}
