package org.example.bookstore.entities.catalog;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.bookstore.exceptions.InsufficientStockException;

/**
 * Persisted catalog entry; Flyway constraints enforce nonblank details, nonnegative prices and EUR currency.
 */
@Entity
@Table( name = "books" )
@Getter
@NoArgsConstructor( access = lombok.AccessLevel.PROTECTED )
public class Book {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private Long id;

	@Column( nullable = false, length = 255 )
	private String title;

	@Column( nullable = false, length = 255 )
	private String author;

	@Column( nullable = false, precision = 12, scale = 2 )
	private BigDecimal price;

	@Column( nullable = false, length = 3 )
	private String currency;

	@Column( name = "stock_quantity", nullable = false )
	private int stockQuantity;

	/**
	 * Populates an in-memory book for unit tests that exercise stock invariants without JPA.
	 * Production code never constructs a book directly; catalog rows are owned by persistence.
	 *
	 * @param id catalog book ID
	 * @param title book title
	 * @param author book author
	 * @param price unit price
	 * @param currency supported currency code
	 * @param stockQuantity copies currently available
	 */
	Book( Long id, String title, String author, BigDecimal price, String currency, int stockQuantity ) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.price = price;
		this.currency = currency;
		this.stockQuantity = stockQuantity;
	}

	/**
	 * Decrements available stock by the requested quantity, rejecting reservations stock cannot cover.
	 * The book row must already be locked by the caller so concurrent checkouts cannot oversell it.
	 *
	 * @param quantity copies to reserve for one checkout line
	 * @throws InsufficientStockException if fewer copies remain than requested
	 */
	public void reserveStock( int quantity ) {
		if ( quantity <= 0 ) throw new IllegalArgumentException( "Quantity to reserve must be positive" );
		if ( stockQuantity < quantity ) throw new InsufficientStockException( title, stockQuantity, quantity );
		stockQuantity -= quantity;
	}
}
