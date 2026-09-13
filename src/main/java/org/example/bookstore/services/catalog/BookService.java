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

/**
 * Reads ordered catalog pages in a read-only transaction and maps them to the public API contract.
 */
@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;

    /**
     * Queries one database page ordered by ID and maps entities to public DTOs.
     * Bounds are validated at the HTTP boundary (page 0..10000, size 1..100).
     * Offset pagination and total counts can still become expensive at large offsets;
     * separate page requests do not share a catalog snapshot.
     *
     * @param page zero-based page number
     * @param size requested page capacity
     * @return content and catalog-wide pagination metadata, including empty pages
     */
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
