package org.example.bookstore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Provides disposable PostgreSQL or an explicitly named external test database for integration tests.
 */
@TestConfiguration( proxyBeanMethods = false )
public class PostgresTestConfiguration {

	@Bean
	@Profile("!external-test-db")
	@ServiceConnection
	PostgreSQLContainer postgresSQLContainer() {
		return new PostgreSQLContainer( "postgres:17-alpine" );
	}

    /** Optional test-only adapter for environments that cannot access the Docker socket. */
    @Bean
    @Profile("external-test-db")
    DataSource externalTestDatabase(
            @Value("${bookstore.test.database-url}") String url,
            @Value("${bookstore.test.database-username}") String username,
            @Value("${bookstore.test.database-password}") String password) {
        // Tests delete fixture tables: require an explicitly named test database.
        if (!url.matches("jdbc:postgresql://[^/]+/bookstore_[A-Za-z0-9_]*_test")) {
            throw new IllegalArgumentException("External tests require a dedicated bookstore_*_test PostgreSQL database");
        }
        return new DriverManagerDataSource(url, username, password);
    }
}
