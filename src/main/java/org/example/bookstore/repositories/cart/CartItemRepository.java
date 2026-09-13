package org.example.bookstore.repositories.cart;

import java.util.List;
import java.util.Optional;
import org.example.bookstore.entities.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	@Query( "select item from CartItem item join fetch item.book " + "where item.userId = :userId order by item.book.id" )
	List<CartItem> findCart( @Param( "userId" ) Long userId );

	Optional<CartItem> findByUserIdAndBook_Id( Long userId, Long bookId );

	long countByUserId( Long userId );

	@Modifying
	@Query( "delete from CartItem item where item.userId = :userId" )
	void deleteCart( @Param( "userId" ) Long userId );
}