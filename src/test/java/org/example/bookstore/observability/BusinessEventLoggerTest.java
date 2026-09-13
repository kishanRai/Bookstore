package org.example.bookstore.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.example.bookstore.application.events.BusinessEvents;
import org.junit.jupiter.api.Test;

/** Checks the actual Log4j2 backend and event context isolation without starting Spring. */
class BusinessEventLoggerTest {
    /** Captures an immutable Log4j2 event before the scoped context is restored. */
    private static class RecordingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        /** Creates an in-memory appender for one test invocation. */
        RecordingAppender() { super("business-event-test", null, null, false, Property.EMPTY_ARRAY); }

        /** @param event log event whose context must survive subsequent thread cleanup */
        @Override public void append(LogEvent event) { events.add(event.toImmutable()); }
    }

    /** Verifies event correlation replaces the ambient value only while logging the committed operation. */
    @Test
    void logsCapturedCorrelationAndRestoresObserverThreadContext() {
        Logger logger = (Logger) LogManager.getLogger(BusinessEventLogger.class);
        Level previousLevel = logger.getLevel();
        var appender = new RecordingAppender();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        ThreadContext.put("correlationId", "observer-context");
        try {
            new BusinessEventLogger().log(new BusinessEvent(BusinessEvents.Type.ORDER_CREATED, 7, 12L, "request-context"));
            assertThat(appender.events).hasSize(1);
            var logged = appender.events.get(0);
            assertThat(logged.getContextData().toMap()).containsEntry("correlationId", "request-context")
                .containsEntry("event", "ORDER_CREATED").containsEntry("customerId", "7")
                .containsEntry("orderId", "12").containsEntry("outcome", "committed");
            assertThat(logged.getMessage().getFormattedMessage()).isEqualTo("Bookstore business operation completed");
            assertThat(ThreadContext.getImmutableContext()).containsOnlyKeys("correlationId");
            assertThat(ThreadContext.get("correlationId")).isEqualTo("observer-context");
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
            ThreadContext.clearMap();
        }
    }
}
