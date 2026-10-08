# Courier Tracking

Spring Boot service that ingests courier geolocations, detects Migros store entrances and reports the total distance
travelled by each courier.

## Requirements

- Java 21
- Maven (the bundled `./mvnw` wrapper is enough)
- Optional: Docker (Docker image, docker-compose, `PostgresIntegrationTest`)

## Run

```bash
./mvnw spring-boot:run
```

The default profile is `demo`: in-memory H2, Swagger UI and DEBUG logging enabled.

| What | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |
| H2 console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:courier-tracking`, user `sa`, empty password) |

Other ways to run:

```bash
docker compose up --build            # Docker, see "Run with Docker" below

DB_USERNAME=... DB_PASSWORD=... \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres   # PostgreSQL on localhost:5432/courier_tracking
```

The schema is managed by Flyway (`src/main/resources/db/migration`).

### Run with Docker

Only Docker (with Compose) is needed; Java and Maven are not required on the host. The image builds the application
itself (tests are skipped in the image build, run them with `./mvnw test`).

```bash
docker compose up --build -d      # build and start in the background
docker compose ps                 # "healthy" once the application is ready (about 15-30 s)
docker compose logs -f            # follow the log: every counted entrance is logged
docker compose down               # stop and remove (in-memory data is gone)
```

The application is then reachable at http://localhost:8080 (Swagger UI at `/swagger-ui.html`), so all curl examples
and `./demo.sh` work unchanged. If port 8080 is already in use, pick another host port:

```bash
APP_PORT=8090 docker compose up --build -d
./demo.sh http://localhost:8090
```

Without Compose:

```bash
docker build -t courier-tracking .
docker run --rm -p 8080:8080 courier-tracking
```

The container runs as a non-root user with the default `demo` profile (in-memory H2, Swagger UI on). To use PostgreSQL
instead, pass `-e SPRING_PROFILES_ACTIVE=postgres -e DB_USERNAME=... -e DB_PASSWORD=...` and point
`spring.datasource.url` at your database (`-e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/courier_tracking`).

### Persistence

The default setup uses **H2 in memory, so all data (locations, courier state, entrances) is lost on restart.** Stores are
reloaded from `stores.json` at every start. Use the `postgres` profile for durable data.

## Test

```bash
./mvnw test
```

`PostgresIntegrationTest` uses Testcontainers and is skipped when Docker is not available.

## Demo scripts

With the application running:

```bash
./demo.sh          # one courier walks towards a store, leaves and comes back; prints status codes and results
./demo-stream.sh   # larger scenario, optional args: BASE_URL and number of bulk couriers
```

## API

Base path: `/api/v1`.

### `POST /couriers/locations` — ingest one location

```bash
curl -X POST http://localhost:8080/api/v1/couriers/locations \
  -H 'Content-Type: application/json' \
  -d '{"time":"2026-01-01T10:00:00Z","courierId":"courier-1","lat":40.9927807,"lng":29.1244229}'
```

Returns `202 Accepted` with an empty body.

| Field | Rule |
| --- | --- |
| `time` | required, ISO-8601 **with offset** (e.g. `2026-01-01T10:00:00Z` or `...+03:00`); normalised to UTC. Epoch numbers are rejected. Must not be more than 5 minutes ahead of the server clock |
| `courierId` | required, not blank, max 64 chars, only letters, digits, `.` `_` `:` `-` |
| `lat` | required number, -90 to 90 |
| `lng` | required number, -180 to 180 |

Invalid input (missing/null field, blank id, out-of-range or non-numeric value, `NaN`, malformed JSON, bad time format)
returns `400` with a common error body (a `time` too far in the future gives the same `400` without `fieldErrors`):

```json
{"timestamp":"...","status":400,"error":"Bad Request","message":"Request validation failed","path":"/api/v1/couriers/locations","fieldErrors":{"lat":"lat must be between -90 and 90"}}
```

### `POST /couriers/locations/batch` — ingest several locations

```bash
curl -X POST http://localhost:8080/api/v1/couriers/locations/batch \
  -H 'Content-Type: application/json' \
  -d '{"locations":[
        {"time":"2026-01-01T10:00:00Z","courierId":"courier-2","lat":40.9946,"lng":29.1244229},
        {"time":"2026-01-01T10:00:30Z","courierId":"courier-2","lat":40.9927807,"lng":29.1244229}]}'
```

1 to 1000 locations, possibly of several couriers. They are sorted by event time and processed one by one; the batch is
**not atomic** (a failing item does not roll back earlier ones). Returns `202`.

### `GET /couriers/{courierId}/total-distance`

```bash
curl http://localhost:8080/api/v1/couriers/courier-2/total-distance
# {"courierId":"courier-2","totalDistanceMeters":202.29693004420457}   (after the batch example above)
```

The unit is **meters**. `404` if no location of this courier has been accepted yet.

### `GET /couriers/{courierId}/store-entrances`

```bash
curl http://localhost:8080/api/v1/couriers/courier-2/store-entrances
# [{"storeId":1,"storeName":"Ataşehir MMM Migros","entranceTime":"2026-01-01T10:00:30Z"}]
```

Counted entrances, oldest first. `404` for an unknown courier; `200` with `[]` for a known courier without entrances.

### `GET /stores`

Lists the stores loaded from `stores.json`.

## How it works

```
Controller -> LocationService -> publishes CourierLocationEvent
                                   |-> StoreEntranceListener  -> EntranceRule chain -> StoreEntranceRepository + StoreEntranceEvent
                                   |-> TravelDistanceListener -> CourierState (total distance)
                                   StoreEntranceEvent -> StoreEntranceLogListener (SLF4J)
