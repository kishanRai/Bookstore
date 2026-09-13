# Bookstore API

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white)
![Build](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)

A Java 17 / Spring Boot REST API for an online bookstore, with a paginated book catalog, user registration, session authentication, persisted shopping carts and transactional order checkout backed by PostgreSQL.

**Implemented:** catalog listing, bounded pagination, registration, login, current-user lookup, logout, CSRF protection, API validation, persisted carts and order checkout.

## Contents

- [Run from a new machine](#run-from-a-new-machine)
- [Explore with Swagger UI](#explore-with-swagger-ui)
- [Test with Postman](#test-with-postman)
- [API reference](#api-reference)
- [Cart and checkout](#cart-and-checkout)
- [Architecture and security](#architecture-and-security)
- [Tests](#tests)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Remaining work](#remaining-work)

## Run from a new machine

### 1. Install prerequisites

| Requirement | Purpose |
|---|---|
| JDK 17 | Compile and run the backend; set `JAVA_HOME` to the JDK directory |
| Git | Clone the repository |
| Docker Desktop with Linux containers, or Docker Engine with Compose v2 | Run PostgreSQL and Testcontainers integration tests |
| Internet access on first build | Download Maven, dependencies and the PostgreSQL image |
| Postman (optional) | Run the supplied API collection |

Start Docker before running tests. A separate Maven or PostgreSQL installation is not required: the repository includes the Maven Wrapper, and Docker supplies PostgreSQL.

### 2. Clone and check the tools

```shell
git clone https://github.com/kishanRai/Bookstore.git
cd Bookstore
java -version
docker version
docker compose version
```

`java -version` should report Java 17, and `docker version` should show a reachable server. Run subsequent commands from this repository directory.

### 3. Build and run the tests

Windows PowerShell:

```powershell
.\mvnw.cmd -v
.\mvnw.cmd clean verify
```

macOS/Linux:

```bash
chmod +x mvnw
./mvnw -v
./mvnw clean verify
```

Check that Maven also reports Java 17. The first run downloads dependencies and may take longer. Integration tests create disposable PostgreSQL containers with dynamically assigned ports; they do not require the Compose database or the `local` profile. Continue after Maven reports `BUILD SUCCESS` and no test failures.

### 4. Start the local database

```shell
docker compose up -d --wait
docker compose ps
```

The `postgres` service should be healthy. Compose exposes PostgreSQL at `127.0.0.1:15432` and stores its data in a named volume. Port `5432` is used inside the container.

### 5. Start the application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

macOS/Linux:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Keep this terminal open. The `local` profile connects to the Compose database, Flyway creates or migrates the schema, and Hibernate validates its mappings. Wait for the application-started message; the API listens on `http://localhost:8080`.

**IntelliJ alternative:** open `pom.xml` as a Maven project, select JDK 17, reload Maven, and run `BookstoreApplication` with **Active profiles** set to just `local`. The full `--spring.profiles.active=local` argument does not belong in that field. Do not start both the Maven and IntelliJ application processes on port 8080.

### 6. Confirm health and add optional demo data

In a second terminal, check health. Use `curl.exe` in Windows PowerShell; use `curl` on macOS/Linux:

```powershell
curl.exe http://localhost:8080/actuator/health
curl.exe "http://localhost:8080/api/v1/books?page=0&size=20"
```

Health should include `"status":"UP"`. A fresh database has no books or users, so an empty catalog response is expected. There is no catalog write API yet. To insert one sample book, run this single-line command in any of the shells above:

```shell
docker compose exec -T postgres psql -U bookstore -d bookstore -c "INSERT INTO books (title, author, price, currency) SELECT 'Clean Code', 'Robert C. Martin', 35.00, 'EUR' WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = 'Clean Code' AND author = 'Robert C. Martin');"
```

The demo command avoids inserting another matching row when rerun sequentially. Actual IDs are assigned by PostgreSQL.

### 7. Exercise the backend

Open [Swagger UI](http://localhost:8080/docs/index.html) for interactive API documentation and follow [Explore with Swagger UI](#explore-with-swagger-ui). Alternatively, follow [Test with Postman](#test-with-postman) to run the supplied assertions for registration, authentication, cart and checkout.

To stop local development, press `Ctrl+C` in the application terminal, then run `docker compose down`. The database volume is retained for the next start.

## Explore with Swagger UI

Start the application using setup steps 1–6, then open **[Bookstore Swagger UI](http://localhost:8080/docs/index.html)** in your browser. The page uses Swagger assets bundled with the Maven dependency; no CDN or separate frontend build is required.

| Resource | Local URL | Purpose |
|---|---|---|
| Swagger UI | [`/docs/index.html`](http://localhost:8080/docs/index.html) | Interactive documentation with automatic CSRF handling |
| OpenAPI JSON | [`/v3/api-docs`](http://localhost:8080/v3/api-docs) | Machine-readable HTTP API contract |
| OpenAPI YAML | [`/v3/api-docs.yaml`](http://localhost:8080/v3/api-docs.yaml) | The same contract in YAML format |

Expand an operation, select **Try it out**, enter its parameters/body, then select **Execute**. The catalog, authentication, cart and order sections include request/response schemas, field examples, pagination and quantity limits, and expected error responses. Health remains available at `/actuator/health` as described in setup; the generated contract covers the business API under `/api/v1`.

### Browser walkthrough

1. **Catalog → GET /api/v1/books:** fetch available books and note a returned ID. Seed the demo book from setup step 6 if the catalog is empty.
2. **Authentication → POST /register:** enter your own unused email and a password of 12–128 characters. Expect `201`. If already registered, continue with login.
3. **Authentication → POST /login:** enter the same credentials. Expect `200`, then call **GET /me** to confirm the session.
4. **Cart → POST /api/v1/cart/items:** use the catalog ID and `quantity: 2`. Inspect **GET /api/v1/cart**; use PUT to replace quantity or DELETE to remove a line.
5. **Orders → POST /api/v1/orders:** leave the body empty and enter a UUID in **Idempotency-Key**. In PowerShell generate one with `[guid]::NewGuid().ToString()`; in the browser console use `crypto.randomUUID()`. A nonempty cart returns `201` and its saved order summary.
6. Execute checkout again with the **same UUID**: expect `200` and the same order. Use **GET /api/v1/orders/{orderId}** with its ID to retrieve it. Generate a new UUID for a subsequent purchase.
7. **Authentication → POST /logout:** expect `204`. **GET /me** now returns `401`.

The browser retains the HttpOnly `JSESSIONID` cookie. **Use the login operation to authenticate; no value needs to be pasted into Authorize.** The bookstore Swagger page fetches `/api/v1/authentication/csrf` with the same session before every POST, PUT, PATCH or DELETE and sends the returned `X-CSRF-TOKEN`. This also refreshes the token after login/logout. CSRF checks remain enabled for every client. Open `/docs/index.html` for this behavior; the dependency's default `/swagger-ui/index.html` does not include the bookstore interceptor.

Swagger and Postman have separate cookie jars, so log in separately in each. Use the same browser tab/host throughout. These requests change the database just like Postman requests: registrations and completed orders persist. Swagger's generated curl snippets do not carry the browser's HttpOnly session cookie; terminal clients must retain cookies and fetch CSRF as described under [Session and CSRF flow for other clients](#session-and-csrf-flow-for-other-clients).

### Maintaining documentation

- Controller OpenAPI annotations describe operations and status codes; DTO schema annotations and Jakarta validation describe bodies and limits.
- `OpenApiConfiguration` defines session/CSRF schemes, reusable Problem Details responses and logout, which is implemented by Spring Security's filter rather than a controller.
- Focused Javadoc on catalog, registration, cart, checkout and customer locking explains transaction boundaries and design tradeoffs. A generated HTML Javadoc site is not required to run the application.
- Keep this README, the generated contract and the Postman collection aligned when changing an endpoint. The OpenAPI integration test checks documentation availability and key contracts.

The project uses [springdoc-openapi](https://springdoc.org/) `3.1.1` for Spring Boot 4, with [Swagger UI's request interceptor](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/) for the browser CSRF flow.

## Test with Postman

1. Select **Import → Files** and choose [`postman/Bookstore-Ready-to-Test.postman_collection.json`](postman/Bookstore-Ready-to-Test.postman_collection.json).
2. Open **Bookstore - Ready to Test** and select **No environment**. The collection includes `baseUrl = http://localhost:8080`; a separate environment file is not needed.
3. Keep Postman's cookie jar enabled. It retains the session cookie automatically.
4. Send **Ops → Health Check**, then **Catalog → Get Books**.
5. Run **Authentication → Registration** in its numbered order.
6. Run **Authentication → Session** in order: login (`200`), current user (`200`), logout (`204`), and current user after logout (`401`).
7. Seed at least one book, then run **Cart and Checkout** in order; it logs in again and creates one test order.

The collection contains **37 saved requests**: one health request, one catalog request, 15 registration cases, four session requests and 16 cart/checkout requests. A collection pre-request script also calls `/api/v1/authentication/csrf` before each mutation and adds the returned header/token. These auxiliary requests are not included in the saved-request count.

Registration expects `201` for cases 01, 14 and 15; `409` for cases 02 and 03; and `400` for cases 04–13. Expected error responses count as successful tests when their assertions pass. A complete registration run creates three persistent test accounts. Saved example responses are illustrative.

### Custom email and reusable variables

Request **01 - Register user successfully** normally generates a fresh `registrationEmail` for every send. To use your own email, change its **Body → raw → JSON** to:

```json
{
  "email": "reader@example.com",
  "password": "BookstoreTest2026!"
}
```

Leave the scripts enabled. After a `201` response, the request saves `registeredEmail`, `registeredUserId` and `registeredPassword` as collection variables. The Session login request reuses them:

```json
{
  "email": "{{registeredEmail}}",
  "password": "{{registeredPassword}}"
}
```

`registrationEmail` is the address being submitted; `registeredEmail` is the normalized address returned after successful registration. Inspect saved values under **collection → Variables**. Reusing an existing email at the registration endpoint intentionally returns `409`; use the login endpoint to access that account. Restore `"{{registrationEmail}}"` in request 01 for repeatable automated registration runs. Use test credentials when running or exporting this collection.

## API reference

Base URL: `http://localhost:8080`. Request and success-response bodies use JSON unless the endpoint returns no content.

| Method | Path | Access | Success |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | `200` with health status when healthy |
| `GET` | `/api/v1/books` | Public | `200` with a page of books |
| `GET` | `/api/v1/authentication/csrf` | Public; creates CSRF session state as needed | `200` with header name and token |
| `POST` | `/api/v1/authentication/register` | Public, with CSRF token and associated cookie | `201` with user ID and email |
| `POST` | `/api/v1/authentication/login` | Public, with CSRF token and associated cookie | `200` with user ID and email; authenticates session |
| `GET` | `/api/v1/authentication/me` | Authenticated session | `200` with user ID and email |
| `POST` | `/api/v1/authentication/logout` | CSRF token and associated session cookie | `204`, no body; invalidates current session |
| `GET` | `/api/v1/cart` | Authenticated session | `200` with cart and totals |
| `POST` | `/api/v1/cart/items` | Authenticated session and CSRF | `200` with updated cart |
| `PUT` | `/api/v1/cart/items/{bookId}` | Authenticated session and CSRF | `200` with updated cart |
| `DELETE` | `/api/v1/cart/items/{bookId}` | Authenticated session and CSRF | `204`, no body |
| `POST` | `/api/v1/orders` | Authenticated session, CSRF and Idempotency-Key | `201` for creation; `200` for retry |
| `GET` | `/api/v1/orders/{orderId}` | Authenticated owner | `200` with saved summary |

### Catalog pagination

```http
GET /api/v1/books?page=0&size=20
```

| Parameter | Type | Default | Inclusive limits | Meaning |
|---|---|---|---|---|
| `page` | Integer | `0` | `0`–`10000` | Zero-based page number |
| `size` | Integer | `20` | `1`–`100` | Maximum books returned on a page |

Both parameters are optional; omitted or empty values use their defaults. Sorting is always by ascending `id`. Out-of-range, fractional, nonnumeric or integer-overflow values return `400` rather than being clamped.

Illustrative response for a catalog containing one book:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Clean Code",
      "author": "Robert C. Martin",
      "price": 35.00,
      "currency": "EUR"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

| Field | Meaning |
|---|---|
| `content` | Array of books on the requested page |
| `page` / `size` | Requested page number and capacity; `size` is not the returned item count |
| `totalElements` | Total books across the catalog; Java `long` |
| `totalPages` | Pages at the requested size; zero for an empty catalog |
| `hasNext` | Whether another page of data exists |

Book IDs are integers; title and author are strings. Prices use Java `BigDecimal` and JSON numbers; clients format decimal places for display. Currency is constrained to `EUR` by the database.

An empty catalog returns `200` with `content: []`, zero totals and `hasNext: false`. A valid page beyond the last result also returns `200` with empty content while preserving the catalog's totals. Clients read `response.content`, not a top-level array.

Pagination is applied in the database. Offset queries and total-count queries can still become costly for large catalogs; a size bound does not guarantee constant query time. At size 100, page 10000 starts at offset 1000000. `hasNext` reflects available data and does not override the page-number cap. Separate requests do not share a snapshot, so concurrent catalog changes can shift results or totals. Cursor pagination is a possible later improvement.

### Registration and login

Both endpoints accept `email` and `password` fields, as shown in the Postman examples.

| Field | Registration validation | Login validation |
|---|---|---|
| `email` | Nonblank, valid email syntax, at most 254 characters | Same |
| `password` | Nonblank, 12–128 characters | Nonblank, at most 128 characters; credentials must match |

Email is normalized to lowercase using `Locale.ROOT`; login and duplicate detection are case-insensitive. Passwords are not trimmed or case-normalized. Successful registration (`201`), login (`200`) and current-user lookup (`200`) return this shape:

```json
{
  "id": 1,
  "email": "reader@example.com"
}
```

Registration creates an account but does not log it in. Passwords and hashes are excluded from these responses. Duplicate registration returns `409`. Unknown-user and incorrect-password login attempts return the same generic `401` message.

### Session and CSRF flow for other clients

1. `GET /api/v1/authentication/csrf`; retain the session cookie and read `headerName` and `token` from the JSON response.
2. Send registration or login with that cookie, `Content-Type: application/json` and the returned header/token, normally `X-CSRF-TOKEN`.
3. On successful login, retain the updated session cookie and fetch `/csrf` again before the next mutation: login rotates the session ID and clears the previous CSRF token.
4. Send `/me` with the same cookie to retrieve the authenticated user.
5. Send `POST /logout` with the current CSRF token and cookie. Afterwards, `/me` returns `401`; fetch a new CSRF token before another registration or login attempt.

Example CSRF response (the value is generated at runtime):

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "<generated-token>"
}
```

Authentication uses the `JSESSIONID` cookie, not a bearer token. Browser clients must use `credentials: "include"` for requests participating in the session, including the initial CSRF request. Local credentialed CORS permits `http://localhost:5173` and `http://localhost:3000`; use `localhost` consistently rather than mixing it with `127.0.0.1`.

### Error responses

Controller validation and application exceptions are handled through `ApiExceptionHandler`; authentication/access failures in the security filters also return `application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid email or password"
}
```

| Status | Typical cause |
|---|---|
| `400` | Malformed JSON, invalid request fields or invalid pagination |
| `401` | Incorrect login credentials or anonymous access to `/me` |
| `403` | Missing/invalid CSRF token or forbidden access |
| `404` | Missing book/cart item/order, or an order owned by another user |
| `409` | Email already registered, cart capacity exceeded, or checkout of an empty cart |

CSRF validation runs before the controller, so an invalid POST without a valid token can return `403` before field validation would return `400`. Framework-generated Problem Details may include additional fields such as `instance`; clients should use the HTTP status and available fields instead of assuming an exact error-property set.

## Cart and checkout

All cart and order endpoints use the authenticated session. POST, PUT and DELETE also require CSRF. Ownership is resolved on the server; requests do not accept a customer ID, prices or order totals as authoritative input.

### Cart operations

| Method | Endpoint | Result |
|---|---|---|
| `GET` | `/api/v1/cart` | `200` with items, total quantity, total and currency |
| `POST` | `/api/v1/cart/items` | `200` with the updated cart; adds to an existing book's quantity |
| `PUT` | `/api/v1/cart/items/{bookId}` | `200` with the updated cart; replaces quantity |
| `DELETE` | `/api/v1/cart/items/{bookId}` | `204`; repeated removal of an absent item also returns `204` |

Add a book using its ID from the catalog:

```json
{"bookId": 1, "quantity": 2}
```

Update quantity with `{"quantity": 4}`. Quantity must be an integer from 1 to 99; use DELETE to remove an item. Invalid request fields return `400`, an unknown book or a missing item on PUT returns `404`, and exceeding the combined quantity or cart capacity returns `409`. A cart supports up to 100 distinct books.

Illustrative cart for two copies priced at EUR 12.50 each:

```json
{
  "items": [{"bookId": 1, "title": "Example book", "author": "Example author", "unitPrice": 12.50, "quantity": 2, "lineTotal": 25.00}],
  "totalQuantity": 2,
  "total": 25.00,
  "currency": "EUR"
}
```

The empty cart returns `items: []`, `totalQuantity: 0`, `total: 0.00` and `currency: "EUR"`. Items are ordered by book ID. Cart contents persist across logout and application restarts; cart prices reflect the catalog at the time of the request.

### Create and retrieve an order

Send `POST /api/v1/orders` without a body, with the session cookie, CSRF header and an `Idempotency-Key` header containing a UUID. The server checks out the user's current cart. A new key and nonempty cart return `201 Created` with `Location: /api/v1/orders/{id}` and an order summary. An empty cart returns `409`; an absent or malformed key returns `400`.

```json
{
  "id": 1,
  "createdAt": "2026-09-13T12:00:00Z",
  "items": [{"bookId": 1, "title": "Example book", "author": "Example author", "unitPrice": 12.50, "quantity": 2, "lineTotal": 25.00}],
  "total": 25.00,
  "currency": "EUR"
}
```

`GET /api/v1/orders/{id}` returns the saved summary to its owner. Missing orders and orders belonging to another user both return `404`. Order titles, authors and unit prices are copied at checkout, so subsequent catalog changes do not rewrite historical orders. Monetary calculations use `BigDecimal`. This checkout records an order; payments, stock reservation, shipping and tax calculation are not implemented.

Generate one key per checkout attempt and reuse it if the response is lost. Retrying a committed checkout with the same key returns the original order with `200`, even if the cart now contains new items; those new items are not consumed. Keys are scoped to the user and retained with the order. Use a new key for a genuinely new order. Adding cart items via POST is not idempotent; replaying an add increases quantity again.

Order creation, item snapshots and cart clearing run in a single database transaction. All cart operations and checkout lock the owning `app_users` row with a pessimistic write lock at READ COMMITTED isolation, which serializes concurrent operations for that user while allowing different users to proceed independently. Database uniqueness constraints backstop one cart row per user/book and one order per user/key. This coarse lock is a deliberate tradeoff for a bounded cart; inventory and concurrent catalog administration would require additional coordination.

### Validate the complete flow in Postman

Seed at least one catalog book using the setup command, then run Registration, Session, and **Cart and Checkout** in order with a fresh test account. The last folder logs in again, fetches a book ID, checks cart CRUD and invalid quantity handling, creates one order, retries the same checkout key, reads the order, verifies the empty cart and logs out. A complete collection run creates three test users and one test order.

Run **Create order** once; use **Retry the same checkout** to resend its key. Clicking Create order again generates a new key. The collection's existing CSRF script obtains fresh tokens for all mutations. React's credentialed CORS configuration permits `Idempotency-Key` in addition to the existing headers.

## Architecture and security

| Layer / tool | Implementation |
|---|---|
| Runtime and framework | Java 17, Spring Boot 4.1.1, Spring MVC and Jakarta Validation |
| Persistence | Spring Data JPA, PostgreSQL 17, Flyway |
| Authentication | Spring Security, database-backed `UserDetailsService`, existing PBKDF2 encoder |
| Tests | JUnit, Mockito, MockMvc and PostgreSQL Testcontainers |
| Build and local development | Maven Wrapper, Docker Compose, Lombok and DevTools |

The code is grouped by technical layer, with `catalog`, `authentication`, `cart` and `orders` subpackages:

```text
src/main/java/org/example/bookstore/
  configurations/security/    HTTP security, authentication provider, password encoder
  controllers/               Request mapping and validation
  services/                  Catalog mapping, authentication, cart rules and transactional checkout
  repositories/              Database access
  entities/                  Book, AppUser, CartItem, PurchaseOrder and OrderItem models
  dtos/                      API request/response records
  exceptions/                Application exceptions and Problem Details

src/main/resources/db/migration/
  V1__create_books.sql
  V2__create_app_users.sql
  V3__create_cart_and_orders.sql
```

- `DaoAuthenticationProvider` verifies credentials against stored PBKDF2 hashes. The login request's string representation redacts credentials.
- JSON login explicitly invokes session authentication strategies and saves the security context. The session ID changes on authentication to protect against session fixation.
- Spring Security's logout filter clears authentication, invalidates the session and expires its cookie. Form login, HTTP Basic and saved-request redirects are disabled for this API.
- `CurrentUserService` resolves user identity from the authenticated principal. HTTP security is separate from database authentication configuration so MVC slice tests can exercise access rules without loading repositories.
- Flyway owns the schema; `ddl-auto=validate` checks mappings rather than changing tables. Database constraints enforce unique normalized email, valid catalog values and EUR currency.

Applied Flyway migrations must remain unchanged, including formatting. Add a new versioned migration for future schema changes.

## Tests

| Test class | Coverage |
|---|---|
| `BookControllerTest` | MVC contract, pagination defaults/bounds and invalid input, using actual HTTP security rules and a mocked catalog service |
| `BookCatalogIntegrationTest` | Database-backed catalog ordering, page metadata, empty catalog and pages beyond the last result |
| `RegistrationIntegrationTest` | Registration, stored password hashing, case-insensitive duplicates and validation, with CSRF tokens |
| `AuthenticationIntegrationTest` | Login, normalization, session rotation/persistence, wrong credentials, unknown users, anonymous access, missing CSRF and logout |
| `CartCheckoutIntegrationTest` | Cart operations, limits, owner isolation, snapshots, idempotency, concurrent updates and transaction rollback |
| `OpenApiIntegrationTest` | Public documentation/assets, endpoint schemas, security requirements and CSRF session flow |
| `BookstoreApplicationTests` | Application-context startup |

Integration tests use `PostgresTestConfiguration` and `@ServiceConnection`; they apply the real Flyway migrations to disposable PostgreSQL containers. Authentication tests obtain CSRF tokens from the endpoint and reuse sessions across requests. Catalog MVC tests use `@WebMvcTest`; database integration tests use `@SpringBootTest` with MockMvc.

Run the full suite with the build command in setup step 3. For focused authentication work:

```powershell
.\mvnw.cmd "-Dtest=AuthenticationIntegrationTest,RegistrationIntegrationTest,BookControllerTest" test
```

Use `./mvnw` instead of `.\mvnw.cmd` on macOS/Linux. Test reports are generated under `target/surefire-reports/`. Test coverage described here is not a substitute for a successful verification run of the revision being reviewed.

For documentation changes, run `.\mvnw.cmd "-Dtest=OpenApiIntegrationTest" test`. The small browser request interceptor also has optional tests using Node.js 22 or later:

```shell
node --test src/test/javascript/bookstore-docs.test.cjs
```

These JavaScript tests use Node's built-in test runner with no npm dependencies. Node is not required to build or run the backend; Java integration tests run in the normal Maven suite.

## Configuration

| Setting | Local value / behavior |
|---|---|
| Application port | `8080` |
| Active profile for local startup | `local` |
| Database URL | `jdbc:postgresql://127.0.0.1:15432/bookstore` |
| Local database credentials | `bookstore` / `bookstore_local`, for the Compose development database |
| Session timeout | 30 minutes of inactivity |
| Session cookie | `HttpOnly=true`, `SameSite=Lax` |
| Open Session in View | Disabled |
| Exposed actuator endpoint | Health; internal health details hidden |
| API documentation | Public GET access to Swagger assets/page and OpenAPI JSON/YAML |

[`application.properties`](src/main/resources/application.properties) contains shared settings; [`application-local.properties`](src/main/resources/application-local.properties) selects the Compose database. Without `local` or separately supplied datasource settings, a standalone application has no configured database. Tests provide their own connection.

Sessions are currently stored in the application process, so restarting signs users out. Multiple replicas require a shared session store or an explicitly designed routing strategy. For HTTPS deployment, configure secure session cookies, deployed frontend origins and external database credentials. The checked-in credentials and CORS origins are for local development.

## Troubleshooting

| Symptom | Action |
|---|---|
| Testcontainers cannot find Docker | Start Docker, enable Linux containers where applicable, and confirm `docker version` reaches the server |
| Database connection refused | Confirm `docker compose ps` is healthy, use the `local` profile and check host port `15432` |
| Database port already allocated | Free port `15432`, or change the Compose host port and the local datasource URL together |
| Application port 8080 already allocated | Stop the earlier application process; avoid running Maven and IntelliJ instances simultaneously |
| Flyway checksum mismatch | Compare the applied migration with Git history and restore unintended edits; put intended schema changes in a new migration |
| Swagger mutation returns `403` | Use `/docs/index.html`, keep the same host/session and check the browser network panel for the CSRF request |
| POST returns `403` | Obtain a fresh CSRF token using the same cookie jar; refresh it after login/logout and keep collection scripts enabled |
| `/me` returns `401` after login | Retain the updated session cookie, use the same host, and enable browser credentials or Postman's cookie jar |
| `registeredEmail` is unresolved | Run registration request 01 successfully, keep requests in the same collection, and select No environment |
| Duplicate registration returns `409` | Use login for that account or register a different email |
| Maven dependency not found after a failed download | Disable Maven offline mode, run `.\mvnw.cmd -U dependency:resolve`, then reload Maven in the IDE |
| Maven reports `PKIX path building failed` | Check the JDK used by Maven and its certificate/proxy configuration; fix trust rather than disabling TLS verification |

On macOS/Linux, substitute `./mvnw` in Maven commands. For startup failures, inspect the first meaningful `Caused by` message; a Maven completion banner alone does not prove the application is running.

## Remaining work

The core backend requirements are implemented. The remaining assignment work is the React catalog, authentication, cart and checkout/order-summary interface.

Further extensions include catalog administration, stock management and payments, cursor pagination, CI automation and shared session storage for multiple application instances.
