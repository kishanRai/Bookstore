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

/**
 * Verifies ordered database pagination, page metadata and empty-catalog behavior.
 */
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
class BookCatalogIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long insertBook( String title, String author, String price ) {
		return jdbcTemplate.queryForObject( """
												INSERT INTO books (title, author, price, currency, stock_quantity) VALUES (?, ?, ?, 'EUR', 100) RETURNING id
												""", Long.class, title, author, new BigDecimal( price ) );
	}

	@Test
	void shouldReturnEmptyListWhenDatabaseHasNoBooks() throws Exception {
		mockMvc.perform(get("/api/v1/books"))
			.andExpect(status().isOk())
			.andExpect(content().json("""
                    {
                      "content": [],
                      "page": 0,
                      "size": 20,
                      "totalElements": 0,
                      "totalPages": 0,
                      "hasNext": false
                    }
                    """));
	}

	@Test
	void shouldReturnEmptyContentBeyondLastPage() throws Exception {
		insertBook("Only Book", "An Author", "10.00");

		mockMvc.perform(get("/api/v1/books")
							.param("page", "1")
							.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
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
			.andExpect( jsonPath( "$.content.length()" ).value( 2 ) )
			.andExpect( jsonPath( "$.content[0].id" ).value( firstId ) )
			.andExpect( jsonPath( "$.content[0].title" ).value( BOOK_FIRST_TITLE ) )
			.andExpect( jsonPath( "$.content[0].author" ).value( BOOK_AUTHOR ) )
			.andExpect( jsonPath( "$.content[0].price" ).value( 39.90 ) )
			.andExpect( jsonPath( "$.content[0].currency" ).value( BOOK_PAYCODE ) )
			.andExpect( jsonPath( "$.content[1].id" ).value( secondId ) )
			.andExpect( jsonPath( "$.content[1].title" ).value( BOOK_SECOND_TITLE ) )
			.andExpect( jsonPath( "$.content[1].author" ).value( BOOK_AUTHOR ) )
			.andExpect( jsonPath( "$.content[1].price" ).value( 45.50 ) )
			.andExpect( jsonPath( "$.content[1].currency" ).value( BOOK_PAYCODE ) );
	}


	@Test
	void shouldReturnRequestedPageWithMetadata() throws Exception {

		String BOOK_FIRST_TITLE = "Harry Potter and chamber of secrets";
		String BOOK_SECOND_TITLE = "Harry Potter and the prisoner of Azkaban";
		String BOOK_THIRD_TITLE = "Harry Potter and the goblet of fire";
		String BOOK_AUTHOR = "J.K. Rowling";

		insertBook(BOOK_FIRST_TITLE, BOOK_AUTHOR, "10.00");
		Long secondId = insertBook(BOOK_SECOND_TITLE, BOOK_AUTHOR, "20.00");
		insertBook(BOOK_THIRD_TITLE, BOOK_AUTHOR, "30.00");

		mockMvc.perform(get("/api/v1/books")
							.param("page", "1")
							.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id")
						   .value(secondId ))
			.andExpect(jsonPath("$.content[0].title").value(BOOK_SECOND_TITLE))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(1))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(3))
			.andExpect(jsonPath("$.hasNext").value(true));
	}

}
