package org.example.bookstore.observability;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Writes committed business events through Log4j2, independently of domain rules and persistence. */
@Log4j2
@Component
public class BusinessEventLogger {
    /**
     * Logs the captured request correlation and business identifiers, restoring the observer thread's context.
     * Context fields are emitted once by Spring Boot's ECS formatter; credentials are never included.
     *
     * @param event committed operation with an optional order ID and request correlation
     */
    @EventListener
    public void log(BusinessEvent event) {
        String previous = ThreadContext.get("correlationId");
        try (var context = CloseableThreadContext.put("event", event.type().name())
                .put("customerId", Long.toString(event.customerId())).put("outcome", "committed")) {
            if (event.correlationId() == null) ThreadContext.remove("correlationId");
            else ThreadContext.put("correlationId", event.correlationId());
            if (event.orderId() != null) context.put("orderId", event.orderId().toString());
            log.info("Bookstore business operation completed");
        } finally {
            if (previous == null) ThreadContext.remove("correlationId");
            else ThreadContext.put("correlationId", previous);
        }
    }
}
