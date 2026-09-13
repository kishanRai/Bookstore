package org.example.bookstore.observability;

import org.example.bookstore.application.events.BusinessEvents;

/** Contains identifiers only; credentials, tokens and customer email are deliberately excluded. */
public record BusinessEvent(BusinessEvents.Type type, long customerId, Long orderId, String correlationId) { }
