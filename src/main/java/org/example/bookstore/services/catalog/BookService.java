package org.example.bookstore.services.catalog;

import org.example.bookstore.dtos.catalog.BookPageResponse;
import org.example.bookstore.dtos.catalog.BookResponse;
import org.example.bookstore.repositories.catalog.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;

	@Transactional( readOnly = true )
	public BookPageResponse getBooks( int page, int size ) {
		PageRequest pageRequest = PageRequest.of( page, size, Sort.by( "id" ).ascending() );

		Page<BookResponse> bookPage = bookRepository.findAll( pageRequest )
			.map( book -> new BookResponse( book.getId(), book.getTitle(), book.getAuthor(), book.getPrice(), book.getCurrency() ) );

		return new BookPageResponse( bookPage.getContent(),
									 bookPage.getNumber(),
									 bookPage.getSize(),
									 bookPage.getTotalElements(),
									 bookPage.getTotalPages(),
									 bookPage.hasNext() );
	}

}
