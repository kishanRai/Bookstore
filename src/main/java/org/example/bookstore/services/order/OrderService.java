package org.example.bookstore.services.order;

import java.time.Instant;
import java.util.UUID;
import org.example.bookstore.dtos.order.OrderItemResponse;
import org.example.bookstore.dtos.order.OrderResponse;
import org.example.bookstore.entities.order.PurchaseOrder;
import org.example.bookstore.exceptions.CartConflictException;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.example.bookstore.repositories.cart.CartItemRepository;
import org.example.bookstore.repositories.order.PurchaseOrderRepository;
import org.example.bookstore.services.authentication.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final PurchaseOrderRepository orders;
	private final CartItemRepository cart;
	private final CurrentUserService currentUser;

	public record CheckoutResult(OrderResponse order, boolean created) {
	}

    /**
     * Places an order and clears the customer's cart in one database transaction.
     * The customer-row lock serializes this operation with all cart mutations.
     * An existing key is checked before reading the cart: retries return the saved
     * order and leave any newly filled cart untouched. Keys are retained with the order.
     * Book details and prices are copied into order lines; catalog changes do not
     * rewrite historical orders. Failure during persistence or cart deletion rolls back both.
     *
     * @param authentication trusted customer principal
     * @param idempotencyKey UUID reused for retries of one purchase, scoped to the customer
     * @return saved order and whether this request created it
     * @throws CartConflictException when a new key is used with an empty cart
     */
	@Transactional( isolation = Isolation.READ_COMMITTED )
	public CheckoutResult checkout( Authentication authentication, UUID idempotencyKey ) {
		Long userId = currentUser.lockCartOwner( authentication );
		var previous = orders.findByUserIdAndIdempotencyKey( userId, idempotencyKey );
		if ( previous.isPresent() )
			return new CheckoutResult( response( previous.get() ), false );

		var items = cart.findCart( userId );
		if ( items.isEmpty() )
			throw new CartConflictException( "Cannot check out an empty cart" );

		var order = new PurchaseOrder( userId, idempotencyKey, Instant.now(), items );
		orders.saveAndFlush( order );
		cart.deleteCart( userId );
		return new CheckoutResult( response( order ), true );
	}

    /**
     * Reads a saved summary using both order ID and authenticated customer ID.
     * Missing and foreign orders produce the same not-found result.
     *
     * @param authentication trusted customer principal
     * @param orderId requested order ID
     * @return the owner's saved order summary
     * @throws ResourceNotFoundException when no order belongs to this customer with that ID
     */
	@Transactional( readOnly = true )
	public OrderResponse get( Authentication authentication, Long orderId ) {
		Long userId = currentUser.get( authentication ).id();
		var order = orders.findByIdAndUserId( orderId, userId ).orElseThrow( () -> new ResourceNotFoundException( "Order not found" ) );
		return response( order );
	}

	private OrderResponse response( PurchaseOrder order ) {
		var items = order.getItems()
			.stream()
			.map( item -> new OrderItemResponse( item.getBookId(),
												 item.getTitle(),
												 item.getAuthor(),
												 item.getUnitPrice(),
												 item.getQuantity(),
												 item.lineTotal() ) )
			.toList();
		return new OrderResponse( order.getId(), order.getCreatedAt(), items, order.getTotal(), order.getCurrency() );
	}
}
