package org.example.bookstore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)

@Sql(
	statements = "DELETE FROM books",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	statements = "DELETE FROM books",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
public class BookCatalogIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long insertBook( String title, String author, String price ) {
		return jdbcTemplate.queryForObject( """
												INSERT INTO books (title, author, price, currency) VALUES (?, ?, ?, 'EUR') RETURNING id
												""", Long.class, title, author, new BigDecimal( price ) );
	}

	@Test
	void shouldReturnEmptyListWhenDatabaseHasNoBooks() throws Exception {
		mockMvc.perform( get( "/api/v1/books" ) ).andExpect( status().isOk() ).andExpect( content().json( "[]" ) );
	}

	@Test
	void shouldReturnBooksFromDatabaseInIdOrder() throws Exception {
		String BOOK_FIRST_TITLE = "Harry Potter and the Philosopher's Stone";
		String BOOK_SECOND_TITLE = "Harry Potter and the Chamber of Secrets";
		String BOOK_AUTHOR = "J.K. Rowling";
		String BOOK_PAYCODE = "EUR";

		Long firstId = insertBook( BOOK_FIRST_TITLE, BOOK_AUTHOR, "39.90" );

		Long secondId = insertBook( BOOK_SECOND_TITLE, BOOK_AUTHOR, "45.50" );

		mockMvc.perform( get( "/api/v1/books" ).accept( MediaType.APPLICATION_JSON ) )
			.andExpect( status().isOk() )
			.andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
			.andExpect( jsonPath( "$.length()" ).value( 2 ) )
			.andExpect( jsonPath( "$[0].id" ).value( firstId ) )
			.andExpect( jsonPath( "$[0].title" ).value( BOOK_FIRST_TITLE ) )
			.andExpect( jsonPath( "$[0].author" ).value( BOOK_AUTHOR ) )
			.andExpect( jsonPath( "$[0].price" ).value( 39.90 ) )
			.andExpect( jsonPath( "$[0].currency" ).value( BOOK_PAYCODE ) )
			.andExpect( jsonPath( "$[1].id" ).value( secondId ) )
			.andExpect( jsonPath( "$[1].title" ).value( BOOK_SECOND_TITLE ) )
			.andExpect( jsonPath( "$[1].author" ).value( BOOK_AUTHOR ) )
			.andExpect( jsonPath( "$[1].price" ).value( 45.50 ) )
			.andExpect( jsonPath( "$[1].currency" ).value( BOOK_PAYCODE ) );
	}

}
