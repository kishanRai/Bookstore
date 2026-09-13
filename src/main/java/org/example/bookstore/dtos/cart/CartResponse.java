package org.example.bookstore.dtos.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Persisted cart; empty carts have no items and zero totals.")
public record CartResponse(
    @Schema(description = "Up to 100 distinct books ordered by book ID.")
    List<CartItemResponse> items,

    @Schema(description = "Total copies across all lines, not distinct books.", example = "2", minimum = "0", maximum = "9900")
    int totalQuantity,

    @Schema(description = "Sum of line totals calculated by the server.", example = "70.00", minimum = "0")
    BigDecimal total,

    @Schema(description = "Currency for the cart.", example = "EUR", allowableValues = {"EUR"})
    String currency
) {
}
