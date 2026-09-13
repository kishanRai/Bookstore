package org.example.bookstore.controllers.catalog;

import org.example.bookstore.dtos.catalog.BookPageResponse;
import org.example.bookstore.services.catalog.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping( "/api/v1/books" )
@RequiredArgsConstructor
public class BookController {

	private final BookService bookService;

	@GetMapping
	public BookPageResponse getBooks( @RequestParam( name = "page", defaultValue = "0" ) @Min( 0 ) @Max( 10000 ) int page,
									  @RequestParam( name = "size", defaultValue = "20" ) @Min( 1 ) @Max( 100 ) int size ) {
		return bookService.getBooks( page, size );
	}
}
