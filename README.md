# API Store Management Tool

Backend API for an online game store. Buyers can browse and purchase games; managers can maintain
the catalog and stock, inspect inventory value, and review completed-purchase statistics.

## Stack

- Java 21 and Maven Wrapper
- Spring Boot 4, Spring MVC, Spring Security, JWT, Validation, JPA, and Actuator
- MySQL 8.4 with Flyway migrations
- springdoc-openapi 3 with Swagger UI
- JUnit 5, Mockito, MockMvc, H2 test slices, and a real-MySQL smoke test

## Prerequisites

- JDK 21 or newer
- Docker Desktop or another Docker Compose installation
- Git

The Maven installation bundled with an IDE is optional because the repository includes `mvnw` and
`mvnw.cmd`.

## Local setup

1. Open a terminal in `game-store-api`.
2. Create the local environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

   On macOS or Linux, use `cp .env.example .env`.

3. Replace every placeholder in `.env`. `JWT_SECRET_BASE64` must decode to at least 32 bytes.
4. Start MySQL and wait for it to become healthy:

   ```powershell
   docker compose up -d db
   docker compose ps
   ```

5. Start the API:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   On macOS or Linux, run `./mvnw spring-boot:run`.

The service listens on `http://localhost:8080`. Flyway creates the schema and the configured manager
account is bootstrapped idempotently during startup.

## OpenAPI documentation

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

Use the Swagger UI **Authorize** button with the JWT returned by the login endpoint. The generated
specification documents the BUYER and MANAGER operations, request validation, pagination, date
filters, and JWT bearer scheme.

## API examples

The examples below use Bash/Git Bash syntax and `curl`. PowerShell users can run the same requests
with `curl.exe` and replace `$TOKEN`, `$MANAGER_TOKEN`, and `$GAME_ID` manually.

Register and log in a buyer:

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: readme-register-1' \
  -d '{"email":"buyer@example.com","password":"safe-password-123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"buyer@example.com","password":"safe-password-123"}'
```

Copy `accessToken` from the login response:

```bash
TOKEN='<buyer-access-token>'
```

Search the catalog and purchase a game:

```bash
curl "http://localhost:8080/api/catalog/games?query=strategy&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

GAME_ID=1
curl -X POST http://localhost:8080/api/buyer/purchases \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"gameId\":$GAME_ID,\"quantity\":1}]}"

curl http://localhost:8080/api/buyer/purchases \
  -H "Authorization: Bearer $TOKEN"
```

Log in with `MANAGER_EMAIL` and `MANAGER_PASSWORD` from `.env`, then copy its `accessToken`:

```bash
MANAGER_TOKEN='<manager-access-token>'

curl -X POST http://localhost:8080/api/manager/games \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sku":"GAME-001","title":"Space Strategy","description":"Tactical campaign","price":29.99,"stockQuantity":10}'

curl -X PATCH "http://localhost:8080/api/manager/games/$GAME_ID/stock" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"delta":5}'

curl "http://localhost:8080/api/manager/inventory/summary?lowStockThreshold=5" \
  -H "Authorization: Bearer $MANAGER_TOKEN"

curl "http://localhost:8080/api/manager/statistics/purchases?from=2026-01-01&to=2026-12-31&topLimit=10" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

All responses contain `X-Correlation-ID`. A safe client value may contain up to 64 letters, digits,
`.`, `_`, or `-`; otherwise the API generates a UUID. Errors use `application/problem+json` with a
stable `code`, `correlationId`, `timestamp`, and field-level `errors` for validation failures.

## Logging profiles

Default logs contain one concise completion line per HTTP request and include the correlation ID.
Enable diagnostic logging only when needed:

```text
SPRING_PROFILES_ACTIVE=debug
```

The debug profile adds Spring Web, Spring Security, and Hibernate SQL diagnostics. Request bodies,
credentials, and SQL bind values remain disabled to avoid leaking sensitive data.

## Tests

Run the normal unit, repository, controller/security, integration, and application smoke tests:

```powershell
.\mvnw.cmd clean verify
```

The normal suite uses isolated H2 databases and does not require Docker. To run the separately gated
end-to-end journey against the MySQL container configured in `.env`:

```powershell
docker compose up -d db
.\mvnw.cmd -Pmysql-smoke verify
```

The MySQL smoke test starts the application on a random port and exercises health, OpenAPI, buyer
registration/login, manager login and game creation, catalog search, transactional checkout, purchase
history, inventory, and sales statistics over HTTP. It creates uniquely named test data and does not
delete existing records.

## Continuous integration

`.github/workflows/ci.yml` runs two jobs on pushes to `main` and pull requests:

1. Java 21 Maven Wrapper `clean verify` for the normal test suite.
2. The `mysql-smoke` Maven profile against a MySQL 8.4.10 service container.

The workflow grants read-only repository permissions and uses Maven dependency caching.
