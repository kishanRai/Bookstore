package org.example.bookstore.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.example.bookstore.entities.catalog.Book;

/**
 * Test Data Builder assembling mocked catalog books with sensible defaults. Centralizes the
 * Mockito stubbing that domain and service tests otherwise repeat for every book fixture.
 */
public final class BookTestDataBuilder {
    private long id = 1L;
    private String title = "Book 1";
    private String author = "Author";
    private BigDecimal price = new BigDecimal("12.50");
    private String currency = "EUR";
    private int stockQuantity = 100;

    private BookTestDataBuilder() { }

    /**
     * Starts a new builder with defaults sufficient for tests that do not care about a specific field.
     *
     * @return a builder for one mocked book
     */
    public static BookTestDataBuilder aBook() { return new BookTestDataBuilder(); }

    /**
     * Sets the book ID and derives a matching default title ("Book " + id), overridable via {@link #withTitle}.
     *
     * @param id catalog book ID
     * @return this builder
     */
    public BookTestDataBuilder withId(long id) { this.id = id; this.title = "Book " + id; return this; }

    /**
     * Overrides the default derived title.
     *
     * @param title book title
     * @return this builder
     */
    public BookTestDataBuilder withTitle(String title) { this.title = title; return this; }

    /**
     * Sets the unit price parsed from a plain decimal string.
     *
     * @param price exact unit price, e.g. "12.50"
     * @return this builder
     */
    public BookTestDataBuilder withPrice(String price) { this.price = new BigDecimal(price); return this; }

    /**
     * Sets the copies available before any reservation.
     *
     * @param stockQuantity copies currently in stock
     * @return this builder
     */
    public BookTestDataBuilder withStock(int stockQuantity) { this.stockQuantity = stockQuantity; return this; }

    /**
     * Builds a mocked book stubbed with every configured field.
     *
     * @return the mocked book
     */
    public Book build() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getTitle()).thenReturn(title);
        when(book.getAuthor()).thenReturn(author);
        when(book.getPrice()).thenReturn(price);
        when(book.getCurrency()).thenReturn(currency);
        when(book.getStockQuantity()).thenReturn(stockQuantity);
        return book;
    }
}
