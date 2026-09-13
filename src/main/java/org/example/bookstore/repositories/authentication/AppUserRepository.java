package org.example.bookstore.repositories.authentication;

import java.util.Optional;
import org.example.bookstore.entities.authentication.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByEmail( String email );

	@Lock( LockModeType.PESSIMISTIC_WRITE )
	@Query( "select u from AppUser u where u.email = :email" )
	Optional<AppUser> findByEmailForUpdate( @Param( "email" ) String email );
}
