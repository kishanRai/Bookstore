package org.example.bookstore.controllers.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;
import org.example.bookstore.dtos.order.OrderResponse;
import org.example.bookstore.application.order.CheckoutUseCase;
import org.example.bookstore.controllers.StoreResponseMapper;
import org.example.bookstore.services.authentication.CurrentUserService;
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

/**
 * Exposes idempotent checkout and owner-restricted retrieval of historical order summaries.
 */
@Tag(name = "Orders", description = "Atomic checkout and saved order summaries.")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping( "/api/v1/orders" )
@RequiredArgsConstructor
public class OrderController {

	private final CheckoutUseCase orderService;
    private final CurrentUserService currentUser;

	/**
	 * Checks out the authenticated cart using a customer-scoped idempotency key.
	 *
	 * @param authentication principal established by Spring Security
	 * @param idempotencyKey UUID scoped to the customer for checkout retries
	 * @return status 201 with Location for creation, or status 200 with the saved order for replay
	 */
	@Operation(summary = "Check out my cart",
        description = "No request body. Requires a UUID Idempotency-Key. Reserves stock, creates an order with saved book details and prices, and clears the cart in one transaction. Reusing the same key for the same customer returns the original order without consuming any newly added cart items or reserving stock again. Use a new key for a new purchase. Fails with 409 if any line's stock is insufficient, leaving stock and the cart unchanged. Records an order only; payment is outside scope.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Order created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)), headers = @Header(name = "Location", description = "Relative URL of the saved order", schema = @Schema(type = "string", example = "/api/v1/orders/1"))),
            @ApiResponse(responseCode = "200", description = "Previously created order replayed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)), headers = @Header(name = "Location", description = "Relative URL of the saved order", schema = @Schema(type = "string", example = "/api/v1/orders/1"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Error403"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Error409")
        })
    @PostMapping
	public ResponseEntity<OrderResponse> checkout( @Parameter(hidden = true) Authentication authentication, @Parameter(description = "UUID retained for retries of the same purchase; generate a new UUID for each new purchase", required = true, schema = @Schema(type = "string", format = "uuid")) @RequestHeader( "Idempotency-Key" ) UUID idempotencyKey ) {
		var result = orderService.checkout(currentUser.get(authentication).id(), idempotencyKey);
		return ResponseEntity.status( result.created() ? 201 : 200 )
			.location( URI.create( "/api/v1/orders/" + result.order().getId() ) )
			.body(StoreResponseMapper.order(result.order()));
	}

	/**
	 * Returns a saved order only for its authenticated owner.
	 *
	 * @param authentication principal established by Spring Security
	 * @param orderId saved order ID
	 * @return the historical order summary
	 */
	@Operation(summary = "Get my saved order",
        description = "Returns book details and prices captured at checkout. Missing orders and orders belonging to another customer both return 404.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Saved order summary", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
        })
    @GetMapping( "/{orderId}" )
	public OrderResponse get( @Parameter(hidden = true) Authentication authentication, @Parameter(description = "Positive order ID returned by checkout", example = "1") @PathVariable @Positive Long orderId ) {
		return StoreResponseMapper.order(orderService.get(currentUser.get(authentication).id(), orderId));
	}
}
