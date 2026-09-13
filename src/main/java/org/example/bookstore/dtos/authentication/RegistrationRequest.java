package org.example.bookstore.dtos.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New customer credentials. Registration does not start an authenticated session.")
public record RegistrationRequest(
    @Schema(description = "Email normalized to lowercase.", example = "reader@example.com", format = "email", maxLength = 254)
    @NotBlank(message = "Email must not be blank") @Email(message = "Email must be a valid email address") @Size(max = 254, message = "Email must not exceed 254 characters")
    String email,

    @Schema(description = "Case-sensitive password; never trimmed or returned.", example = "BookstoreTest2026!", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY, maxLength = 128, minLength = 12)
    @NotBlank(message = "Password must not be blank") @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters long")
    String password
) {
    @Override
    public String toString() {
        return "RegistrationRequest[REDACTED]";
    }

}
