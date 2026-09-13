package org.example.bookstore.repositories.order;

import java.util.Optional;
import java.util.UUID;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	@EntityGraph( attributePaths = "items" )
	Optional<PurchaseOrder> findByUserIdAndIdempotencyKey( Long userId, UUID idempotencyKey );

	@EntityGraph( attributePaths = "items" )
	Optional<PurchaseOrder> findByIdAndUserId( Long id, Long userId );
}
