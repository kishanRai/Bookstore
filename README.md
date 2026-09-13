# Bookstore

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white)
![Build](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)

A Spring Boot REST API for a book catalog, built test-first (TDD) with a clean layered architecture (controller → service → repository → entity), Flyway-managed PostgreSQL schema, and a dual test strategy (fast MockMvc slice tests + real-database Testcontainers integration tests).

> This project was built as a machine-coding / system-design interview exercise. It intentionally starts small and correct rather than broad — see [Roadmap](#roadmap--possible-extensions) for what's deliberately left out.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [API Endpoints](#api-endpoints)
- [Postman Collection](#postman-collection)
- [Getting Started](#getting-started)
- [Testing Strategy](#testing-strategy)
- [Configuration](#configuration)
- [Roadmap / Possible Extensions](#roadmap--possible-extensions)

## Overview

Bookstore exposes a read API over a `books` catalog:

- Each book has a `title`, `author`, `price`, and `currency`.
- The schema is owned by **Flyway migrations**, not Hibernate auto-DDL (`ddl-auto=validate`), so the database is the source of truth and every change is versioned.
- Currency is currently constrained to `EUR` at the database level (a deliberate, narrow first slice — see [Roadmap](#roadmap--possible-extensions)).
- The service layer maps entities to a `BookResponse` DTO, keeping persistence details out of the API contract. Results are wrapped in `BookPageResponse` with bounded pagination and metadata.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 4.1.1 (Web MVC, Validation, Actuator, Data JPA, DevTools) |
| Database | PostgreSQL 17 |
| Schema Migrations | Flyway (`flyway-database-postgresql`) |
| Boilerplate reduction | Lombok |
| MVC Slice Testing | JUnit + Mockito (`@WebMvcTest`) |
| Integration Testing | Testcontainers (`@SpringBootTest` against a real containerized Postgres) |
| Build | Maven (via Maven Wrapper — no local Maven install required) |
| Local Infra | Docker Compose |

## Architecture

```
src/main/java/org/example/bookstore/
├── BookstoreApplication.java          # Spring Boot entry point
├── controllers/catalog/
│   └── BookController.java            # REST layer — /api/v1/books
├── services/catalog/
│   └── BookService.java               # Business logic, entity → DTO mapping
├── repositories/catalog/
│   └── BookRepository.java            # Spring Data JPA repository
├── entities/catalog/
│   └── Book.java                      # JPA entity, maps to the `books` table
└── dtos/catalog/
    ├── BookResponse.java              # One book
    └── BookPageResponse.java          # Books plus pagination metadata

src/main/resources/
├── application.properties             # Base config (used by tests via Testcontainers)
├── application-local.properties        # `local` profile — points at Docker Compose Postgres
└── db/migration/V1__create_books.sql  # Flyway migration defining the `books` table + constraints
```

Packages are grouped by technical layer, with a `catalog` subpackage in each layer. Additional domains can follow the same structure.

## API Endpoints

| Method | Path | Description | Success Response |
|---|---|---|---|
| `GET` | `/api/v1/books` | Returns a bounded page of books, ordered by `id` ascending | `200 OK` — JSON object containing `content` and pagination metadata |
| `GET` | `/actuator/health` | Application health (Spring Boot Actuator; details hidden) | `200 OK` when healthy — `{"status":"UP"}` |

### Catalog query parameters

```http
GET /api/v1/books?page=0&size=20
```

| Parameter | Type | Required | Default | Inclusive limits | Meaning |
|---|---|---|---|---|---|
| `page` | Integer | No | `0` | `0` to `10000` | Zero-based page number: `0` is the first page, `1` is the second |
| `size` | Integer | No | `20` | `1` to `100` | Maximum number of books returned in one page |

Omitted or empty parameters use their defaults. Results always use ascending `id` order; client-controlled sorting is not currently supported. Values outside the limits are rejected with `400 Bad Request`, rather than silently clamped. These bounds are declared on `BookController` using `@Min` and `@Max`.

Examples (use `curl.exe` in Windows PowerShell):

```powershell
# First page, default capacity of 20 books
curl.exe "http://localhost:8080/api/v1/books"

# Second page, at most two books
curl.exe "http://localhost:8080/api/v1/books?page=1&size=2"

# Largest allowed page capacity
curl.exe "http://localhost:8080/api/v1/books?page=0&size=100"
```

On macOS/Linux, use `curl` with the same quoted URLs.

### Successful response

For a catalog containing two books, `GET /api/v1/books?page=0&size=1` returns:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Harry Potter and the Philosopher's Stone",
      "author": "J.K. Rowling",
      "price": 45.50,
      "currency": "EUR"
    }
  ],
  "page": 0,
  "size": 1,
  "totalElements": 2,
  "totalPages": 2,
  "hasNext": true
}
```

The title, price, and ID above are illustrative; actual values come from the database.

| Field | JSON type | Meaning |
|---|---|---|
| `content` | Array of book objects | Books on this page; empty when no rows match the requested page |
| `page` | Number (integer) | Requested zero-based page number |
| `size` | Number (integer) | Requested page capacity, not the actual number of returned books |
| `totalElements` | Number (integer) | Total books in the catalog, across all pages; represented by a Java `long` |
| `totalPages` | Number (integer) | Number of pages for the requested size; `0` for an empty catalog |
| `hasNext` | Boolean | Whether another page exists after the requested page |

Each book contains `id` (integer), `title` (string), `author` (string), `price` (JSON number backed by Java `BigDecimal`), and `currency` (currently `"EUR"`). Clients should format prices for display; JSON numbers do not guarantee trailing zeroes.

The number of returned books is `content.length`, which can be smaller than `size`. To browse sequentially, keep the same `size` and increment `page` while `hasNext` is `true`, subject to the page-number cap.

**Contract change:** the endpoint now returns a page object instead of its earlier bare JSON array. Frontend and Postman consumers should read the books from `response.content`.

### Empty catalog and pages beyond the last result

An empty catalog with default parameters returns `200 OK`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false
}
```

A valid page number beyond the available results also returns `200 OK`, not `404`. For example, with two books, requesting `page=1&size=20` returns:

```json
{
  "content": [],
  "page": 1,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "hasNext": false
}
```

### Invalid requests

| Example query | Result | Reason |
|---|---|---|
| `?page=-1` | `400 Bad Request` | Page cannot be negative |
| `?page=10001` | `400 Bad Request` | Exceeds the page-number cap |
| `?size=0` or `?size=-1` | `400 Bad Request` | Size must be positive |
| `?size=101` | `400 Bad Request` | Exceeds the maximum page capacity |
| `?page=abc` or `?size=1.5` | `400 Bad Request` | Parameters must be integers |
| `?page=2147483648` | `400 Bad Request` | Value cannot be represented by the controller's Java `int` parameter |

Invalid requests are rejected before invoking `BookService`. A custom error-body schema is not defined yet; clients should rely on the HTTP status rather than an assumed error JSON structure.

### Pagination implementation and limits

`BookService` passes a `PageRequest` with ascending ID order to `BookRepository.findAll(...)`. The database limits the selected rows; the application does not load the entire catalog and then slice a Java list. The result is mapped to our `BookPageResponse` DTO.

This bounds the number of books loaded and serialized per request. It does not guarantee constant query time: offset queries can become expensive on deep pages, and obtaining totals can require a count query. The page cap is an application policy, not a Spring limitation. With `size=100`, page `10000` begins at offset `1000000`; requests for a higher page remain invalid even if more books exist. `hasNext` reflects the data and does not override this cap.

Ascending ID order is deterministic for an unchanged catalog. Separate page requests do not share a database snapshot, so concurrent inserts or deletions can change totals or shift results. Cursor pagination is a possible future improvement for larger catalogs.

## Postman Collection

A ready-to-import Postman collection and environment are included in [`postman/`](postman/):

- [`Bookstore.postman_collection.json`](postman/Bookstore.postman_collection.json) — requests for every endpoint above, with saved example responses
- [`Bookstore.postman_environment.json`](postman/Bookstore.postman_environment.json) — a `baseUrl` variable defaulting to `http://localhost:8080`

**To import:**

1. Open Postman → **Import** → **Files**.
2. Select both `postman/Bookstore.postman_collection.json` and `postman/Bookstore.postman_environment.json`.
3. Select the **Bookstore - Local** environment (top-right environment dropdown) so `{{baseUrl}}` resolves.
4. Start the app (see [Getting Started](#getting-started)), then run **Catalog → Get All Books** or **Ops → Health Check**. The existing catalog request name is historical: the endpoint now returns one page. Add `page` and `size` in the Params tab; refer to [API Endpoints](#api-endpoints) for the current response contract. Older saved responses in the collection may still show the previous array format.

<details>
<summary><strong>Or paste the raw collection JSON</strong> (Postman → Import → Raw Text)</summary>

```json
{
	"info": {
		"_postman_id": "8f2b6a3e-1c4d-4b8a-9e2f-6d0a3c7b5e9a",
		"name": "Bookstore API",
		"description": "Spring Boot Bookstore catalog service — book listing and health check endpoints.",
		"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
	},
	"item": [
		{
			"name": "Catalog",
			"item": [
				{
					"name": "Get All Books",
					"request": {
						"method": "GET",
						"header": [
							{ "key": "Accept", "value": "application/json" }
						],
						"url": {
							"raw": "{{baseUrl}}/api/v1/books",
							"host": ["{{baseUrl}}"],
							"path": ["api", "v1", "books"]
						}
					}
				}
			]
		},
		{
			"name": "Ops",
			"item": [
				{
					"name": "Health Check",
					"request": {
						"method": "GET",
						"header": [
							{ "key": "Accept", "value": "application/json" }
						],
						"url": {
							"raw": "{{baseUrl}}/actuator/health",
							"host": ["{{baseUrl}}"],
							"path": ["actuator", "health"]
						}
					}
				}
			]
		}
	],
	"variable": [
		{ "key": "baseUrl", "value": "http://localhost:8080", "type": "string" }
	]
}
```

*(This is a trimmed copy for quick pasting — the full file with saved example responses lives at [`postman/Bookstore.postman_collection.json`](postman/Bookstore.postman_collection.json).)*

</details>

## Getting Started

### Prerequisites

- Java 17
- Docker Desktop (for PostgreSQL — also required for running the integration test suite, via Testcontainers)
- No local Maven install needed — the Maven Wrapper (`mvnw` / `mvnw.cmd`) is bundled

### 1. Clone

```bash
git clone <repository-url>
cd Bookstore
```

### 2. Run the test suite

Testcontainers automatically starts and tears down a disposable Postgres container for the integration tests — no manual database setup needed to run `verify`:

```bash
# macOS/Linux
./mvnw clean verify

# Windows
.\mvnw.cmd clean verify
```

Run a single test class:

```bash
.\mvnw.cmd "-Dtest=BookControllerTest" test
.\mvnw.cmd "-Dtest=BookCatalogIntegrationTest" test
```

### 3. Run the app locally against Docker Postgres

Start the database (defined in `compose.yaml`, exposed on host port `15432`):

```bash
docker compose up -d --wait
docker compose ps
```

Run the app with the `local` profile, which points at that container and lets Flyway auto-migrate the schema on boot:

```bash
# macOS/Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Windows
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The app starts on **http://localhost:8080**.

### 4. Seed sample data

There is no write endpoint yet, so sample rows are inserted directly via `psql` inside the running container (`currency` must be `EUR` — enforced by a DB check constraint):

```bash
docker compose exec postgres psql -U bookstore -d bookstore -c \
  "INSERT INTO books (title, author, price, currency) VALUES ('Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', 45.50, 'EUR');"
```

### 5. Call the API

```bash
curl "http://localhost:8080/api/v1/books?page=0&size=20"
```

...or use the [Postman collection](#postman-collection) above.

## Testing Strategy

Two complementary layers, both TDD-driven:

- **`BookControllerTest`** (`@WebMvcTest` + Mockito `@MockitoBean`) — a fast, sliced MVC test that mocks `BookService` and asserts the controller's HTTP contract (status, content type, JSON body) using a focused Spring MVC context and no database. It covers default pagination, accepted boundary values, and invalid parameters rejected before the service is invoked.
- **`BookCatalogIntegrationTest`** (`@SpringBootTest` + `@AutoConfigureMockMvc`, backed by a real containerized Postgres via `PostgresTestConfiguration`) — exercises the full stack (controller → service → repository → real database), verifying empty-catalog behavior, correct `id`-ordered serialization, requested page contents and metadata, and empty content beyond the last page against actual rows. `@Sql` cleans the `books` table before and after each test for isolation.
- **`BookstoreApplicationTests`** — a plain context-load smoke test, also against a real Postgres container.

Together they give fast feedback on the HTTP layer and high confidence that the JPA mappings, Flyway schema, and database constraints actually work end-to-end.

## Configuration

| Profile | File | Purpose | Datasource |
|---|---|---|---|
| default | `application.properties` | Base config; used by tests | None declared — Testcontainers injects one via `@ServiceConnection` |
| `local` | `application-local.properties` | Running the app locally | `jdbc:postgresql://127.0.0.1:15432/bookstore` (Docker Compose) |

Key settings (`application.properties`):

| Property | Value | Meaning |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate only validates the schema — Flyway owns it |
| `spring.jpa.open-in-view` | `false` | No lazy-loading session held open through the view layer |
| `management.endpoints.web.exposure.include` | `health` | Only the health actuator endpoint is exposed |
| `management.endpoint.health.show-details` | `never` | Health responses don't leak internal details |

## Roadmap / Possible Extensions

Not implemented today — listed to show the intended direction, not as claims about current functionality:

- `POST /api/v1/books` (and `PUT`/`DELETE`) to manage the catalog via the API instead of `psql`
- Cursor pagination for larger catalogs and client-controlled sorting (bounded `page`/`size` pagination is implemented)
- Multi-currency support (the `chk_books_currency` constraint currently pins `EUR`)
- OpenAPI/Swagger UI for interactive API docs
- Global exception handling / `@ControllerAdvice` for consistent error responses
- CI pipeline running `mvnw clean verify` on push
