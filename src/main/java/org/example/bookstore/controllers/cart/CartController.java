package org.example.bookstore.controllers.cart;

import org.example.bookstore.dtos.cart.AddCartItemRequest;
import org.example.bookstore.dtos.cart.CartResponse;
import org.example.bookstore.dtos.cart.UpdateCartItemRequest;
import org.example.bookstore.services.cart.CartService;
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

@RestController
@RequestMapping( "/api/v1/cart" )
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public CartResponse get( Authentication authentication ) {
		return cartService.get( authentication );
	}

	@PostMapping( "/items" )
	public CartResponse add( Authentication authentication, @Valid @RequestBody AddCartItemRequest request ) {
		return cartService.add( authentication, request );
	}

	@PutMapping( "/items/{bookId}" )
	public CartResponse update( Authentication authentication, @PathVariable @Positive Long bookId,
								@Valid @RequestBody UpdateCartItemRequest request ) {
		return cartService.update( authentication, bookId, request );
	}

	@DeleteMapping( "/items/{bookId}" )
	@ResponseStatus( HttpStatus.NO_CONTENT )
	public void remove( Authentication authentication, @PathVariable @Positive Long bookId ) {
		cartService.remove( authentication, bookId );
	}
}

