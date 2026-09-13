package org.example.bookstore.dtos.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Public catalog description and exact EUR price for one available book.
 */
@Schema(description = "Available book with its current catalog price.")
public record BookResponse(
    @Schema(description = "Book ID.", example = "1")
    long id,

    @Schema(description = "Book title.", example = "Clean Code")
    String title,

    @Schema(description = "Book author.", example = "Robert C. Martin")
    String author,

    @Schema(description = "Current unit price in EUR.", example = "35.00", minimum = "0")
    BigDecimal price,

    @Schema(description = "Currency used for all prices.", example = "EUR", allowableValues = {"EUR"})
    String currency
) {
}
