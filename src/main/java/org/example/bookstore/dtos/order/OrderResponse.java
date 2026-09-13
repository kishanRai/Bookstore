package org.example.bookstore.dtos.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

@Schema(description = "Saved order summary. Catalog changes do not rewrite these details.")
public record OrderResponse(
    @Schema(description = "Order ID; visible only to its owner.", example = "1")
    Long id,

    @Schema(description = "Checkout timestamp in UTC.", example = "2026-09-13T12:00:00Z", format = "date-time")
    Instant createdAt,

    @Schema(description = "Saved lines ordered by book ID.")
    List<OrderItemResponse> items,

    @Schema(description = "Saved sum of line totals; excludes payment, shipping and tax processing.", example = "70.00", minimum = "0")
    BigDecimal total,

    @Schema(description = "Currency of the order.", example = "EUR", allowableValues = {"EUR"})
    String currency
) {
}
