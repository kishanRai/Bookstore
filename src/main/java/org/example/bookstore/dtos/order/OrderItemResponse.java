package org.example.bookstore.dtos.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Historical book details, purchased quantity and line total captured at checkout.
 */
@Schema(description = "Order line containing book details and price saved at checkout.")
public record OrderItemResponse(
    @Schema(description = "Historical book ID.", example = "1")
    Long bookId,

    @Schema(description = "Book title.", example = "Clean Code")
    String title,

    @Schema(description = "Book author.", example = "Robert C. Martin")
    String author,

    @Schema(description = "Price per copy in EUR.", example = "35.00", minimum = "0")
    BigDecimal unitPrice,

    @Schema(description = "Number of copies.", example = "2", minimum = "1", maximum = "99")
    int quantity,

    @Schema(description = "Unit price multiplied by quantity.", example = "70.00", minimum = "0")
    BigDecimal lineTotal
) {
}
