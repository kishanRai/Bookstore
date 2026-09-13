package org.example.bookstore.dtos.authentication;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Carries a session-bound CSRF token and the header name required for subsequent mutations.
 */
@Schema(description = "CSRF token associated with the session cookie.")
public record CsrfResponse(
    @Schema(description = "Header to send with mutations.", example = "X-CSRF-TOKEN")
    String headerName,

    @Schema(description = "Runtime token. Obtain a fresh value after login/logout.")
    String token
) {
}
