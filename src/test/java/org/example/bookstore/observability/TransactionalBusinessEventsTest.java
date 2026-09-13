package org.example.bookstore.observability;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.example.bookstore.application.events.BusinessEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Verifies that committed business events are not published before commit or after rollback.
 */
class TransactionalBusinessEventsTest {
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final TransactionalBusinessEvents events = new TransactionalBusinessEvents(publisher);
    private void begin() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clear(); }

    @Test void observersSeeSuccessOnlyAfterCommit() {
        begin();
        events.afterCommit(BusinessEvents.Type.ORDER_CREATED, 7, 123L);
        verifyNoInteractions(publisher);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(publisher).publishEvent(new BusinessEvent(BusinessEvents.Type.ORDER_CREATED, 7, 123L, null));
    }
    @Test void rollbackDoesNotPublishSuccess() {
        begin();
        events.afterCommit(BusinessEvents.Type.ORDER_CREATED, 7, 123L);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(publisher);
    }
    @Test void missingTransactionIsRejected() {
        assertThatThrownBy(() -> events.afterCommit(BusinessEvents.Type.ORDER_CREATED, 7, 123L))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(publisher);
    }
}
