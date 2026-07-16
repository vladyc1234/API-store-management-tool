# API Store Management Tool

Backend API for an online game store. Buyers can browse and purchase games; managers can maintain
the catalog and stock, inspect inventory value, and review completed-purchase statistics.

## Stack

- Java 21 and Maven Wrapper
- Spring Boot 4, Spring MVC, Spring Security, JWT, Validation, JPA, and Actuator
- MySQL 8.4 with Flyway migrations
- springdoc-openapi 3 with Swagger UI
- JUnit 5, Mockito, MockMvc, H2 test slices, and a real-MySQL smoke test

## Package architecture

Code is grouped by business feature under `com.gamestore.game_store_api`: `auth` owns registration,
login, and JWT issuing; `game` owns catalog and inventory; `purchase` owns checkout, history, and
statistics; `user` owns accounts and roles; `common` contains the sealed domain-error categories;
`config`, `bootstrap`, and `error` contain cross-cutting infrastructure. Controllers expose immutable
record DTOs and never return JPA entities.

## Prerequisites

- JDK 21 or newer
- Docker Desktop or another Docker Compose installation
- Git

The Maven installation bundled with an IDE is optional because the repository includes `mvnw` and
`mvnw.cmd`.

### IntelliJ IDEA

Open `game-store-api/pom.xml` as a Maven project, select JDK 23 as the Project SDK, and leave Maven
configured to use the project wrapper. The compiler emits Java 21 bytecode. Use the committed
`GameStoreApi` run configuration and activate `SPRING_PROFILES_ACTIVE=debug` only while diagnosing.

## Environment variables

| Variable | Purpose | Example |
|---|---|---|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/game_store` |
| `DB_USERNAME` | Application database user | `game_store` |
| `DB_PASSWORD` | Application database password | local secret |
| `JWT_SECRET_BASE64` | Base64 HS256 key; at least 32 decoded bytes | generated secret |
| `MANAGER_EMAIL` | Idempotently bootstrapped manager login | `manager@example.com` |
| `MANAGER_PASSWORD` | Manager password, 12–72 characters | strong local secret |

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

The `prod` profile disables both Swagger UI and the generated API document.

## API v1 endpoints

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a buyer |
| POST | `/api/v1/auth/login` | Public | Obtain a one-hour bearer JWT |
| GET | `/api/v1/games` | Buyer, Manager | Filter and page the catalog |
| GET | `/api/v1/games/{id}` | Buyer, Manager | Find an active game |
| POST | `/api/v1/manager/games` | Manager | Create a game |
| PATCH | `/api/v1/manager/games/{id}/price` | Manager | Replace a price |
| PATCH | `/api/v1/manager/games/{id}/stock` | Manager | Set absolute stock |
| DELETE | `/api/v1/manager/games/{id}` | Manager | Soft-deactivate a game |
| POST | `/api/v1/purchases` | Buyer | Buy one game |
| GET | `/api/v1/purchases/me` | Buyer | View personal purchase history |
| GET | `/api/v1/manager/inventory` | Manager | View stock and low-stock state |
| GET | `/api/v1/manager/statistics/purchases` | Manager | View EUR sales statistics |
| GET | `/actuator/health` | Public | Minimal readiness check |

Buyer registration passwords must be 6–72 characters long and contain at least one letter and one
special character. Authentication is exposed only under the versioned `/api/v1/auth` path.

## API examples

The examples below use Bash/Git Bash syntax and `curl`. PowerShell users can run the same requests
with `curl.exe` and replace `$TOKEN`, `$MANAGER_TOKEN`, and `$GAME_ID` manually.

Register and log in a buyer:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-ID: readme-register-1' \
  -d '{"email":"buyer@example.com","displayName":"Example Buyer","password":"safe-password-123"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"buyer@example.com","password":"safe-password-123"}'
```

Copy `accessToken` from the login response:

```bash
TOKEN='<buyer-access-token>'
```

Search the catalog and purchase a game:

```bash
curl "http://localhost:8080/api/v1/games?query=strategy&genre=Strategy&platform=PC&minimumPrice=10&maximumPrice=60&sort=price&direction=asc&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

GAME_ID=1
curl -X POST http://localhost:8080/api/v1/purchases \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"gameId\":$GAME_ID,\"quantity\":1}"

curl http://localhost:8080/api/v1/purchases/me \
  -H "Authorization: Bearer $TOKEN"
```

Log in with `MANAGER_EMAIL` and `MANAGER_PASSWORD` from `.env`, then copy its `accessToken`:

```bash
MANAGER_TOKEN='<manager-access-token>'

curl -X POST http://localhost:8080/api/v1/manager/games \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sku":"GAME-001","title":"Space Strategy","description":"Tactical campaign","genre":"Strategy","platform":"PC","price":29.99,"stockQuantity":10}'

curl -X PATCH "http://localhost:8080/api/v1/manager/games/$GAME_ID/stock" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"stockQuantity":15}'

curl "http://localhost:8080/api/v1/manager/inventory?lowStockThreshold=5" \
  -H "Authorization: Bearer $MANAGER_TOKEN"

curl "http://localhost:8080/api/v1/manager/statistics/purchases?from=2026-01-01&to=2026-12-31&topLimit=5&lowStockThreshold=5" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

All responses contain `X-Correlation-ID`. A safe client value may contain up to 64 letters, digits,
`.`, `_`, or `-`; otherwise the API generates a UUID. Errors use `application/problem+json` with a
stable `code`, `correlationId`, `timestamp`, and field-level `errors` for validation failures.

Example validation response:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request body fields are invalid",
  "instance": "/api/v1/manager/games",
  "code": "validation_failed",
  "timestamp": "2026-07-16T12:00:00Z",
  "correlationId": "client-request-123",
  "errors": [{"field": "price", "message": "must be greater than or equal to 0.01"}]
}
```

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

`.github/workflows/ci.yml` runs two jobs on every push and pull request:

1. Java 21 Maven Wrapper `clean verify` for the normal test suite.
2. The `mysql-smoke` Maven profile against a MySQL 8.4.10 service container.

The workflow grants read-only repository permissions and uses Maven dependency caching.

## V1 limitations

V1 has no frontend, cart, payment gateway, refunds, download/licensing service, multi-currency,
refresh tokens, token revocation, password reset, or self-service manager registration. EUR is the
only currency. CORS is intentionally not configured until a real frontend origin exists. Legacy
unversioned endpoints remain temporarily available for compatibility; new clients should use
`/api/v1` exclusively.
