package org.example.bookstore.repositories.authentication;

import java.util.Optional;
import org.example.bookstore.entities.authentication.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

/**
 * Queries customer accounts and acquires customer-row locks for serialized cart operations.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	/**
	 * Looks up a customer by the normalized email stored during registration.
	 *
	 * @param email normalized customer email
	 * @return the matching account, if present
	 */
	Optional<AppUser> findByEmail( String email );

	/**
	 * Acquires a pessimistic write lock on the customer row within an active transaction.
	 *
	 * @param id persisted resource ID
	 * @return the locked customer, if present
	 */
	@Lock( LockModeType.PESSIMISTIC_WRITE )
	@Query( "select u from AppUser u where u.id = :id" )
	Optional<AppUser> findByIdForUpdate( @Param( "id" ) Long id );
}
