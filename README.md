# API Store Management Tool

Spring Boot backend for an online game store. The application uses Java 21, Maven, MySQL, Flyway,
JWT authentication, and buyer/manager role-based access.

Run MySQL from `game-store-api` with `docker compose up -d db`, then start the API with
`mvnw.cmd spring-boot:run` on Windows or `./mvnw spring-boot:run` on macOS/Linux.

Normal logging is intentionally concise. Enable diagnostic logging only when needed by setting
`SPRING_PROFILES_ACTIVE=debug`. Debug logging includes Spring Web, Spring Security, and Hibernate SQL,
but never SQL bind values or request bodies.

Every HTTP response includes `X-Correlation-ID`. Clients may supply a value containing up to 64
letters, digits, `.`, `_`, or `-`; otherwise the API generates a UUID. Error responses use
`application/problem+json` and include a stable `code`, `correlationId`, `timestamp`, and field-level
`errors` when validation fails.
