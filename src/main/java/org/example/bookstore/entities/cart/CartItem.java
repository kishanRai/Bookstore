package org.example.bookstore.entities.cart;

import org.example.bookstore.entities.catalog.Book;
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

	public CartItem( Long userId, Book book, int quantity ) {
		this.userId = userId;
		this.book = book;
		changeQuantity( quantity );
	}

	public void changeQuantity( int quantity ) {
		if ( quantity < 1 || quantity > 99 ) {
			throw new IllegalArgumentException( "Quantity must be between 1 and 99" );
		}
		this.quantity = quantity;
	}
}