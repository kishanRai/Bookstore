package org.example.bookstore.dtos.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(description = "Copies to add; repeated POST requests increment the quantity again.")
public record AddCartItemRequest(
    @Schema(description = "Existing book ID from the catalog.", example = "1", minimum = "1")
    @NotNull @Positive
    Long bookId,

    @Schema(description = "Number of copies; use DELETE to remove an item.", example = "2", minimum = "1", maximum = "99")
    @NotNull @Min(1) @Max(99)
    Integer quantity
) {
}
