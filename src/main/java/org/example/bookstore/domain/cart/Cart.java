package org.example.bookstore.domain.cart;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.example.bookstore.domain.money.Money;
import org.example.bookstore.entities.cart.CartItem;
import org.example.bookstore.entities.catalog.Book;
import org.example.bookstore.exceptions.CartConflictException;
import org.example.bookstore.exceptions.InvalidCartQuantityException;
import org.example.bookstore.exceptions.ResourceNotFoundException;

/**
 * Customer cart aggregate. Owns merging, quantity/capacity rules, ordering and totals.
 * Persistence adapters restore it under a customer lock; this class has no Spring dependencies.
 * The aggregate uses the existing cart-item entities and does not require a new cart table.
 */
public final class Cart {
    private final long customerId;
    private final Map<Long, CartItem> items = new TreeMap<>();

    /**
     * Restores ordered membership while rejecting invalid quantities, duplicate books, mixed owners and excess capacity.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param storedItems persisted lines fetched for this customer
     */
    private Cart(long customerId, Collection<CartItem> storedItems) {
        if (customerId <= 0) throw new IllegalArgumentException("Customer ID must be positive");
        this.customerId = customerId;
        for (CartItem item : storedItems) {
            if (item.getUserId() != customerId) throw new IllegalArgumentException("Cart item belongs to another customer");
            requireQuantity(item.getQuantity());
            if (items.putIfAbsent(item.getBook().getId(), item) != null) {
                throw new IllegalArgumentException("Duplicate book in cart");
            }
        }
        if (items.size() > CartLimits.MAX_DISTINCT_BOOKS) throw new IllegalArgumentException("Stored cart exceeds capacity");
    }

    /**
     * Rehydrates an aggregate from stored lines after validating ownership, uniqueness and cart limits.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param items persisted lines fetched for this customer
     * @return the validated cart aggregate
     */
    public static Cart restore(long customerId, Collection<CartItem> items) {
        return new Cart(customerId, Objects.requireNonNull(items));
    }

    /**
     * Creates an empty cart for a valid customer identity.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @return a cart with no lines and a zero total
     */
    public static Cart empty(long customerId) { return restore(customerId, List.of()); }
    /**
     * Returns the customer whose membership and quantity rules this aggregate controls.
     *
     * @return the positive customer ID
     */
    public long customerId() { return customerId; }
    /**
     * Returns a membership snapshot ordered by book ID; contained entities still expose validated quantity changes.
     *
     * @return an unmodifiable list of current cart lines
     */
    public List<CartItem> items() { return List.copyOf(items.values()); }

    /**
     * Adds a new book or merges copies into an existing line without exceeding quantity or capacity limits.
     *
     * @param book existing catalog book
     * @param quantity requested copy count
     * @return the added or updated cart line
     * @throws org.example.bookstore.exceptions.InvalidCartQuantityException if the requested quantity is outside the allowed range
     * @throws org.example.bookstore.exceptions.CartConflictException if merging or adding would exceed a cart limit
     */
    public CartItem add(Book book, int quantity) {
        Objects.requireNonNull(book, "Book is required");
        Objects.requireNonNull(book.getId(), "Book must exist in the catalog");
        requireQuantity(quantity);
        CartItem existing = items.get(book.getId());
        if (existing != null) {
            int combined = existing.getQuantity() + quantity;
            if (combined > CartLimits.MAX_QUANTITY) throw new CartConflictException("A book cannot have more than " + CartLimits.MAX_QUANTITY + " copies in the cart");
            existing.changeQuantity(combined);
            return existing;
        }
        if (items.size() >= CartLimits.MAX_DISTINCT_BOOKS) throw new CartConflictException("Cart cannot contain more than " + CartLimits.MAX_DISTINCT_BOOKS + " distinct books");
        CartItem added = new CartItem(customerId, book, quantity);
        items.put(book.getId(), added);
        return added;
    }

    /**
     * Replaces a present line quantity after validating its bounds.
     *
     * @param bookId catalog book ID
     * @param quantity requested copy count
     * @return the updated cart line
     * @throws org.example.bookstore.exceptions.ResourceNotFoundException if the cart does not contain the book
     */
    public CartItem changeQuantity(long bookId, int quantity) {
        requireQuantity(quantity);
        CartItem item = Optional.ofNullable(items.get(bookId))
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.changeQuantity(quantity);
        return item;
    }

    /**
     * Removes the specified book from aggregate membership, leaving absent books unchanged.
     *
     * @param bookId catalog book ID
     * @return the removed line, or an empty optional when absent
     */
    public Optional<CartItem> remove(long bookId) { return Optional.ofNullable(items.remove(bookId)); }
    /**
     * Sums exact line totals using the currently associated catalog prices.
     *
     * @return the total in EUR, or exact zero for an empty cart
     */
    public Money total() { return items.values().stream().map(CartItem::lineTotal).reduce(Money.zero(), Money::add); }
    /**
     * Counts copies across all lines rather than the number of distinct books.
     *
     * @return the total number of copies
     */
    public int totalQuantity() { return items.values().stream().mapToInt(CartItem::getQuantity).sum(); }

    /** Returns the ordered source lines for an order factory; the factory must copy their values. */
    public List<CartItem> checkoutItems() {
        if (items.isEmpty()) throw new CartConflictException("Cannot check out an empty cart");
        return items();
    }

    /**
     * Enforces the domain quantity bounds for HTTP-independent callers.
     *
     * @param quantity requested copy count
     * @throws org.example.bookstore.exceptions.InvalidCartQuantityException if quantity is outside 1–99
     */
    public static void requireQuantity(int quantity) {
        if (quantity < CartLimits.MIN_QUANTITY || quantity > CartLimits.MAX_QUANTITY) {
            throw new InvalidCartQuantityException("Quantity must be between " + CartLimits.MIN_QUANTITY + " and " + CartLimits.MAX_QUANTITY);
        }
    }
}
