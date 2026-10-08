# Courier Tracking

Spring Boot REST service that takes streaming courier geolocations, logs every courier entrance to a Migros store
(100 m radius) and reports the total distance travelled by each courier.

## Run

Requires Java 21+. The Maven wrapper is included.

```bash
./mvnw spring-boot:run      # starts on http://localhost:8080
./demo.sh                   # in a second terminal: streams a sample courier and prints the results
./mvnw test                 # unit + integration tests
```

Swagger UI: http://localhost:8080/swagger-ui.html

## API

| Method | Path | Result |
| --- | --- | --- |
| `POST` | `/api/couriers/locations` | `200`, or `400` for an invalid location |
| `GET` | `/api/couriers/{courierId}/total-distance` | `{"courierId":"c1","totalDistanceMeters":909.96}`, `404` for an unknown courier |
| `GET` | `/api/couriers/{courierId}/store-entrances` | `[{"courierId":"c1","storeName":"Ataşehir MMM Migros","time":"..."}]`, `404` for an unknown courier |

```bash
curl -X POST http://localhost:8080/api/couriers/locations \
  -H 'Content-Type: application/json' \
  -d '{"time":"2026-01-01T10:00:00Z","courierId":"courier-1","lat":40.9923307,"lng":29.1244229}'
```

| Field | Rule |
| --- | --- |
| `time` | required, ISO-8601 with offset (`2026-01-01T10:00:00Z`), at most 1 minute ahead of the server clock |
| `courierId` | required, 1-64 letters, digits, `.`, `_`, `-` |
| `lat` / `lng` | required, -90..90 / -180..180 |

Errors use the RFC 9457 problem format; validation errors list the invalid fields under `errors`.

## Design

```
CourierController ──> CourierTrackingService ──> CourierRepository (in memory)
                              │                  StoreRepository   (stores.json)
                              │
                              ├─ DistanceCalculator ◄── HaversineDistanceCalculator      (Strategy)
                              └─ publishes StoreEntrance ──> StoreEntranceLogger          (Observer)
```

For each location `CourierTrackingService.recordLocation`:

1. rejects a time too far in the future,
2. ignores the location if it is older than the courier's last accepted one,
3. for every store, counts an entrance if the courier moved from outside to inside the 100 m radius and the last
   entrance of this courier to this store was at least 1 minute ago,
4. adds the distance from the previous location to the total.

### Design patterns

- **Strategy**: `DistanceCalculator` hides how distance is measured. The service uses it for both the store radius and
  the travel distance, so another formula (e.g. Vincenty) can be added without touching the service.
- **Observer**: a counted entrance is published as a Spring application event. `StoreEntranceLogger` logs it (the
  "log courier and store" requirement). Other reactions, such as notifications or metrics, can be added as new
  listeners without changing the tracking logic.

## Decisions and edge cases

**Store entrances**
- A point exactly 100 m from the store is inside.
- An entrance is the move from outside to inside. A courier waiting inside a store is one entrance, not one
  per location. A courier whose first location is already inside gets an entrance.
- "Reentries over 1 minute": a re-entry less than 1 minute after the last counted entrance to the same store is not
  counted. Exactly 1 minute later counts. The rule uses the location times, not the server clock.
- Stores and couriers are independent: the same courier entering two stores, or two couriers entering the same store,
  within a minute all count.

**Time and ordering**
- The time sent by the client is used. A location older than the courier's last accepted one is ignored (logged as a
  warning). Otherwise it would add a zig-zag to the distance or create a false entrance.
- A location with the same time as the last one is processed. An exact duplicate adds 0 m and no entrance.
- A time more than 1 minute in the future is rejected with `400`. Otherwise every later, correct location of that
  courier would be treated as out of order.

**Distance**
- Great-circle (Haversine) distance in meters, summed leg by leg. The first location gives 0.
- An unknown courier (no location yet) returns `404` instead of `0`, so a mistyped id is not mistaken for a courier who
  did not move.

**Concurrency**
- Locations of the same courier are processed one at a time (`synchronized` on the courier), so two parallel requests
  cannot double-count an entrance or lose a distance update. Different couriers are processed in parallel
  (`ConcurrentHashMap`).

**Input**
- Missing fields, out-of-range coordinates, non-numeric values, an invalid time and malformed JSON return `400`.
  `courierId` is restricted to safe characters because it appears in log lines.
- The application does not start if `stores.json` is missing, malformed or empty.

## Limitations

- Data is kept in memory and lost on restart. A database can replace `CourierRepository` without changing the service.
- Entrances are detected only at the reported points. If a courier crosses the radius between two sparse locations,
  the entrance is missed.
- GPS noise is not filtered: small jumps while standing still add to the distance.
