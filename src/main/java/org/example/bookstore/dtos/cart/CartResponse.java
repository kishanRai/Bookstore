package org.example.bookstore.dtos.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, int totalQuantity,
						   BigDecimal total, String currency
) {}