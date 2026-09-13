package org.example.bookstore.dtos.authentication;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public customer details; passwords and hashes are excluded.")
public record UserResponse(
    @Schema(description = "Customer ID.", example = "1")
    Long id,

    @Schema(description = "Normalized registered email.", example = "reader@example.com", format = "email")
    String email
) {
}
