package org.example.bookstore.repositories.catalog;

import org.example.bookstore.entities.catalog.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

}
