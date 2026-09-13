package org.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import( PostgresTestConfiguration.class )

@Sql( statements = "DELETE FROM app_users", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD )
@Sql( statements = "DELETE FROM app_users", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD )
class AuthenticationIntegrationTest {

	private static final String EMAIL = "kishan@email.com";
	private static final String PASSWORD = "BookstoreTest2026!";
	private static final String AUTH = "/api/v1/authentication";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	private Long insertUser() {
		return jdbcTemplate.queryForObject( """
												INSERT INTO app_users (email, password_hash)
												VALUES (?, ?) RETURNING id
												""", Long.class, EMAIL, passwordEncoder.encode( PASSWORD ) );
	}

	private record CsrfSession(MockHttpSession session, String headerName, String token) {

	}

	// Exercise the real CSRF endpoint and session, without mocking authentication.
	private CsrfSession csrf( MockHttpSession existingSession ) throws Exception {
		var request = get( AUTH + "/csrf" );
		if ( existingSession != null ) {
			request.session( existingSession );
		}
		MvcResult result = mockMvc.perform( request )
			.andExpect( status().isOk() )
			.andExpect( jsonPath( "$.headerName" ).isString() )
			.andExpect( jsonPath( "$.token" ).isString() )
			.andReturn();

		var body = objectMapper.readTree( result.getResponse().getContentAsString() );
		var session = (MockHttpSession) result.getRequest().getSession( false );
		assertThat( session ).isNotNull();
		assertThat( body.get( "token" ).asText() ).isNotBlank();
		return new CsrfSession( session, body.get( "headerName" ).asText(), body.get( "token" ).asText() );
	}

	private ResultActions login( CsrfSession csrf, String email, String password ) throws Exception {
		return mockMvc.perform( post( AUTH + "/login" ).session( csrf.session() )
									.header( csrf.headerName(), csrf.token() )
									.contentType( MediaType.APPLICATION_JSON )
									.content( objectMapper.writeValueAsString( Map.of( "email", email, "password", password ) ) ) );
	}

	@Test
	void shouldLoginNormalizeEmailRotateSessionAndRememberUser() throws Exception {
		Long userId = insertUser();
		CsrfSession csrf = csrf( null );
		String originalSessionId = csrf.session().getId();

		login( csrf, EMAIL.toUpperCase( java.util.Locale.ROOT ), PASSWORD ).andExpect( status().isOk() )
			.andExpect( jsonPath( "$.id" ).value( userId ) )
			.andExpect( jsonPath( "$.email" ).value( EMAIL ) )
			.andExpect( jsonPath( "$.password" ).doesNotExist() )
			.andExpect( jsonPath( "$.passwordHash" ).doesNotExist() );

		assertThat( csrf.session().getId() ).isNotEqualTo( originalSessionId );

		mockMvc.perform( get( AUTH + "/me" ).session( csrf.session() ) )
			.andExpect( status().isOk() )
			.andExpect( jsonPath( "$.id" ).value( userId ) )
			.andExpect( jsonPath( "$.email" ).value( EMAIL ) );
	}

	@Test
	void shouldRejectWrongPasswordWithoutAuthenticatingSession() throws Exception {
		insertUser();
		CsrfSession csrf = csrf( null );
		login(csrf, EMAIL, "WrongPassword2026!").andExpect( status().isUnauthorized() )
			.andExpect( jsonPath( "$.status" ).value( 401 ) )
			.andExpect( jsonPath( "$.detail" ).value( "Invalid email or password" ) );

		mockMvc.perform( get( AUTH + "/me" ).session( csrf.session() ) ).andExpect( status().isUnauthorized() );
	}

	@Test
	void shouldReturnSameErrorForUnknownEmail() throws Exception {
		CsrfSession csrf = csrf( null );
		login(csrf, "unknown@example.com", PASSWORD).andExpect( status().isUnauthorized() )
			.andExpect( jsonPath( "$.status" ).value( 401 ) )
			.andExpect( jsonPath( "$.detail" ).value( "Invalid email or password" ) );
	}

	@Test
	void shouldRequireLoginForCurrentUser() throws Exception {
		mockMvc.perform( get( AUTH + "/me" ) ).andExpect( status().isUnauthorized() );
	}

	@Test
	void shouldRejectLoginWithoutCsrfToken() throws Exception {
		insertUser();
		mockMvc.perform( post( AUTH + "/login" ).contentType( MediaType.APPLICATION_JSON )
							 .content( objectMapper.writeValueAsString( Map.of( "email", EMAIL, "password", PASSWORD ) ) ) )
			.andExpect( status().isForbidden() );
	}

	@Test
	void shouldLogoutAndInvalidateSession() throws Exception {
		insertUser();
		CsrfSession beforeLogin = csrf( null );
		login( beforeLogin, EMAIL, PASSWORD ).andExpect( status().isOk() );

		// Successful authentication clears the old CSRF token. Fetch a new one.
		CsrfSession afterLogin = csrf( beforeLogin.session() );
		mockMvc.perform( post( AUTH + "/logout" ).session( afterLogin.session() ).header( afterLogin.headerName(), afterLogin.token() ) )
			.andExpect( status().isNoContent() );

		assertThat( afterLogin.session().isInvalid() ).isTrue();
		mockMvc.perform( get( AUTH + "/me" ) ).andExpect( status().isUnauthorized() );
	}

}
