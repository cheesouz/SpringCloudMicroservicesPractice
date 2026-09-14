**Zadanie rekrutacyjne - multiserwisy od Tecna (Piotr Ostrowski)**

[![English](https://img.shields.io/badge/lang-English-blue.svg)](README.md)

System czterech usług oparty o Spring Boot / Spring Cloud, zbudowany jako zadanie rekrutacyjne zlecone przez Piotra Ostrowskiego dla [Tecna](https://www.tecna.pl/). Składa się z rejestru usług Eureka, Spring Cloud Gateway pełniącego rolę jedynego punktu wejścia, serwisu API (REST) oraz serwisu magazynu opartego o bazę danych ze schematem i danymi testowymi zarządzanymi przez Flyway. Serwis API komunikuje się z serwisem magazynu wyłącznie poprzez Service Discovery (bez zaszytych na stałe adresów/portów), a sam jest dostępny wyłącznie przez gateway — rejestr usług i serwis magazynu nie mają bezpośredniego dostępu z hosta. Poza podstawowym CRUD ze stronicowaniem, sortowaniem i filtrowaniem, projekt pokazuje zagadnienia zbliżone do produkcyjnych: zabezpieczenie API przy pomocy JWT, odporność komunikacji (timeouty, retry, circuit breaker oraz endpoint testowy „chaos”), propagację korelacji żądań (correlationId) między usługami, rate limiting na poziomie gateway’a oraz w pełni skonteneryzowane, odizolowane sieciowo wdrożenie Docker Compose z healthcheckami i utwardzonymi Dockerfile’ami.

[Link do Swagger-UI](http://localhost:8082/swagger-ui/index.html)

## Porty
| Usługa | Port | Rola |
|---|---|---|
| `eureka` | 8761 | Rejestr usług |
| `api_service` | 8080 | REST API do operacji CRUD na użytkownikach |
| `db_service` | 8081 | Warstwa danych (H2 w pamięci) |
| `gateway` | 8082 | Gateway (kieruje żądania `/api/**`) |

## Wymagania

- Java 21
- Gradle (poprzez wrapper, dołączony do katalogu każdej usługi)
- Docker + Docker Compose (do uruchomienia w kontenerach)

## Uruchamianie za pomocą Docker Compose

Wszystkie cztery usługi są skonteneryzowane. Każda usługa ma własny wielostopniowy (multi-stage) `Dockerfile` (build → ekstrakcja warstw → minimalny obraz uruchomieniowy `eclipse-temurin:21-jre` działający jako użytkownik non-root), a `docker-compose.yml` w katalogu głównym repozytorium orkiestruje ich uruchomienie.

```bash
docker compose up -d --build
```

Usługi startują w określonej kolejności zależności: `eureka` musi osiągnąć stan „healthy”, zanim wystartują pozostałe trzy usługi, aby mogły zarejestrować się w rejestrze usług.

### Topologia sieci

Compose definiuje dwie odizolowane sieci:

| Sieć | Usługi | Cel |
|---|---|---|
| `edge` | `gateway` | Jedyny punkt wejścia z hosta |
| `internal` | `eureka`, `gateway`, `api_service`, `db_service` | Komunikacja między usługami |

Dostęp z hosta jest możliwy **wyłącznie** przez gateway na porcie `8082`. Rejestr usług, serwis API oraz serwis magazynu nie publikują żadnych portów na hosta — są dostępne wyłącznie w sieci `internal`, a cały ruch API przechodzi przez gateway (`/api/**` → `api_service` → `db_service` poprzez Eureka + LoadBalancer).

Tylko gateway publikuje port na hosta:

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

## Uruchamianie lokalne (bez Dockera)

Każda usługa jest samodzielnym projektem Gradle z własnym `./gradlew`. Wszystkie polecenia należy uruchamiać z katalogu danej usługi.

```bash
cd eureka
./gradlew bootRun
```

Najpierw uruchom `eureka` i poczekaj, aż będzie dostępna, następnie uruchom pozostałe usługi w dowolnej kolejności:

```bash
cd api_service && ./gradlew bootRun
cd db_service && ./gradlew bootRun
cd gateway   && ./gradlew bootRun
```

Podczas uruchamiania lokalnego usługi rejestrują się w Eurece pod adresem `http://localhost:8761/eureka`. Adres ten jest konfigurowalny przez zmienną środowiskową `EUREKA_URI` (używaną przez Docker Compose do wskazania na kontener `eureka`).

`db_service` korzysta z bazy danych H2 w pamięci (`jdbc:h2:mem:db`), która przy starcie jest zasilana przez Flyway 100 rekordami użytkowników testowych. Dane nie są zachowywane po restarcie.

## API

Wszystkie endpointy dotyczące użytkowników są zdefiniowane w `api_service` i kierowane (load-balanced) do `db_service` poprzez Service Discovery. Gateway na porcie `8082` jest jedynym punktem wejścia wystawionym na hosta; żądania pod `/api/**` są przekazywane do `api_service` (np. `http://localhost:8082/api/users`).

### Listowanie użytkowników (ze stronicowaniem)

```bash
curl "http://localhost:8082/api/users"
```

Opcjonalne parametry zapytania: `page` (domyślnie `0`), `size` (domyślnie `20`), `sort` (domyślnie `lastName,asc`, obsługuje `lastName` i `dateOfBirth`), `lastName` (dopasowanie częściowe, bez rozróżniania wielkości liter), `dateOfBirth` (`yyyy-MM-dd`).

```bash
curl "http://localhost:8082/api/users?page=1&size=10&sort=dateOfBirth,desc&lastName=a"
```

### Pobieranie użytkownika

```bash
curl "http://localhost:8082/api/users/1"
```

### Tworzenie użytkownika

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

### Aktualizacja użytkownika

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

### Usuwanie użytkownika

```bash
curl -X DELETE "http://localhost:8082/api/users/1"
```

### Chaos (testy deweloperskie)

Dostępny przez gateway, służy do symulowania opóźnień i błędów:

```bash
curl "http://localhost:8082/api/dev/chaos?delayMs=500&errorRate=0"
```

Parametry:

| Parametr | Typ | Domyślnie | Opis |
|---|---|---|---|
| `delayMs` | int | `5000` (5 s) | Symulowane opóźnienie w milisekundach przed odpowiedzią |
| `errorRate` | double | `0.5` | Prawdopodobieństwo (w %) symulacji błędu, w zakresie `0–100` |

## Odporność: testowanie retry i circuit breakera

### 1. Wywołanie otwarcia obwodu (CLOSED → OPEN)

Spraw, by `db_service` odpowiadał po 5 s (dłużej niż 2-sekundowy timeout) i zawsze kończył się błędem:

```bash
for i in $(seq 1 6); do
  curl -s -w " -> %{http_code}\n" "http://localhost:8082/api/dev/chaos?delayMs=5000&errorRate=100"
done
```

Oczekiwany rezultat:

- pierwsze wywołania zwracają **504** — `{"message":"Call to db-service timed out after 2000ms.",...}` — każde trwa ok. 6 s, ponieważ każda z 3 prób retry osiąga 2-sekundowy timeout;
- gdy tylko obwód (breaker) się otworzy, kolejne wywołania kończą się **szybkim** błędem **503** — `{"message":"Circuit breaker for db-service is OPEN; the service is temporarily unavailable.",...}` — w ok. 1 s zamiast czekania na 2-sekundowy timeout.

### 2. Potwierdzenie w logach

```bash
docker logs api-service | grep -E "\[RETRY\]|\[CB\]"
```

Poszukaj wpisów:

- `[RETRY] attempt 1 for dbService after: ...` oraz `[RETRY] attempt 2 ...`
- `[RETRY] dbService exhausted retries, giving up: ...`
- `[CB] dbService recorded a failure: ...`
- `[CB] dbService transitioned CLOSED -> OPEN`
- `[CB] dbService rejected call — breaker is OPEN`

### 3. Obserwacja powrotu do normy (OPEN → HALF_OPEN → CLOSED)

Odczekaj dłużej niż 5-sekundowe okno stanu OPEN, a następnie wznów normalny ruch:

```bash
sleep 6
for i in $(seq 1 3); do
  curl -s -o /dev/null -w "get user -> %{http_code}\n" \
    "http://localhost:8082/api/users/1" -H "Authorization: Bearer $TOKEN"
done
```

Pierwsze wywołanie przełącza breaker w stan HALF_OPEN; kolejne udane wywołania zamykają go (CLOSED). Potwierdź to poleceniem:

```bash
docker logs api-service | grep -E "\[CB\].*(OPEN|HALF_OPEN|CLOSED)"
```

Uwaga: odpowiedź 404 z `db_service` również liczy się jako błąd dla circuit breakera, więc seria odpowiedzi 404 może otworzyć obwód tak samo jak timeouty i błędy 5xx.

## Jak wywołać rate limiting (gateway)
```bash
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/users/3
done
```

## Correlation ID: jak testować

Każdemu żądaniu docierającemu do `api_service` przypisywany jest nagłówek `X-Correlation-ID`: jeśli przychodzący nagłówek jest obecny, jest on wykorzystywany, w przeciwnym razie generowany jest UUID. Wartość ta jest zwracana w odpowiedzi, przekazywana do `db_service` i logowana przez **obie** usługi.

### Z własnym nagłówkiem

```bash
curl -s -D - -o /dev/null "http://localhost:8082/api/users?size=1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-ID: demo-correlation-42"
```

Odpowiedź zawiera nagłówek `X-Correlation-ID: demo-correlation-42`, a obie usługi logują tę wartość:

```bash
docker logs api-service | grep -F "demo-correlation-42"
# np. [X-Correlation-ID] received correlation id: demo-correlation-42

docker logs db-service | grep -F "demo-correlation-42"
# np. [X-Correlation-ID] received correlation id: demo-correlation-42
```

### Bez nagłówka (serwis API generuje UUID)

Przechwyć identyfikator zwrócony w nagłówkach odpowiedzi i sprawdź logi obu usług:

```bash
CID=$(curl -s -D - -o /dev/null "http://localhost:8082/api/users?size=1" \
  -H "Authorization: Bearer $TOKEN" |
  awk -F': ' 'tolower($1)=="x-correlation-id" {gsub("\r","",$2); print $2}')

echo "correlation id: $CID"

docker logs api-service | grep -F "$CID"   # wygenerowany nowy correlation id
docker logs db-service  | grep -F "$CID"   # odebrany correlation id
```

Obie usługi logują ten sam UUID, co potwierdza, że identyfikator został wygenerowany raz, na brzegu systemu (edge), i przekazany dalej przez cały łańcuch żądania.

## Uwierzytelnianie (JWT) — szybka konfiguracja z domyślnym sekretem JWT
Wszystkie endpointy CRUD wymagają dołączenia tokena JWT w nagłówkach żądania. Najprostszym sposobem uzyskania tokena jest uruchomienie poniższych poleceń z katalogu głównego projektu. Jest to szybka wersja konfiguracji, wykorzystująca domyślną wartość sekretu JWT. Sposób uruchomienia opisano poniżej.

#### Generowanie testowego tokena
```bash
cd api_service
TOKEN=$(JWT_SECRET=this-is-a-long-random-secret-of-around-32-bytes ./scripts/generate-test-token.sh)
echo "$TOKEN"
```
### Testowanie

#### brak tokena -> 401
```bash
curl http://localhost:8082/api/users
```

#### z tokenem -> 200
```bash
curl http://localhost:8082/api/users -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X POST http://localhost:8082/api/users -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe","emailAdress":"john.doe@example.com","dateOfBirth":"1990-05-15"}'
```

### Konfiguracja z własnym sekretem JWT
Zaktualizuj plik env lub uruchom w katalogu głównym projektu:
```bash
JWT_SECRET="$MY_SECRET" docker compose up -d --build
```
Wygeneruj token JWT do testów:
```bash
cd api_service
TOKEN=$(JWT_SECRET="$MY_SECRET" ./scripts/generate-test-token.sh)
```
Przetestuj w następujący sposób:
```bash
curl http://localhost:8082/api/users -H "Authorization: Bearer $TOKEN"
```