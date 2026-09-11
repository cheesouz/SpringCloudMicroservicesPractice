| Service | Port | Role |
|---|---|---|
| `eureka` | 8761 | Service registry |
| `api_service` | 8080 | REST API gateway for user CRUD |
| `db_service` | 8081 | Data layer (H2 in-memory) |
| `gateway` | 8082 | API gateway (routes not configured yet) |

## Requirements

- Java 21
- Gradle (via the wrapper, included in each service directory)

## Running the project

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

`db_service` uses an in-memory H2 database (`jdbc:h2:mem:db`) that is seeded with 100 users by Flyway on startup. Data does not survive a restart.

## Tests

```bash
cd api_service && ./gradlew test
cd db_service  && ./gradlew test
```

## API

All user endpoints are exposed through `api_service` on port 8080 and load-balanced to `db_service` via service discovery. The same endpoints are also reachable through the `gateway` on port 8082 under the `/api/**` route (e.g. `http://localhost:8082/api/users`).

### List users (paginated)

```bash
curl "http://localhost:8080/api/users"
```

Optional query parameters: `page` (default `0`), `size` (default `20`), `sort` (default `lastName,asc`, supports `lastName` and `dateOfBirth`), `lastName` (partial, case-insensitive), `dateOfBirth` (`yyyy-MM-dd`).

```bash
curl "http://localhost:8080/api/users?page=1&size=10&sort=dateOfBirth,desc&lastName=a"
```

### Get a user

```bash
curl "http://localhost:8080/api/users/1"
```

### Create a user

```bash
curl -X POST "http://localhost:8080/api/users" \
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
curl -X PUT "http://localhost:8080/api/users/1" \
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
curl -X DELETE "http://localhost:8080/api/users/1"
```