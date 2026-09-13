package org.example.bookstore.repositories.cart;

import java.util.List;
import java.util.Optional;
import org.example.bookstore.entities.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Loads cart lines with their books and performs customer-scoped cart queries and deletion.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	/**
	 * Fetches customer cart lines together with books, ordered by book ID to avoid lazy-loading queries.
	 *
	 * @param userId owning customer ID
	 * @return the ordered lines, or an empty list
	 */
	@Query( "select item from CartItem item join fetch item.book " + "where item.userId = :userId order by item.book.id" )
	List<CartItem> findCart( @Param( "userId" ) Long userId );

	/**
	 * Finds one book line within a customer's cart.
	 *
	 * @param userId owning customer ID
	 * @param bookId catalog book ID
	 * @return the matching line, if present
	 */
	Optional<CartItem> findByUserIdAndBook_Id( Long userId, Long bookId );

	/**
	 * Counts distinct persisted book lines for the customer.
	 *
	 * @param userId owning customer ID
	 * @return the number of cart lines
	 */
	long countByUserId( Long userId );

	/**
	 * Deletes all customer cart lines in the caller's transaction during checkout.
	 *
	 * @param userId owning customer ID
	 */
	@Modifying
	@Query( "delete from CartItem item where item.userId = :userId" )
	void deleteCart( @Param( "userId" ) Long userId );
}