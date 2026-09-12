package org.example.bookstore.entities.catalog;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table( name = "books" )
@Getter
@NoArgsConstructor( access = lombok.AccessLevel.PROTECTED )
public class Book {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private Long id;

	@Column( nullable = false, length = 255 )
	private String title;

	@Column( nullable = false, length = 255 )
	private String author;

	@Column( nullable = false, precision = 12, scale = 2 )
	private BigDecimal price;

	@Column( nullable = false, length = 3 )
	private String currency;

}
