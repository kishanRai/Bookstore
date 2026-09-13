package org.example.bookstore.entities.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.domain.money.CurrencyCode;
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

/**
 * Order aggregate preserving checkout metadata, exact totals and defensively exposed historical lines.
 */
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

    /** Validated constructor retained for explicit domain construction. Prefer fromCart for checkout. */
    public PurchaseOrder(Long userId, UUID idempotencyKey, Instant createdAt, List<CartItem> cartItems) {
        cartItems = Cart.restore(Objects.requireNonNull(userId), cartItems).checkoutItems();
        Objects.requireNonNull(idempotencyKey, "Idempotency key is required");
        Objects.requireNonNull(createdAt, "Creation time is required");
		this.userId = userId;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = createdAt;
		this.currency = CurrencyCode.EUR.name();
		this.total = new BigDecimal( "0.00" );
		for ( CartItem cartItem : cartItems ) {
			OrderItem item = new OrderItem( this, cartItem );
			items.add( item );
			total = total.add( item.lineTotal() );
		}
	}
    /** Named factory: validates checkout and copies every line into an independent historical snapshot. */
    public static PurchaseOrder fromCart(Cart cart, UUID key, Instant createdAt) {
        return new PurchaseOrder(cart.customerId(), key, createdAt, cart.checkoutItems());
    }

    /** JPA accesses the backing field; callers cannot add or delete historical lines. */
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
