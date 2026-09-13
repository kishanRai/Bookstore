package org.example.bookstore.dtos.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long bookId,
								String title,
								String author,
								BigDecimal unitPrice,
								int quantity,
								BigDecimal lineTotal
) {}