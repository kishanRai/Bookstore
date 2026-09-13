package org.example.bookstore.configurations.documentation;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents HTTP contracts, including the logout operation implemented by a security filter. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    /**
     * Builds API metadata, session/CSRF schemes and reusable Problem Details schemas.
     *
     * @param version version displayed in the OpenAPI document
     * @return the base OpenAPI document
     */
    @Bean
    OpenAPI bookstoreOpenApi(@Value("${bookstore.api.version:0.0.1-SNAPSHOT}") String version) {
        var components = new Components()
            .addSecuritySchemes("sessionAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                .description("Log in through POST /api/v1/authentication/login. The browser stores and sends the HttpOnly cookie; do not paste it into Authorize."))
            .addSecuritySchemes("csrfToken", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")
                .description("Obtain from GET /api/v1/authentication/csrf using the same session. The bookstore Swagger page fetches and supplies this automatically before every mutation."))
            .addSchemas("ApiProblem", new ObjectSchema()
                .description("Problem Details. Framework responses may include instance or other extension fields.")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Bad Request"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Invalid request content."))
                .addProperty("instance", new StringSchema().description("Optional request URI.")));
        components.addResponses("Error400", problem(400, "Bad Request", "Invalid body, field, path/query parameter or required header."));
        components.addResponses("Error401", problem(401, "Unauthorized", "Authentication required or invalid email or password."));
        components.addResponses("Error403", problem(403, "Forbidden", "Access denied or invalid CSRF token. CSRF validation runs before controller validation."));
        components.addResponses("Error404", problem(404, "Not Found", "Book/cart item/order not found, or order belongs to another customer."));
        components.addResponses("Error409", problem(409, "Conflict", "Email already registered, cart capacity exceeded or empty cart at checkout."));
        return new OpenAPI().components(components).info(new Info().title("Bookstore API").version(version)
            .description("Online bookstore with a public catalog, session authentication, persisted carts and atomic order checkout. "
                + "Use /docs/index.html for Swagger testing with automatic CSRF handling. Register, log in, add a catalog book to your cart, then check out. "
                + "All monetary amounts are EUR. Checkout records an order; payments and stock reservation are outside scope."));
    }

    /**
     * Documents the filter-owned logout route and applies mutation-specific CSRF/security requirements.
     *
     * @return the customizer applied to the generated API paths
     */
    @Bean
    OpenApiCustomizer filterOperationsAndCsrf() {
        return api -> {
            // LogoutFilter owns this route; a documentation-only controller would create a misleading handler.
            api.path("/api/v1/authentication/logout", new PathItem().post(new Operation()
                .operationId("logout").tags(List.of("Authentication")).summary("Log out")
                .description("Invalidates the current session and expires JSESSIONID. Requires the session-bound CSRF token; also returns 204 for an anonymous session with a valid token.")
                .responses(new ApiResponses()
                    .addApiResponse("204", new ApiResponse().description("Session ended; no response body"))
                    .addApiResponse("403", new ApiResponse().$ref("#/components/responses/Error403")))));
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (path.startsWith("/api/v1/") && List.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
                        PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE).contains(method)) {
                    // Schemes in one requirement are ANDed: authenticated mutations need both cookie and CSRF.
                    var requirement = new SecurityRequirement().addList("csrfToken");
                    if (path.startsWith("/api/v1/cart") || path.startsWith("/api/v1/orders")) {
                        requirement.addList("sessionAuth");
                    }
                    operation.setSecurity(List.of(requirement));
                }
            }));
        };
    }

    /**
     * Builds a reusable Problem Details response with an illustrative status and description.
     *
     * @param status HTTP status code
     * @param title client-safe response title
     * @param description client-safe explanation of the response
     * @return the OpenAPI error response component
     */
    private static ApiResponse problem(int status, String title, String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))
                .example(Map.of("type", "about:blank", "title", title, "status", status, "detail", description))));
    }
}
