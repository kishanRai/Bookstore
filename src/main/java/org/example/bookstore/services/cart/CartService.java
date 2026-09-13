package org.example.bookstore.services.cart;

import java.math.BigDecimal;
import org.example.bookstore.dtos.cart.AddCartItemRequest;
import org.example.bookstore.dtos.cart.CartItemResponse;
import org.example.bookstore.dtos.cart.CartResponse;
import org.example.bookstore.dtos.cart.UpdateCartItemRequest;
import org.example.bookstore.entities.cart.CartItem;
import org.example.bookstore.exceptions.CartConflictException;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.example.bookstore.repositories.cart.CartItemRepository;
import org.example.bookstore.repositories.catalog.BookRepository;
import org.example.bookstore.services.authentication.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Manages a customer's persisted cart using current catalog prices.
 * Every operation locks the customer row, including reads, to serialize cart access
 * with checkout. This deliberately trades per-customer concurrency for simple consistency.
 * HTTP validation bounds individual quantities; this service also enforces the combined
 * limit of 99 copies per book and 100 distinct books. Prices and ownership come from the server.
 */
@Service
@RequiredArgsConstructor
@Transactional( isolation = Isolation.READ_COMMITTED )
public class CartService {

	private static final int MAX_DISTINCT_BOOKS = 100;
	private static final int MAX_QUANTITY = 99;
	private final CartItemRepository items;
	private final BookRepository books;
	private final CurrentUserService currentUser;

	public CartResponse get( Authentication authentication ) {
		// Serialize reads too, so the returned cart cannot overlap a checkout.
		return response( currentUser.lockCartOwner( authentication ) );
	}

	public CartResponse add( Authentication authentication, AddCartItemRequest request ) {
		Long userId = currentUser.lockCartOwner( authentication );
		var existing = items.findByUserIdAndBook_Id( userId, request.bookId() );
		if ( existing.isPresent() ) {
			CartItem item = existing.get();
			int quantity = item.getQuantity() + request.quantity();
			if ( quantity > MAX_QUANTITY ) {
				throw new CartConflictException( "A book cannot have more than 99 copies in the cart" );
			}
			item.changeQuantity( quantity );
		}
		else {
			var book = books.findById( request.bookId() ).orElseThrow( () -> new ResourceNotFoundException( "Book not found" ) );
			if ( items.countByUserId( userId ) >= MAX_DISTINCT_BOOKS ) {
				throw new CartConflictException( "Cart cannot contain more than 100 distinct books" );
			}
			items.save( new CartItem( userId, book, request.quantity() ) );
		}
		return response( userId );
	}

	public CartResponse update( Authentication authentication, Long bookId, UpdateCartItemRequest request ) {
		Long userId = currentUser.lockCartOwner( authentication );
		var item = items.findByUserIdAndBook_Id( userId, bookId ).orElseThrow( () -> new ResourceNotFoundException( "Cart item not found" ) );
		item.changeQuantity( request.quantity() );
		return response( userId );
	}

	public void remove( Authentication authentication, Long bookId ) {
		Long userId = currentUser.lockCartOwner( authentication );
		items.findByUserIdAndBook_Id( userId, bookId ).ifPresent( items::delete );
	}

	private CartResponse response( Long userId ) {
		var lines = items.findCart( userId ).stream().map( item -> {
			var book = item.getBook();
			return new CartItemResponse( book.getId(),
										 book.getTitle(),
										 book.getAuthor(),
										 book.getPrice(),
										 item.getQuantity(),
										 book.getPrice().multiply( BigDecimal.valueOf( item.getQuantity() ) ) );
		} ).toList();
		BigDecimal total = lines.stream().map( CartItemResponse::lineTotal ).reduce( new BigDecimal( "0.00" ), BigDecimal::add );
		int quantity = lines.stream().mapToInt( CartItemResponse::quantity ).sum();
		return new CartResponse( lines, quantity, total, "EUR" );
	}
}
