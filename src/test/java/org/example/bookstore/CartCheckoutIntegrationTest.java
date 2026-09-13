package org.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies cart rules, ownership, checkout snapshots, concurrency and rollback through the HTTP layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import( PostgresTestConfiguration.class )
@Sql( statements = { "DELETE FROM order_items", "DELETE FROM customer_orders", "DELETE FROM cart_items", "DELETE FROM books",
					 "DELETE FROM app_users" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD )
@Sql( statements = { "DELETE FROM order_items", "DELETE FROM customer_orders", "DELETE FROM cart_items", "DELETE FROM books",
					 "DELETE FROM app_users" }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD )
class CartCheckoutIntegrationTest {

	private static final String KISHAN = "kishan@example.com";
	private static final String RAI = "rai@example.com";


	@Autowired
	private MockMvc mvc;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ObjectMapper json;
	private Long aliceId;
	private Long firstBook;
	private Long secondBook;

	@BeforeEach
	void fixtures() {
		// These tests provide authenticated principals through Spring Security Test.
		// Real password/session authentication is covered by AuthenticationIntegrationTest.
		aliceId = jdbc.queryForObject( "INSERT INTO app_users(email,password_hash) VALUES (?, 'unused-in-cart-tests') RETURNING id",
									   Long.class, KISHAN );
		jdbc.update( "INSERT INTO app_users(email,password_hash) VALUES (?, 'unused-in-cart-tests')", RAI );
		firstBook = book( "First book", "12.50" );
		secondBook = book( "Second book", "7.99" );
	}

	private Long book( String title, String price ) {
		return jdbc.queryForObject( """
										INSERT INTO books(title,author,price,currency) VALUES (?, 'Author', ?, 'EUR') RETURNING id
										""", Long.class, title, new BigDecimal( price ) );
	}

	private ResultActions cart( String email ) throws Exception {
		return mvc.perform( get( "/api/v1/cart" ).with( user( email ) ) );
	}

	private ResultActions add( String email, Long bookId, int quantity ) throws Exception {
		return mvc.perform( post( "/api/v1/cart/items" ).with( user( email ) )
								.with( csrf() )
								.contentType( MediaType.APPLICATION_JSON )
								.content( json.writeValueAsString( Map.of( "bookId", bookId, "quantity", quantity ) ) ) );
	}

	private ResultActions quantity( String email, Long bookId, int quantity ) throws Exception {
		return mvc.perform( put( "/api/v1/cart/items/{bookId}", bookId ).with( user( email ) )
								.with( csrf() )
								.contentType( MediaType.APPLICATION_JSON )
								.content( json.writeValueAsString( Map.of( "quantity", quantity ) ) ) );
	}

	private ResultActions checkout( String email, UUID key ) throws Exception {
		return mvc.perform( post( "/api/v1/orders" ).with( user( email ) ).with( csrf() ).header( "Idempotency-Key", key.toString() ) );
	}

	private JsonNode body( MvcResult result ) throws Exception {
		return json.readTree( result.getResponse().getContentAsString() );
	}

	@Test
	void shouldReturnAnEmptyCart() throws Exception {
		cart( KISHAN ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.items" ).isEmpty() )
			.andExpect( jsonPath( "$.totalQuantity" ).value( 0 ) )
			.andExpect( jsonPath( "$.total" ).value( 0 ) )
			.andExpect( jsonPath( "$.currency" ).value( "EUR" ) );
	}

	@Test
	void shouldAddMergeAndCalculatePricesFromCatalog() throws Exception {
		add( KISHAN, firstBook, 1 ).andExpect( status().isOk() );
		add( KISHAN, firstBook, 2 ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.items.length()" ).value( 1 ) )
			.andExpect( jsonPath( "$.items[0].quantity" ).value( 3 ) )
			.andExpect( jsonPath( "$.items[0].unitPrice" ).value( 12.50 ) )
			.andExpect( jsonPath( "$.total" ).value( 37.50 ) );
		add( KISHAN, secondBook, 2 ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.totalQuantity" ).value( 5 ) )
			.andExpect( jsonPath( "$.total" ).value( 53.48 ) );
	}

	@Test
	void shouldSetQuantityAndRemoveIdempotently() throws Exception {
		add( KISHAN, firstBook, 2 ).andExpect( status().isOk() );
		quantity( KISHAN, firstBook, 4 ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.totalQuantity" ).value( 4 ) )
			.andExpect( jsonPath( "$.total" ).value( 50.00 ) );
		for ( int attempt = 0; attempt < 2; attempt++ ) {
			mvc.perform( delete( "/api/v1/cart/items/{id}", firstBook ).with( user( KISHAN ) ).with( csrf() ) ).andExpect( status().isNoContent() );
		}
		cart( KISHAN ).andExpect( jsonPath( "$.items" ).isEmpty() );
	}

	@ParameterizedTest
	@ValueSource( ints = { -1, 0, 100 } )
	void shouldRejectInvalidQuantities( int amount ) throws Exception {
		add( KISHAN, firstBook, amount ).andExpect( status().isBadRequest() );
		quantity( KISHAN, firstBook, amount ).andExpect( status().isBadRequest() );
		cart( KISHAN ).andExpect( jsonPath( "$.items" ).isEmpty() );
	}

	@Test
	void shouldRejectMissingFieldsAndInvalidBookIds() throws Exception {
		mvc.perform( post( "/api/v1/cart/items" ).with( user( KISHAN ) ).with( csrf() ).contentType( MediaType.APPLICATION_JSON ).content( "{}" ) )
			.andExpect( status().isBadRequest() );
		add( KISHAN, 0L, 1 ).andExpect( status().isBadRequest() );
		mvc.perform( delete( "/api/v1/cart/items/0" ).with( user( KISHAN ) ).with( csrf() ) ).andExpect( status().isBadRequest() );
	}

	@Test
	void shouldRejectMissingBooksAndMissingCartItems() throws Exception {
		add( KISHAN, Long.MAX_VALUE, 1 ).andExpect( status().isNotFound() ).andExpect( jsonPath( "$.status" ).value( 404 ) );
		quantity( KISHAN, firstBook, 1 ).andExpect( status().isNotFound() );
	}

	@Test
	void shouldPreventCombinedQuantityFromExceeding99() throws Exception {
		add( KISHAN, firstBook, 99 ).andExpect( status().isOk() );
		add( KISHAN, firstBook, 1 ).andExpect( status().isConflict() );
		cart( KISHAN ).andExpect( jsonPath( "$.items[0].quantity" ).value( 99 ) );
	}

	@Test
	void shouldLimitDistinctBooksButAllowUpdatingExistingItems() throws Exception {
		jdbc.update( """
						 INSERT INTO books(title,author,price,currency)
						 SELECT 'Bulk ' || n, 'Author', 1.00, 'EUR' FROM generate_series(1,100) n
						 """ );
		jdbc.update( """
						 INSERT INTO cart_items(user_id,book_id,quantity)
						 SELECT ?, id, 1 FROM books WHERE title LIKE 'Bulk %'
						 """, aliceId );
		add( KISHAN, firstBook, 1 ).andExpect( status().isConflict() );
		Long existing = jdbc.queryForObject( "SELECT min(book_id) FROM cart_items WHERE user_id=?", Long.class, aliceId );
		add( KISHAN, existing, 1 ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.items.length()" ).value( 100 ) )
			.andExpect( jsonPath( "$.totalQuantity" ).value( 101 ) );
	}

	@Test
	void shouldIsolateCartsByAuthenticatedUser() throws Exception {
		add( KISHAN, firstBook, 2 ).andExpect( status().isOk() );
		cart( RAI ).andExpect( jsonPath( "$.items" ).isEmpty() );
		quantity( RAI, firstBook, 9 ).andExpect( status().isNotFound() );
		mvc.perform( delete( "/api/v1/cart/items/{id}", firstBook ).with( user( RAI ) ).with( csrf() ) ).andExpect( status().isNoContent() );
		cart( KISHAN ).andExpect( jsonPath( "$.items[0].quantity" ).value( 2 ) );
	}

	@Test
	void shouldRequireAuthenticationAndCsrf() throws Exception {
		mvc.perform( get( "/api/v1/cart" ) ).andExpect( status().isUnauthorized() );
		mvc.perform( post( "/api/v1/cart/items" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON ).content( "{}" ) )
			.andExpect( status().isUnauthorized() );
		mvc.perform( post( "/api/v1/cart/items" ).with( user( KISHAN ) ).contentType( MediaType.APPLICATION_JSON ).content( "{}" ) )
			.andExpect( status().isForbidden() );
		mvc.perform( get( "/api/v1/orders/1" ) ).andExpect( status().isUnauthorized() );
		mvc.perform( post( "/api/v1/orders" ).with( user( KISHAN ) ).header( "Idempotency-Key", UUID.randomUUID().toString() ) )
			.andExpect( status().isForbidden() );
	}

	@Test
	void shouldPersistOrderSnapshotAndClearOnlyOwnersCart() throws Exception {
		add( KISHAN, firstBook, 2 ).andExpect( status().isOk() );
		add( KISHAN, secondBook, 1 ).andExpect( status().isOk() );
		add( RAI, secondBook, 1 ).andExpect( status().isOk() );
		MvcResult result = checkout( KISHAN, UUID.randomUUID() ).andExpect( status().isCreated() )
			.andExpect( jsonPath( "$.items.length()" ).value( 2 ) )
			.andExpect( jsonPath( "$.total" ).value( 32.99 ) )
			.andReturn();
		long id = body( result ).get( "id" ).asLong();
		assertThat( result.getResponse().getHeader( "Location" ) ).isEqualTo( "/api/v1/orders/" + id );
		cart( KISHAN ).andExpect( jsonPath( "$.items" ).isEmpty() );
		cart( RAI ).andExpect( jsonPath( "$.items.length()" ).value( 1 ) );
		jdbc.update( "UPDATE books SET price=999.00,title='Changed' WHERE id=?", firstBook );
		mvc.perform( get( "/api/v1/orders/{id}", id ).with( user( KISHAN ) ) )
			.andExpect( status().isOk() )
			.andExpect( jsonPath( "$.items[0].title" ).value( "First book" ) )
			.andExpect( jsonPath( "$.items[0].unitPrice" ).value( 12.50 ) )
			.andExpect( jsonPath( "$.total" ).value( 32.99 ) );
		mvc.perform( get( "/api/v1/orders/{id}", id ).with( user( RAI ) ) ).andExpect( status().isNotFound() );
		mvc.perform( get( "/api/v1/orders/{id}", Long.MAX_VALUE ).with( user( KISHAN ) ) ).andExpect( status().isNotFound() );
	}

	@Test
	void shouldRejectEmptyCheckoutAndInvalidKeys() throws Exception {
		checkout( KISHAN, UUID.randomUUID() ).andExpect( status().isConflict() );
		mvc.perform( post( "/api/v1/orders" ).with( user( KISHAN ) ).with( csrf() ) ).andExpect( status().isBadRequest() );
		mvc.perform( post( "/api/v1/orders" ).with( user( KISHAN ) ).with( csrf() ).header( "Idempotency-Key", "not-a-uuid" ) )
			.andExpect( status().isBadRequest() );
	}

	@Test
	void shouldReplayCheckoutWithoutConsumingNewCart() throws Exception {
		add( KISHAN, firstBook, 1 ).andExpect( status().isOk() );
		UUID key = UUID.randomUUID();
		long orderId = body( checkout( KISHAN, key ).andExpect( status().isCreated() ).andReturn() ).get( "id" ).asLong();
		add( KISHAN, secondBook, 2 ).andExpect( status().isOk() );
		checkout( KISHAN, key ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.id" ).value( orderId ) )
			.andExpect( jsonPath( "$.total" ).value( 12.50 ) );
		cart( KISHAN ).andExpect( jsonPath( "$.items[0].bookId" ).value( secondBook ) ).andExpect( jsonPath( "$.totalQuantity" ).value( 2 ) );
		assertThat( jdbc.queryForObject( "SELECT count(*) FROM customer_orders", Long.class ) ).isEqualTo( 1L );
		add( RAI, firstBook, 1 ).andExpect( status().isOk() );
		checkout( RAI, key ).andExpect( status().isCreated() ); // Keys are scoped to the customer.
	}

	private List<MvcResult> concurrently( Callable<MvcResult> operation ) throws Exception {
		var executor = Executors.newFixedThreadPool( 2 );
		var start = new CountDownLatch( 1 );
		Callable<MvcResult> task = () -> {
			start.await();
			return operation.call();
		};
		var first = executor.submit( task );
		var second = executor.submit( task );
		start.countDown();
		try {
			return List.of( first.get( 20, TimeUnit.SECONDS ), second.get( 20, TimeUnit.SECONDS ) );
		}
		finally {
			executor.shutdownNow();
			assertThat( executor.awaitTermination( 5, TimeUnit.SECONDS ) ).isTrue();
		}
	}

	@Test
	void shouldNotLoseConcurrentAdds() throws Exception {
		var results = concurrently( () -> add( KISHAN, firstBook, 1 ).andReturn() );
		assertThat( results ).allSatisfy( r -> assertThat( r.getResponse().getStatus() ).isEqualTo( 200 ) );
		cart( KISHAN ).andExpect( jsonPath( "$.items.length()" ).value( 1 ) ).andExpect( jsonPath( "$.items[0].quantity" ).value( 2 ) );
	}

	@Test
	void shouldCreateOnlyOneOrderForConcurrentRetries() throws Exception {
		add( KISHAN, firstBook, 1 ).andExpect( status().isOk() );
		UUID key = UUID.randomUUID();
		var results = concurrently( () -> checkout( KISHAN, key ).andReturn() );
		assertThat( results.stream().map( r -> r.getResponse().getStatus() ).toList() ).containsExactlyInAnyOrder( 201, 200 );
		assertThat( body( results.get( 0 ) ).get( "id" ).asLong() ).isEqualTo( body( results.get( 1 ) ).get( "id" ).asLong() );
		assertThat( jdbc.queryForObject( "SELECT count(*) FROM customer_orders", Long.class ) ).isEqualTo( 1L );
		cart( KISHAN ).andExpect( jsonPath( "$.items" ).isEmpty() );
	}

	@Test
	void shouldRollBackSavedOrderIfClearingCartFails() throws Exception {
		add( KISHAN, firstBook, 2 ).andExpect( status().isOk() );
		jdbc.execute( """
						  CREATE FUNCTION test_reject_cart_delete() RETURNS trigger AS $$
						  BEGIN RAISE EXCEPTION 'Injected cart delete failure'; END;
						  $$ LANGUAGE plpgsql
						  """ );
		try {
			jdbc.execute( """
							  CREATE TRIGGER test_cart_delete_failure BEFORE DELETE ON cart_items
							  FOR EACH ROW EXECUTE FUNCTION test_reject_cart_delete()
							  """ );
			assertThatThrownBy( () -> checkout( KISHAN, UUID.randomUUID() ) ).hasStackTraceContaining( "Injected cart delete failure" );
			assertThat( jdbc.queryForObject( "SELECT count(*) FROM customer_orders", Long.class ) ).isZero();
			assertThat( jdbc.queryForObject( "SELECT count(*) FROM order_items", Long.class ) ).isZero();
			cart( KISHAN ).andExpect( jsonPath( "$.items[0].quantity" ).value( 2 ) );
		}
		finally {
			jdbc.execute( "DROP TRIGGER IF EXISTS test_cart_delete_failure ON cart_items" );
			jdbc.execute( "DROP FUNCTION test_reject_cart_delete()" );
		}
	}

    @ParameterizedTest
    @ValueSource(strings = {"2.7", "2.0", "\"2\""})
    void shouldRejectNonIntegerJsonQuantitiesWithoutChangingCart(String value) throws Exception {
        add(KISHAN, firstBook, 1).andExpect(status().isOk());
        mvc.perform(post("/api/v1/cart/items").with(user(KISHAN)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"bookId\":" + firstBook + ",\"quantity\":" + value + "}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/cart/items/{id}", firstBook).with(user(KISHAN)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + value + "}"))
            .andExpect(status().isBadRequest());
        cart(KISHAN).andExpect(jsonPath("$.totalQuantity").value(1));
    }

    @Test
    void shouldRejectFractionalBookIdInsteadOfSelectingAnotherBook() throws Exception {
        mvc.perform(post("/api/v1/cart/items").with(user(KISHAN)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"bookId\":" + firstBook + ".9,\"quantity\":1}"))
            .andExpect(status().isBadRequest());
        cart(KISHAN).andExpect(jsonPath("$.items").isEmpty());
    }
}

