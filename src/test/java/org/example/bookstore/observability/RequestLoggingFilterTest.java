package org.example.bookstore.observability;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Verifies response correlation and restoration of the request thread logging context.
 */
class RequestLoggingFilterTest {
    @Test void assignsCorrelationIdAndRestoresThreadContext() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/login");
        request.setServletPath("/api/v1/authentication/login");
        var response = new MockHttpServletResponse();
        ThreadContext.put("correlationId", "previous-context");
        try {
            new RequestLoggingFilter().doFilter(request, response, (req, res) -> {
                assertThat(ThreadContext.get("correlationId")).isNotEqualTo("previous-context");
                response.setStatus(401);
            });
            assertThat(UUID.fromString(response.getHeader("X-Correlation-ID"))).isNotNull();
            assertThat(ThreadContext.get("correlationId")).isEqualTo("previous-context");
        } finally { ThreadContext.remove("correlationId"); }
    }
}
