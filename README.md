[Swagger-UI link](http://localhost:8082/swagger-ui/index.html)

## Ports
| Service | Port | Role |
|---|---|---|
| `eureka` | 8761 | Service registry |
| `api_service` | 8080 | REST API for user CRUD |
| `db_service` | 8081 | Data layer (H2 in-memory) |
| `gateway` | 8082 | API gateway (routes `/api/**`) |

## Requirements

- Java 21
- Gradle (via the wrapper, included in each service directory)
- Docker + Docker Compose (for containerized run)

## Running with Docker Compose

All four services are containerized. Each service has its own multi-stage `Dockerfile` (build → layer extraction → minimal `eclipse-temurin:21-jre` runtime running as a non-root user), and `docker-compose.yml` at the repo root orchestrates them.

```bash
docker compose up -d --build
```

Services start in dependency order: `eureka` must be healthy before the other three start, so they can register with the service registry.

### Network topology

Compose defines two isolated networks:

| Network | Services | Purpose |
|---|---|---|
| `edge` | `gateway` | Sole entry point from the host |
| `internal` | `eureka`, `gateway`, `api_service`, `db_service` | Inter-service communication |

Access from the host is **only** possible through the gateway on port `8082`. Eureka, API, and DB services publish no host ports — they are reachable exclusively via the `internal` network, and all API traffic flows through the gateway (`/api/**` → `api_service` → `db_service` via Eureka + LoadBalancer).

Only the gateway binds a host port:

```bash
docker compose ps
```

```
NAME          STATUS                    PORTS
api-service   Up X seconds (healthy)   8080/tcp
db-service    Up X seconds (healthy)   8081/tcp
eureka        Up X seconds (healthy)   8761/tcp
gateway       Up X seconds (healthy)   0.0.0.0:8082->8082/tcp
```

## Running locally (without Docker)

Each service is a standalone Gradle project with its own `./gradlew`. Run every command from inside the service directory.

```bash
cd eureka
./gradlew bootRun
```

Start `eureka` first and wait until it is up, then start the other services in any order:

```bash
cd api_service && ./gradlew bootRun
cd db_service && ./gradlew bootRun
cd gateway   && ./gradlew bootRun
```

When running locally, services register with Eureka at `http://localhost:8761/eureka`. This is configurable via the `EUREKA_URI` environment variable (used by Docker Compose to point at the `eureka` container).

`db_service` uses an in-memory H2 database (`jdbc:h2:mem:db`) that is seeded with 100 users by Flyway on startup. Data does not survive a restart.

## API

All user endpoints are defined by `api_service` and load-balanced to `db_service` via service discovery. The gateway on port `8082` is the only host-exposed entry point; requests under `/api/**` are forwarded to `api_service` (e.g. `http://localhost:8082/api/users`).

### List users (paginated)

```bash
curl "http://localhost:8082/api/users"
```

Optional query parameters: `page` (default `0`), `size` (default `20`), `sort` (default `lastName,asc`, supports `lastName` and `dateOfBirth`), `lastName` (partial, case-insensitive), `dateOfBirth` (`yyyy-MM-dd`).

```bash
curl "http://localhost:8082/api/users?page=1&size=10&sort=dateOfBirth,desc&lastName=a"
```

### Get a user

```bash
curl "http://localhost:8082/api/users/1"
```

### Create a user

```bash
curl -X POST "http://localhost:8082/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "emailAdress": "john.doe@example.com",
    "dateOfBirth": "1990-05-15"
  }'
```

### Update a user

```bash
curl -X PUT "http://localhost:8082/api/users/1" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "emailAdress": "jane.doe@example.com",
    "dateOfBirth": "1992-08-21"
  }'
```

### Delete a user

```bash
curl -X DELETE "http://localhost:8082/api/users/1"
```

### Chaos (developer testing)

Available through the gateway for simulating latency and failures:

```bash
curl "http://localhost:8082/api/dev/chaos?delayMs=500&errorRate=0"
```

Parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `delayMs` | int | `5000` (5 s) | Simulated delay in milliseconds before responding |
| `errorRate` | double | `0.5` | Probability (in %) of simulating an error, in the range `0–100` |

## How to trigger rate limiting (gateway)
```bash
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/users/3
done
```
## Authentication (JWT) - fast setup with default JWT secret
All CRUD endpoints need a JWT to be included in the request headers. The simplest way to acquire a token is by running the following curl commands from the root directory of the project. This is a quick setup version, which uses the default jwt secret value. See below to see how to run it 

#### Generating a test token
```bash
cd api_service
TOKEN=$(JWT_SECRET=this-is-a-long-random-secret-of-around-32-bytes ./scripts/generate-test-token.sh)
echo "$TOKEN"
```
### Testing

#### no token -> 401
```bash
curl http://localhost:8082/api/users
```

#### with token -> 200
```bash
curl http://localhost:8082/api/users -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X POST http://localhost:8082/api/users -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe","emailAdress":"john.doe@example.com","dateOfBirth":"1990-05-15"}'
```

### Setup with custom JWT secret
Wither update the env file or in project root run:
```bash
JWT_SECRET="$MY_SECRET" docker compose up -d --build
```
Generate the JWT token for testing
```bash
cd api_service
TOKEN=$(JWT_SECRET="$MY_SECRET" ./scripts/generate-test-token.sh)
```
Test it as follows:
```bash
curl http://localhost:8082/api/users -H "Authorization: Bearer $TOKEN"
```