```

Everything for one location runs in one transaction, so the raw log, the courier state and the entrances commit together.

### Stores

`src/main/resources/stores.json` (`name`, `lat`, `lng`, UTF-8) is read at startup and upserted by name. The application
**fails to start** if the file is missing, malformed, empty, has a store without name or coordinates, an out-of-range
coordinate or a duplicate name. Location: `courier-tracking.stores-file`.

### Configuration (`application.yml`)

| Property | Default | Meaning |
| --- | --- | --- |
| `courier-tracking.entrance.radius-meters` | 100 | store radius |
| `courier-tracking.entrance.reentry-cooldown` | 60s | minimum time between two counted entrances of the same courier to the same store |
| `courier-tracking.distance.algorithm` | haversine | `haversine` or `equirectangular` |
| `courier-tracking.distance.min-movement-meters` | 0 | legs shorter than this are treated as GPS noise (not added to the distance) |
| `courier-tracking.location.max-future-skew` | 5m | how far a ping's `time` may be ahead of the server clock (tolerates clock drift between devices and server) |
| `courier-tracking.concurrency.max-retries` / `retry-delay` | 5 / 10ms | retries on concurrent updates of the same courier |

## Design patterns

- **Observer** — `LocationService` publishes a `CourierLocationEvent` and does not know who reacts. `StoreEntranceListener`
  and `TravelDistanceListener` are independent subscribers, and `StoreEntranceLogListener` subscribes to the
  `StoreEntranceEvent`. New reactions (e.g. notifications) can be added without touching the ingestion code.
- **Strategy** — `DistanceCalculator` with `HaversineDistanceCalculator` (default, accurate on a sphere) and
  `EquirectangularDistanceCalculator`; chosen by configuration in `DistanceCalculatorConfig`. Both the radius check and
  the travelled distance use the same strategy.
- **Chain of Responsibility** — an entrance is counted only if every `EntranceRule` agrees, in `@Order`:
  `WithinRadiusRule` → `OutsideToInsideTransitionRule` → `ReentryCooldownRule`. Each rule is small and tested alone.
- **Repository** (Spring Data) for persistence and Spring **singleton** beans for services and listeners.

## Decisions on ambiguous points

**Entrances**
- **Radius:** a point at exactly 100 m is *inside* (`<=`).
- **What counts as an entrance:** the courier's previous accepted position was outside the store (or there is none) and the
  current one is inside. A courier waiting inside produces one entrance, not one per minute. A courier whose first ping is
  already inside gets an entrance.
- **"Over 1 minute":** the cooldown is measured between the **event times** of the two entrances, never the server clock.
  Exactly 60 s later counts again (`>=`); 59.999 s does not.
- **GPS jitter:** an out-in re-entry inside the cooldown is not counted and does **not** restart the cooldown.
- Every store is checked independently: different couriers at the same store, or the same courier at different stores
  within a minute, all count; a point inside several radii counts each store.

**Time and ordering**
- **Time source:** the `time` sent by the client; the server clock is only used for error timestamps.
- A location **older than the courier's last accepted one** is saved in the raw log (`courier_locations`) but ignored for
  entrances and distance. A location with the **same timestamp** as the last one is in order and is processed, even with
  different coordinates.
- An **identical duplicate** event adds 0 m and cannot produce an entrance (the courier was already at that point), but
  it is stored again in the raw log.
- **Gaps:** after a long silence the distance is the straight line between the two pings; no gap detection.
- **Future timestamps are rejected** with `400` if `time` is more than `max-future-skew` (5 min) ahead of the server
  clock; otherwise a far-future ping would make every later, correct ping "out of order" for that courier. In a batch,
  one such item rejects the whole batch before anything is processed.
- **Old timestamps are accepted** (no maximum age): a courier's first ping may be arbitrarily old, which supports
  replaying history and the demo scripts. Later pings older than the courier's last accepted one are handled by the
  out-of-order rule above, so they cannot corrupt distance or entrances.

**Distance**
- Great-circle distance in **meters** (Haversine), summed leg by leg. The first ping gives 0; the same point again adds 0.
- **No speed filter:** unrealistic jumps (e.g. > 200 km/h) are not rejected. `min-movement-meters` is 0 by default.
- **Unknown courier** (no accepted location yet): `404` for both courier endpoints.

**Concurrency**
- Pings of one courier are serialised with an optimistic lock on `courier_states` (force-increment, version column) and
  retried; the same entrance can therefore not be written twice. If retries are exhausted the API answers `409 Conflict`.
  Different couriers run in parallel.

## Known limitations

- Entrances are detected from pings only. If pings are sparse (e.g. one every minute) a courier can cross the 100 m
  circle between two pings and the entrance is missed; there is no interpolation along the path.
- In-memory H2 by default: data is lost on restart.
- Raw location log grows without retention.
- No authentication.
