package org.example.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies that the Spring application context starts with the PostgreSQL test configuration.
 */
@SpringBootTest
@Import( PostgresTestConfiguration.class )
class BookstoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
