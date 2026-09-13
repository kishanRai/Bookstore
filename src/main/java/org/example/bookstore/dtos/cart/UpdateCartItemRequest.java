package org.example.bookstore.dtos.cart;


import jakarta.validation.constraints.*;

public record UpdateCartItemRequest(@NotNull @Min(1) @Max(99)
									Integer quantity
) {}