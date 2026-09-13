package org.example.bookstore.repositories.catalog;

import java.util.Optional;
import org.example.bookstore.entities.catalog.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

/**
 * Provides Spring Data persistence and database pagination for catalog books.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

	/**
	 * Acquires a pessimistic write lock on the book row within an active transaction, so
	 * concurrent checkouts reserving the same book serialize instead of overselling stock.
	 *
	 * @param id catalog book ID
	 * @return the locked book, if present
	 */
	@Lock( LockModeType.PESSIMISTIC_WRITE )
	@Query( "select b from Book b where b.id = :id" )
	Optional<Book> findByIdForUpdate( @Param( "id" ) Long id );
}
