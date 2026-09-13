package org.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies the published API contract, bundled Swagger assets and session-aware documentation flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class OpenApiIntegrationTest {
    private static final String AUTH = "/api/v1/authentication";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void documentationAndBundledAssetsArePublic() throws Exception {
        mvc.perform(get("/docs/index.html")).andExpect(status().isOk())
            .andExpect(content().string(containsString("bookstore-docs.js")));
        mvc.perform(get("/docs/bookstore-docs.js")).andExpect(status().isOk())
            .andExpect(content().string(containsString("requestInterceptor: bookstoreRequestInterceptor")));
        for (String asset : new String[]{"swagger-ui.css", "swagger-ui-bundle.js", "swagger-ui-standalone-preset.js"}) {
            mvc.perform(get("/swagger-ui/" + asset)).andExpect(status().isOk());
        }
        mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Bookstore API")));
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post(AUTH + "/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"reader@example.com\",\"password\":\"BookstoreTest2026!\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void contractIncludesAllBusinessOperationsAndFilterBasedLogout() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Bookstore API"))
            .andExpect(jsonPath("$.components.securitySchemes.sessionAuth.in").value("cookie"))
            .andExpect(jsonPath("$.components.securitySchemes.sessionAuth.name").value("JSESSIONID"))
            .andExpect(jsonPath("$.components.securitySchemes.csrfToken.name").value("X-CSRF-TOKEN"))
            .andReturn();
        var api = mapper.readTree(result.getResponse().getContentAsString());
        var paths = api.path("paths");
        assertThat(paths.size()).isEqualTo(11);
        assertThat(paths.path(AUTH + "/csrf").has("get")).isTrue();
        assertThat(paths.path(AUTH + "/register").path("post").path("responses").has("201")).isTrue();
        assertThat(paths.path(AUTH + "/login").path("post").path("responses").has("401")).isTrue();
        assertThat(paths.path(AUTH + "/me").has("get")).isTrue();
        var logout = paths.path(AUTH + "/logout").path("post");
        assertThat(logout.path("responses").has("204")).isTrue();
        assertThat(logout.path("responses").path("204").has("content")).isFalse();
        assertThat(logout.has("requestBody")).isFalse();
        assertThat(paths.path("/api/v1/cart/items/{bookId}").has("put")).isTrue();
        assertThat(paths.path("/api/v1/cart/items/{bookId}").path("delete").path("responses").has("204")).isTrue();
        assertThat(paths.path("/api/v1/orders/{orderId}").path("get").path("responses").has("404")).isTrue();
        assertThat(api.path("components").path("responses").path("Error403").path("content")
            .has("application/problem+json")).isTrue();
    }

    @Test
    void contractPreservesPaginationLimitsAndCheckoutRetrySemantics() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var api = mapper.readTree(result.getResponse().getContentAsString());
        var catalog = api.path("paths").path("/api/v1/books").path("get");
        assertThat(catalog.path("parameters").size()).isEqualTo(2);
        for (var parameter : catalog.path("parameters")) {
            boolean page = "page".equals(parameter.path("name").asText());
            var schema = parameter.path("schema");
            assertThat(schema.path("type").asText()).isEqualTo("integer");
            assertThat(schema.path("minimum").asInt()).isEqualTo(page ? 0 : 1);
            assertThat(schema.path("maximum").asInt()).isEqualTo(page ? 10000 : 100);
            assertThat(schema.path("default").asInt()).isEqualTo(page ? 0 : 20);
        }
        var checkout = api.path("paths").path("/api/v1/orders").path("post");
        assertThat(checkout.path("responses").has("200")).isTrue();
        assertThat(checkout.path("responses").has("201")).isTrue();
        assertThat(checkout.path("responses").path("201").path("headers").has("Location")).isTrue();
        assertThat(checkout.has("requestBody")).isFalse();
        assertThat(checkout.path("parameters").size()).isEqualTo(1);
        var key = checkout.path("parameters").get(0);
        assertThat(key.path("name").asText()).isEqualTo("Idempotency-Key");
        assertThat(key.path("required").asBoolean()).isTrue();
        assertThat(key.path("schema").path("format").asText()).isEqualTo("uuid");
        var security = checkout.path("security");
        assertThat(security.size()).isEqualTo(1);
        assertThat(security.get(0).has("sessionAuth")).isTrue();
        assertThat(security.get(0).has("csrfToken")).isTrue();
        var schemas = api.path("components").path("schemas");
        assertThat(schemas.path("BookPageResponse").path("properties").has("content")).isTrue();
        assertThat(schemas.path("AddCartItemRequest").path("properties").path("quantity").path("maximum").asInt()).isEqualTo(99);
        assertThat(schemas.path("RegistrationRequest").path("properties").path("password").path("writeOnly").asBoolean()).isTrue();
        assertThat(schemas.path("UserResponse").path("properties").has("password")).isFalse();
    }

    @Test
    void documentedBrowserFlowWorksWithRealCsrfAndSessionRotation() throws Exception {
        String email = "docs-" + UUID.randomUUID() + "@example.com";
        String credentials = mapper.writeValueAsString(Map.of("email", email, "password", "BookstoreTest2026!"));
        try {
            var csrf = csrf(null);
            mvc.perform(post(AUTH + "/register").session(csrf.session())
                .header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());
            csrf = csrf(csrf.session());
            var login = mvc.perform(post(AUTH + "/login").session(csrf.session())
                .header(csrf.headerName(), csrf.token()).contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk()).andReturn();
            var session = (MockHttpSession) login.getRequest().getSession(false);
            mvc.perform(get("/api/v1/cart").session(session)).andExpect(status().isOk());
            // Token rotation makes the pre-login token invalid for subsequent mutations.
            mvc.perform(post(AUTH + "/logout").session(session).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isForbidden());
            csrf = csrf(session);
            mvc.perform(post(AUTH + "/logout").session(session).header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());
            mvc.perform(get(AUTH + "/me")).andExpect(status().isUnauthorized());
        } finally {
            jdbc.update("DELETE FROM app_users WHERE email = ?", email);
        }
    }

    private CsrfSession csrf(MockHttpSession existing) throws Exception {
        var request = get(AUTH + "/csrf");
        if (existing != null) request.session(existing);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = mapper.readTree(result.getResponse().getContentAsString());
        return new CsrfSession((MockHttpSession) result.getRequest().getSession(false),
            body.path("headerName").asText(), body.path("token").asText());
    }

    /**
     * Keeps a test session and its CSRF header/token together when exercising authenticated mutations.
     */
    private record CsrfSession(MockHttpSession session, String headerName, String token) { }
}
