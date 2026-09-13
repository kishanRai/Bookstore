package org.example.bookstore.entities.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.bookstore.entities.cart.CartItem;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table( name = "customer_orders" )
@Getter
@NoArgsConstructor( access = AccessLevel.PROTECTED )
public class PurchaseOrder {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private Long id;

	@Column( name = "user_id", nullable = false )
	private Long userId;

	@Column( name = "idempotency_key", nullable = false )
	private UUID idempotencyKey;

	@Column( name = "created_at", nullable = false )
	private Instant createdAt;

	@Column( nullable = false, precision = 18, scale = 2 )
	private BigDecimal total;

	@Column( nullable = false, length = 3 )
	private String currency;


	@OneToMany( mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true )
	@OrderBy( "bookId ASC" )
	private List<OrderItem> items = new ArrayList<>();

	public PurchaseOrder( Long userId, UUID idempotencyKey, Instant createdAt, List<CartItem> cartItems ) {
		if ( cartItems.isEmpty() )
			throw new IllegalArgumentException( "Order must have items" );
		this.userId = userId;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = createdAt;
		this.currency = "EUR";
		this.total = new BigDecimal( "0.00" );
		for ( CartItem cartItem : cartItems ) {
			OrderItem item = new OrderItem( this, cartItem );
			items.add( item );
			total = total.add( item.lineTotal() );
		}
	}
}