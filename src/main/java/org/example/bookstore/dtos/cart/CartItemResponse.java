package org.example.bookstore.dtos.cart;

import java.math.BigDecimal;

public record CartItemResponse(Long bookId, String title, String author,
							   BigDecimal unitPrice, int quantity, BigDecimal lineTotal
){}