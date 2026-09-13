package org.example.bookstore.dtos.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id,
							Instant createdAt,
							List<OrderItemResponse> items,
							BigDecimal total,
							String currency
) {}