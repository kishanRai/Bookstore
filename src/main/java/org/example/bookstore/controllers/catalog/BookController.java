package org.example.bookstore.controllers.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.example.bookstore.dtos.catalog.BookPageResponse;
import org.example.bookstore.services.catalog.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Catalog", description = "Public book catalog with bounded database pagination.")
@RestController
@RequestMapping( "/api/v1/books" )
@RequiredArgsConstructor
public class BookController {

	private final BookService bookService;

	@Operation(summary = "List books",
        description = "Sorted by ascending book ID. Defaults: page 0, size 20. Empty catalogs and valid pages beyond the last result return an empty content array with metadata. Page is bounded to 10000; size to 100.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Page of books", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookPageResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400")
        })
    @GetMapping
	public BookPageResponse getBooks(
        @Parameter(description = "Zero-based page; inclusive range 0 to 10000",
            schema = @Schema(type = "integer", format = "int32", defaultValue = "0", minimum = "0", maximum = "10000"))
        @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(10000) int page,
        @Parameter(description = "Page capacity; inclusive range 1 to 100",
            schema = @Schema(type = "integer", format = "int32", defaultValue = "20", minimum = "1", maximum = "100"))
        @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
		return bookService.getBooks( page, size );
	}
}
