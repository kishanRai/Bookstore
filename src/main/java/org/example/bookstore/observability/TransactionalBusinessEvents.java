package org.example.bookstore.observability;

import lombok.RequiredArgsConstructor;
import org.example.bookstore.application.events.BusinessEvents;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Transaction adapter for the application event port; observers never see rolled-back successes. */
@Component
@RequiredArgsConstructor
public class TransactionalBusinessEvents implements BusinessEvents {
    private final ApplicationEventPublisher publisher;
    /**
     * Captures request correlation and registers delivery after the enclosing transaction commits.
     *
     * @param type successful business operation to observe
     * @param customerId customer ID resolved by a trusted caller
     * @param orderId saved order ID, or null for non-order operations
     */
    @Override
    public void afterCommit(Type type, long customerId, Long orderId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Business events require an active transaction");
        }
        var event = new BusinessEvent(type, customerId, orderId, ThreadContext.get("correlationId"));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * Publishes the captured event after the database commit succeeds.
             */
            @Override public void afterCommit() { publisher.publishEvent(event); }
        });
    }
}
