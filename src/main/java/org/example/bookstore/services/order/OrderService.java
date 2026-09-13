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
