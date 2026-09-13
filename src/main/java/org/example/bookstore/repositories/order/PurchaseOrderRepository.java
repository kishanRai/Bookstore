package org.example.bookstore.repositories.order;

import java.util.Optional;
import java.util.UUID;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Retrieves orders with historical lines using owner-scoped identities and idempotency keys.
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	/**
	 * Fetches an earlier order and its lines using the customer-scoped checkout key.
	 *
	 * @param userId owning customer ID
	 * @param idempotencyKey UUID scoped to the customer for checkout retries
	 * @return the historical order when the key has already been used
	 */
	@EntityGraph( attributePaths = "items" )
	Optional<PurchaseOrder> findByUserIdAndIdempotencyKey( Long userId, UUID idempotencyKey );

	/**
	 * Fetches an order and its lines only when both identity and owner match.
	 *
	 * @param id persisted resource ID
	 * @param userId owning customer ID
	 * @return the owned order, or an empty optional
	 */
	@EntityGraph( attributePaths = "items" )
	Optional<PurchaseOrder> findByIdAndUserId( Long id, Long userId );
}
