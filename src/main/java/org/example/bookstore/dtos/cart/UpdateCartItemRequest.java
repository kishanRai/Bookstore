package org.example.bookstore.dtos.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.bookstore.domain.cart.CartLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Specifies the replacement quantity for an existing cart line.
 */
@Schema(description = "Replacement quantity for an existing cart item.")
public record UpdateCartItemRequest(
    @Schema(description = "Number of copies; use DELETE to remove an item.", example = "2", minimum = "1", maximum = "99")
    @NotNull @Min(CartLimits.MIN_QUANTITY) @Max(CartLimits.MAX_QUANTITY)
    Integer quantity
) {
}
