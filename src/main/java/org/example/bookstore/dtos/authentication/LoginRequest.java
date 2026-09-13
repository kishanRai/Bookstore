package org.example.bookstore.dtos.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated login credentials; its string representation redacts the submitted password.
 */
@Schema(description = "Credentials for session authentication.")
public record LoginRequest(
    @Schema(description = "Email normalized to lowercase.", example = "reader@example.com", format = "email", maxLength = 254)
    @NotBlank @Email @Size(max = 254)
    String email,

    @Schema(description = "Case-sensitive password; never trimmed or returned.", example = "BookstoreTest2026!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY, maxLength = 128)
    @NotBlank @Size(max = 128)
    String password
) {
    /**
     * Produces diagnostic text with the password redacted.
     *
     * @return a representation that never includes the submitted password
     */
    @Override
    public String toString() {
        return "LoginRequest[REDACTED]";
    }

}
