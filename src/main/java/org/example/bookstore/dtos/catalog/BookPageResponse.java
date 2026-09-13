package org.example.bookstore.dtos.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * A bounded catalog page with content and catalog-wide pagination metadata.
 */
@Schema(description = "Database page sorted by ascending book ID; content may be empty.")
public record BookPageResponse(
    @Schema(description = "Books on this page, up to size entries.")
    List<BookResponse> content,

    @Schema(description = "Requested zero-based page.", example = "0", minimum = "0", maximum = "10000")
    int page,

    @Schema(description = "Requested page capacity, not the number of returned books.", example = "20", minimum = "1", maximum = "100")
    int size,

    @Schema(description = "Total catalog books.", example = "1", minimum = "0")
    long totalElements,

    @Schema(description = "Pages at the requested size; zero for an empty catalog.", example = "1", minimum = "0")
    int totalPages,

    @Schema(description = "More data exists; this does not override the page cap.", example = "false")
    boolean hasNext
) {
}
