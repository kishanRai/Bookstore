package org.example.bookstore.services.cart;

import lombok.RequiredArgsConstructor;
import org.example.bookstore.application.CustomerLock;
import org.example.bookstore.application.cart.CartUseCase;
import org.example.bookstore.application.events.BusinessEvents;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.example.bookstore.repositories.cart.CartItemRepository;
import org.example.bookstore.repositories.catalog.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates persistence and locking; Cart owns every cart business rule and monetary calculation. */
@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CartService implements CartUseCase {
    private final CartItemRepository items;
    private final BookRepository books;
    private final CustomerLock customerLock;
    private final BusinessEvents events;

    /**
     * Loads the cart under the same customer lock used by mutations and checkout.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @return the current customer cart
     */
    @Override
    public Cart get(long customerId) { return load(customerId); }

    /**
     * Locks and loads the cart, applies the domain addition and persists the changed line.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param bookId catalog book ID
     * @param quantity requested copy count
     * @return the updated aggregate
     */
    @Override
    public Cart add(long customerId, long bookId, int quantity) {
        Cart cart = load(customerId);
        var book = books.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        items.save(cart.add(book, quantity));
        events.afterCommit(BusinessEvents.Type.CART_ITEM_ADDED, customerId, null);
        return cart;
    }

    /**
     * Applies a domain quantity replacement to a managed line inside the locked transaction.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param bookId catalog book ID
     * @param quantity requested copy count
     * @return the updated aggregate
     */
    @Override
    public Cart changeQuantity(long customerId, long bookId, int quantity) {
        Cart cart = load(customerId);
        cart.changeQuantity(bookId, quantity); // Managed entity: Hibernate persists the domain change.
        events.afterCommit(BusinessEvents.Type.CART_QUANTITY_CHANGED, customerId, null);
        return cart;
    }

    /**
     * Deletes an existing domain line and schedules a committed event; missing lines are ignored.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @param bookId catalog book ID
     */
    @Override
    public void remove(long customerId, long bookId) {
        Cart cart = load(customerId);
        cart.remove(bookId).ifPresent(item -> {
            items.delete(item);
            events.afterCommit(BusinessEvents.Type.CART_ITEM_REMOVED, customerId, null);
        });
    }

    /**
     * Locks the customer before fetching and validating cart membership.
     *
     * @param customerId customer ID resolved by a trusted caller
     * @return the restored aggregate within the active transaction
     */
    private Cart load(long customerId) {
        // Reads also serialize with checkout. Different customers lock different database rows.
        customerLock.acquire(customerId);
        return Cart.restore(customerId, items.findCart(customerId));
    }
}
