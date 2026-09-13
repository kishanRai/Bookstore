package org.example.bookstore.controllers.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.example.bookstore.dtos.cart.AddCartItemRequest;
import org.example.bookstore.dtos.cart.CartResponse;
import org.example.bookstore.dtos.cart.UpdateCartItemRequest;
import org.example.bookstore.application.cart.CartUseCase;
import org.example.bookstore.controllers.StoreResponseMapper;
import org.example.bookstore.services.authentication.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * Exposes cart operations for the authenticated customer and maps domain results to HTTP responses.
 */
@Tag(name = "Cart", description = "Persisted cart belonging to the authenticated customer.")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping( "/api/v1/cart" )
@RequiredArgsConstructor
public class CartController {

	private final CartUseCase cartService;
    private final CurrentUserService currentUser;

	/**
	 * Returns the authenticated customer's persisted cart and current server-calculated totals.
	 *
	 * @param authentication principal established by Spring Security
	 * @return the current cart response
	 */
	@Operation(summary = "Get my cart",
        description = "Returns items ordered by book ID, current catalog prices and server-calculated EUR totals. An empty cart returns empty items and zero totals.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Current cart", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401")
        })
    @GetMapping
	public CartResponse get( @Parameter(hidden = true) Authentication authentication ) {
		return StoreResponseMapper.cart(cartService.get(currentUser.get(authentication).id()));
	}

	/**
	 * Adds the validated book quantity to the authenticated customer's cart.
	 *
	 * @param authentication principal established by Spring Security
	 * @param request validated cart mutation body
	 * @return the updated cart response
	 */
	@Operation(summary = "Add copies to my cart",
        description = "Adds quantity to an existing line or creates a line. Each line supports 1 to 99 copies; the cart supports at most 100 distinct books. Repeating this POST adds copies again. Exceeding the combined quantity or capacity returns 409.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Updated cart", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Error403"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Error409")
        })
    @PostMapping( "/items" )
	public CartResponse add( @Parameter(hidden = true) Authentication authentication, @Valid @RequestBody AddCartItemRequest request ) {
		return StoreResponseMapper.cart(cartService.add(currentUser.get(authentication).id(), request.bookId(), request.quantity()));
	}

	/**
	 * Replaces an existing line quantity for the authenticated customer.
	 *
	 * @param authentication principal established by Spring Security
	 * @param bookId catalog book ID
	 * @param request validated cart mutation body
	 * @return the updated cart response
	 */
	@Operation(summary = "Set a cart quantity",
        description = "Replaces the quantity of an existing cart item with an integer from 1 to 99. A missing line returns 404. Use DELETE to remove an item.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Updated cart", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Error403"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
        })
    @PutMapping( "/items/{bookId}" )
	public CartResponse update( @Parameter(hidden = true) Authentication authentication, @Parameter(description = "Positive book ID from the catalog", example = "1") @PathVariable @Positive Long bookId,
								@Valid @RequestBody UpdateCartItemRequest request ) {
		return StoreResponseMapper.cart(cartService.changeQuantity(currentUser.get(authentication).id(), bookId, request.quantity()));
	}

	/**
	 * Removes a book from the authenticated customer's cart; repeated removal is harmless.
	 *
	 * @param authentication principal established by Spring Security
	 * @param bookId catalog book ID
	 */
	@Operation(summary = "Remove a cart item",
        description = "Removes only the current customer's line. Repeating removal of an absent item also returns 204.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Item absent; no response body", content = @Content),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Error403")
        })
    @DeleteMapping( "/items/{bookId}" )
	@ResponseStatus( HttpStatus.NO_CONTENT )
	public void remove( @Parameter(hidden = true) Authentication authentication, @Parameter(description = "Positive book ID from the catalog", example = "1") @PathVariable @Positive Long bookId ) {
		cartService.remove(currentUser.get(authentication).id(), bookId);
	}
}

