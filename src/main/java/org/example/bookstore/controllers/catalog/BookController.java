package org.example.bookstore.controllers.catalog;

import java.util.List;
import org.example.bookstore.dtos.catalog.BookResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/api/v1/books" )
public class BookController {

	@GetMapping
	public List<BookResponse> getBooks() {
		return List.of(); // Return an empty list for now
	}
}
