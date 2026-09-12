package org.example.bookstore.controllers.catalog;

import java.util.List;
import org.example.bookstore.dtos.catalog.BookResponse;
import org.example.bookstore.services.catalog.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping( "/api/v1/books" )
@RequiredArgsConstructor
public class BookController {

	private final BookService bookService;

	@GetMapping
	public List<BookResponse> getBooks() {
		return bookService.getBooks(); // Return an empty list for now
	}
}
