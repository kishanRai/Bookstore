package org.example.bookstore.controllers.order;

import java.net.URI;
import java.util.UUID;
import org.example.bookstore.dtos.order.OrderResponse;
import org.example.bookstore.services.order.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping( "/api/v1/orders" )
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<OrderResponse> checkout( Authentication authentication, @RequestHeader( "Idempotency-Key" ) UUID idempotencyKey ) {
		var result = orderService.checkout( authentication, idempotencyKey );
		return ResponseEntity.status( result.created() ? 201 : 200 )
			.location( URI.create( "/api/v1/orders/" + result.order().id() ) )
			.body( result.order() );
	}

	@GetMapping( "/{orderId}" )
	public OrderResponse get( Authentication authentication, @PathVariable @Positive Long orderId ) {
		return orderService.get( authentication, orderId );
	}
}
