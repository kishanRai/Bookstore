package org.example.bookstore.observability;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Correlates request completion and committed business events without logging bodies, headers or query strings. */
@Log4j2
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    /**
     * Adds a response correlation ID and logs HTTP outcome and elapsed time with scoped Log4j2 context.
     * The previous thread context is restored even when the downstream request fails.
     *
     * @param request incoming HTTP request
     * @param response response receiving the correlation header
     * @param chain remaining security filters and request handler
     * @throws ServletException if a downstream servlet cannot handle the request
     * @throws IOException if downstream request or response I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        response.setHeader("X-Correlation-ID", correlationId);
        try (var correlation = CloseableThreadContext.put("correlationId", correlationId)) {
            long start = System.nanoTime();
            boolean failed = false;
            try {
                chain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException ex) {
                failed = true;
                throw ex;
            } finally {
                int status = failed ? 500 : response.getStatus();
                String operation = request.getRequestURI();
                String event = operation.endsWith("/login") && status == 401 ? "AUTHENTICATION_FAILED" : "HTTP_REQUEST";
                try (var fields = CloseableThreadContext.put("event", event).put("method", request.getMethod())
                        .put("path", operation).put("status", Integer.toString(status))
                        .put("durationMs", Long.toString((System.nanoTime() - start) / 1_000_000))) {
                    log.info("Bookstore request completed");
                }
            }
        }
    }
}
