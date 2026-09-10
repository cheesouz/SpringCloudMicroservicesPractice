# AGENTS.md

## Architecture

Four **independent** Spring Boot 4.1.1 services (no root multi-module Gradle build). Each directory is a standalone Gradle project with its own `gradlew`, `settings.gradle`, and `build.gradle`.

| Service | Port | Role | Key deps |
|---|---|---|---|
| `eureka` | 8761 | Service registry (Eureka Server) | eureka-server |
| `gateway` | 8082 | API gateway, routes via service discovery | spring-cloud-gateway, spring-security |
| `api_service` | 8080 | REST API (CRUD for users) | resilience4j, loadbalancer, springdoc-openapi |
| `db_service` | 8081 | Data layer (H2 in-memory) | spring-data-jpa, flyway, Lombok, H2 console |

**Startup order**: Start `eureka` first, wait until it's up, then start the other three in any order. All services register with Eureka.

## Running services

All commands run from the **service directory** (e.g., `cd api_service && ...`):

```bash
./gradlew bootRun        # run the service
./gradlew test           # run tests (JUnit 5)
./gradlew build          # full build + tests
```

There is no root-level wrapper; each service has its own `./gradlew`.

## Tech stack

- **Java 21**, Spring Boot 4.1.1, Spring Cloud 2025.1.3
- **Gradle 9.7.1** (wrapper included per service)
- Group: `com.cheesouz`
- Package: `com.cheesouz.<service_name>`

## Key gotchas

- **No multi-module Gradle setup** — never run `./gradlew` from the repo root; always `cd` into a service first.
- `db_service` uses **H2 in-memory DB** (`jdbc:h2:mem:dcbapp`) — data is lost on restart. No real database configured.
- `db_service` entity `User.java` has a broken import (`on.Generated` instead of `lombok.Generated`) — this will fail to compile as-is.
- `gateway` has **Spring Security** enabled — expect 401/403 without proper auth config.
- `api_service` depends on `db_service` via Eureka service discovery — `db_service` must be running for API calls to work.
- `gateway` routes must be configured (currently `application.properties` only sets the app name and port — no route definitions yet).
