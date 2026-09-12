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
- The service layer maps entities to a `BookResponse` DTO, keeping persistence details out of the API contract.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 4.1.1 (Web MVC, Validation, Actuator, Data JPA, DevTools) |
| Database | PostgreSQL 17 |
| Schema Migrations | Flyway (`flyway-database-postgresql`) |
| Boilerplate reduction | Lombok |
| Unit Testing | JUnit 5 + Mockito (`@WebMvcTest`) |
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
    └── BookResponse.java              # API response record

src/main/resources/
├── application.properties             # Base config (used by tests via Testcontainers)
├── application-local.properties        # `local` profile — points at Docker Compose Postgres
└── db/migration/V1__create_books.sql  # Flyway migration defining the `books` table + constraints
```

Package-by-feature (`catalog`) is used from the start so additional domains (e.g. orders, customers) can be added as siblings without restructuring.

## API Endpoints

| Method | Path | Description | Success Response |
|---|---|---|---|
| `GET` | `/api/v1/books` | Returns all books in the catalog, ordered by `id` ascending | `200 OK` — JSON array of books |
| `GET` | `/actuator/health` | Liveness/readiness health check (Spring Boot Actuator; details hidden) | `200 OK` — `{"status":"UP"}` |

**Example — `GET /api/v1/books`**

```json
[
    {
        "id": 1,
        "title": "Harry Potter and the Philosopher's Stone",
        "author": "J.K. Rowling",
        "price": 45.50,
        "currency": "EUR"
    },
    {
        "id": 2,
        "title": "Harry Potter and the Chamber of Secrets",
        "author": "J.K. Rowling",
        "price": 39.90,
        "currency": "EUR"
    }
]
```

An empty catalog returns `200 OK` with `[]` (never a `404`).

## Postman Collection

A ready-to-import Postman collection and environment are included in [`postman/`](postman/):

- [`Bookstore.postman_collection.json`](postman/Bookstore.postman_collection.json) — requests for every endpoint above, with saved example responses
- [`Bookstore.postman_environment.json`](postman/Bookstore.postman_environment.json) — a `baseUrl` variable defaulting to `http://localhost:8080`

**To import:**

1. Open Postman → **Import** → **Files**.
2. Select both `postman/Bookstore.postman_collection.json` and `postman/Bookstore.postman_environment.json`.
3. Select the **Bookstore - Local** environment (top-right environment dropdown) so `{{baseUrl}}` resolves.
4. Start the app (see [Getting Started](#getting-started)), then run **Catalog → Get All Books** or **Ops → Health Check**.

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
curl http://localhost:8080/api/v1/books
```

...or use the [Postman collection](#postman-collection) above.

## Testing Strategy

Two complementary layers, both TDD-driven:

- **`BookControllerTest`** (`@WebMvcTest` + Mockito `@MockitoBean`) — a fast, sliced MVC test that mocks `BookService` and asserts the controller's HTTP contract (status, content type, JSON body) in isolation, with no Spring context startup cost or database involved.
- **`BookCatalogIntegrationTest`** (`@SpringBootTest` + `@AutoConfigureMockMvc`, backed by a real containerized Postgres via `PostgresTestConfiguration`) — exercises the full stack (controller → service → repository → real database), verifying empty-catalog behavior and correct `id`-ordered serialization against actual rows. `@Sql` cleans the `books` table before and after each test for isolation.
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
- Pagination/sorting query params on `GET /api/v1/books`
- Multi-currency support (the `chk_books_currency` constraint currently pins `EUR`)
- OpenAPI/Swagger UI for interactive API docs
- Global exception handling / `@ControllerAdvice` for consistent error responses
- CI pipeline running `mvnw clean verify` on push
