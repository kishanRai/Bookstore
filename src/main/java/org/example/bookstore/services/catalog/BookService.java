package org.example.bookstore.services.catalog;

import java.util.List;
import org.example.bookstore.dtos.catalog.BookResponse;
import org.example.bookstore.repositories.catalog.BookRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;

	@Transactional(readOnly = true)
	public List<BookResponse> getBooks() {
		return bookRepository.findAll( Sort.by( "id" ).ascending() )
			.stream()
			.map( book -> new BookResponse( book.getId(), book.getTitle(), book.getAuthor(), book.getPrice(), book.getCurrency() ) )
			.toList();
	}

}
