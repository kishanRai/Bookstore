package org.example.bookstore.dtos.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Schema(description = "Replacement quantity for an existing cart item.")
public record UpdateCartItemRequest(
    @Schema(description = "Number of copies; use DELETE to remove an item.", example = "2", minimum = "1", maximum = "99")
    @NotNull @Min(1) @Max(99)
    Integer quantity
) {
}
