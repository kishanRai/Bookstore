package org.example.bookstore.entities.cart;

import org.example.bookstore.entities.catalog.Book;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.domain.money.CurrencyCode;
import org.example.bookstore.domain.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persisted customer/book association with a validated quantity and an exact current-price line total.
 */
@Entity
@Table( name = "cart_items" )
@Getter
@NoArgsConstructor( access = AccessLevel.PROTECTED )
public class CartItem {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private Long id;

	@Column( name = "user_id", nullable = false )
	private Long userId;

	@ManyToOne( fetch = FetchType.LAZY, optional = false )
	@JoinColumn( name = "book_id", nullable = false )
	private Book book;

	@Column( nullable = false )
	private int quantity;

	/**
	 * Associates an existing book with a customer and validates the initial quantity.
	 *
	 * @param userId owning customer ID
	 * @param book existing catalog book
	 * @param quantity requested copy count
	 */
	public CartItem( Long userId, Book book, int quantity ) {
		this.userId = userId;
		this.book = book;
		changeQuantity( quantity );
	}

	/**
	 * Replaces the quantity only after applying the shared domain bounds.
	 *
	 * @param quantity requested copy count
	 */
	public void changeQuantity( int quantity ) {
		Cart.requireQuantity(quantity);
		this.quantity = quantity;
	}
    /**
     * Calculates the exact quantity-times-current-price amount.
     *
     * @return the current line total in the book's supported currency
     */
    public Money lineTotal() {
        return new Money(book.getPrice(), CurrencyCode.valueOf(book.getCurrency())).multiply(quantity);
    }
}
