package org.example.bookstore.repositories.authentication;

import org.example.bookstore.entities.authentication.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

}
