package org.example.bookstore.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.example.bookstore.domain.cart.Cart;
import org.example.bookstore.entities.cart.CartItem;
import org.example.bookstore.entities.catalog.Book;
import org.example.bookstore.exceptions.CartConflictException;
import org.example.bookstore.exceptions.InvalidCartQuantityException;
import org.example.bookstore.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises cart invariants and exact totals without Spring or database infrastructure.
 */
class CartTest {
    static Book book(long id, String price) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getTitle()).thenReturn("Book " + id);
        when(book.getAuthor()).thenReturn("Author");
        when(book.getPrice()).thenReturn(new BigDecimal(price));
        when(book.getCurrency()).thenReturn("EUR");
        return book;
    }

    @Test void emptyCartHasExactZeroAndCannotCheckOut() {
        var cart = Cart.empty(7);
        assertThat(cart.total().amount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(cart.totalQuantity()).isZero();
        assertThatThrownBy(cart::checkoutItems).isInstanceOf(CartConflictException.class)
            .hasMessage("Cannot check out an empty cart");
    }

    @Test void mergesCopiesSortsBooksAndCalculatesTotals() {
        var cart = Cart.empty(7);
        cart.add(book(2, "7.99"), 2);
        var first = book(1, "12.50");
        cart.add(first, 1);
        cart.add(first, 2);
        assertThat(cart.items()).extracting(i -> i.getBook().getId()).containsExactly(1L, 2L);
        assertThat(cart.totalQuantity()).isEqualTo(5);
        assertThat(cart.total().amount()).isEqualByComparingTo("53.48");
    }

    @Test void replacementAndRemovalPreserveTotals() {
        var cart = Cart.empty(7);
        cart.add(book(1, "0.10"), 3);
        cart.changeQuantity(1, 2);
        assertThat(cart.total().amount()).isEqualByComparingTo("0.20");
        assertThat(cart.remove(1)).isPresent();
        assertThat(cart.remove(1)).isEmpty();
        assertThat(cart.totalQuantity()).isZero();
        assertThat(cart.total().amount()).isZero();
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0, 100, Integer.MAX_VALUE})
    void rejectsInvalidAddAndReplacementWithoutChangingCart(int quantity) {
        var cart = Cart.empty(7);
        var book = book(1, "12.50");
        cart.add(book, 2);
        assertThatThrownBy(() -> cart.add(book, quantity)).isInstanceOf(InvalidCartQuantityException.class);
        assertThatThrownBy(() -> cart.changeQuantity(1, quantity)).isInstanceOf(InvalidCartQuantityException.class);
        assertThat(cart.totalQuantity()).isEqualTo(2);
    }

    @Test void combinedQuantityCannotExceed99() {
        var cart = Cart.empty(7);
        var book = book(1, "1.00");
        cart.add(book, 98);
        cart.add(book, 1);
        assertThatThrownBy(() -> cart.add(book, 1)).isInstanceOf(CartConflictException.class);
        assertThat(cart.totalQuantity()).isEqualTo(99);
    }

    @Test void fullCartAllowsExistingBooksButRejectsThe101stDistinctBook() {
        var cart = Cart.empty(7);
        for (int id = 1; id <= 100; id++) cart.add(book(id, "1.00"), 1);
        cart.add(book(1, "1.00"), 1);
        assertThatThrownBy(() -> cart.add(book(101, "1.00"), 1)).isInstanceOf(CartConflictException.class);
        assertThat(cart.items()).hasSize(100);
        assertThat(cart.totalQuantity()).isEqualTo(101);
    }

    @Test void missingReplacementIsNotFound() {
        assertThatThrownBy(() -> Cart.empty(7).changeQuantity(1, 2)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void rehydrationRejectsMixedOwnersAndDuplicateBooks() {
        var book = book(1, "1.00");
        var item = new CartItem(7L, book, 1);
        assertThatThrownBy(() -> Cart.restore(8, List.of(item))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Cart.restore(7, List.of(item, item))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void cartMembershipCannotBeChangedThroughExternalLists() {
        var source = new ArrayList<>(List.of(new CartItem(7L, book(1, "1.00"), 1)));
        var cart = Cart.restore(7, source);
        source.clear();
        assertThat(cart.items()).hasSize(1);
        assertThatThrownBy(() -> cart.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
