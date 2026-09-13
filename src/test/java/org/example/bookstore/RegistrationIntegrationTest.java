package org.example.bookstore;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Verifies registration validation, normalized email uniqueness and stored password hashing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Sql(
	statements = "DELETE FROM app_users",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	statements = "DELETE FROM app_users",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class RegistrationIntegrationTest {

	private static final String PASSWORD = "BNPParibas2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private ResultActions register(String email, String password)
		throws Exception {

		String body = objectMapper.writeValueAsString(
			Map.of("email", email, "password", password)
		);

		return mockMvc.perform(post("/api/v1/authentication/register")
								   .with(csrf())
								   .contentType(MediaType.APPLICATION_JSON)
								   .content(body));
	}

	static Stream<Arguments> invalidRegistrations() {
		return Stream.of(
			Arguments.of("not-an-email", PASSWORD),
			Arguments.of("", PASSWORD),
			Arguments.of("reader@example.com", "short"),
			Arguments.of("reader@example.com", " "),
			Arguments.of("reader@example.com", "x".repeat(129))
		);
	}

	@Test
	void shouldRegisterUserAndStorePasswordHash() throws Exception {
		register("Reader@Example.com", PASSWORD)
			.andExpect(status().isCreated())
			.andExpect(content()
						   .contentTypeCompatibleWith( MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.email").value("reader@example.com"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		String storedHash = jdbcTemplate.queryForObject(
			"""
			SELECT password_hash
			FROM app_users
			WHERE email = ?
			""",
			String.class,
			"reader@example.com"
		);

		assertThat(storedHash).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, storedHash)).isTrue();
	}

	@Test
	void shouldRejectDuplicateEmailIgnoringCase() throws Exception {
		register("reader@example.com", PASSWORD)
			.andExpect(status().isCreated());

		register("READER@example.com", PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(
				MediaType.APPLICATION_PROBLEM_JSON
			))
			.andExpect(jsonPath("$.status").value(409));

		Long count = jdbcTemplate.queryForObject(
			"SELECT count(*) FROM app_users",
			Long.class
		);

		assertThat(count).isEqualTo(1L);
	}

	@ParameterizedTest
	@MethodSource("invalidRegistrations")
	void shouldRejectInvalidRegistration(String email, String password)
		throws Exception {

		register(email, password)
			.andExpect(status().isBadRequest());

		Long count = jdbcTemplate.queryForObject(
			"SELECT count(*) FROM app_users",
			Long.class
		);

		assertThat(count).isZero();
	}

}
