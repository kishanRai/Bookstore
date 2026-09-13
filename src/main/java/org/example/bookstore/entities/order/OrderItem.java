package org.example.bookstore.entities.order;

import java.math.BigDecimal;
import org.example.bookstore.entities.cart.CartItem;
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
@Table( name = "order_items" )
@Getter
@NoArgsConstructor( access = AccessLevel.PROTECTED )
public class OrderItem {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private Long id;

	@ManyToOne( fetch = FetchType.LAZY, optional = false )
	@JoinColumn( name = "order_id", nullable = false )
	private PurchaseOrder order;

	@Column( name = "book_id", nullable = false )
	private Long bookId;

	@Column( nullable = false, length = 255 )
	private String title;

	@Column( nullable = false, length = 255 )
	private String author;

	@Column( name = "unit_price", nullable = false, precision = 12, scale = 2 )
	private BigDecimal unitPrice;

	@Column( nullable = false )
	private int quantity;

	public OrderItem( PurchaseOrder order, CartItem item ) {
		this.order = order;
		this.bookId = item.getBook().getId();
		this.title = item.getBook().getTitle();
		this.author = item.getBook().getAuthor();
		this.unitPrice = item.getBook().getPrice();
		this.quantity = item.getQuantity();
	}

	public BigDecimal lineTotal() {
		return unitPrice.multiply( BigDecimal.valueOf( quantity ) );
	}
}