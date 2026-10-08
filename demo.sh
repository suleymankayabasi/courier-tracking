#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
COURIER_ID="demo-courier-$(date +%s)"
STORE_LAT=40.9923307
STORE_LNG=29.1244229
METERS_PER_DEGREE_OF_LATITUDE=111194.93

north_of_store() {
  awk -v lat="$STORE_LAT" -v meters="$1" -v scale="$METERS_PER_DEGREE_OF_LATITUDE" \
    'BEGIN { printf "%.7f", lat + meters / scale }'
}

event_time() {
  printf '2026-01-01T10:%02d:%02dZ' $(($1 / 60)) $(($1 % 60))
}

pretty() {
  if command -v jq >/dev/null 2>&1; then jq .; else cat; echo; fi
}

send() {
  local seconds=$1 meters=$2 note=$3 time lat
  time=$(event_time "$seconds")
  lat=$(north_of_store "$meters")
  printf '  %s  %3s m from the store  %-44s HTTP ' "$time" "$meters" "$note"
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE_URL/api/v1/couriers/locations" \
    -H 'Content-Type: application/json' \
    -d "{\"time\":\"$time\",\"courierId\":\"$COURIER_ID\",\"lat\":$lat,\"lng\":$STORE_LNG}"
}

if ! curl -sf "$BASE_URL/actuator/health/readiness" >/dev/null; then
  echo "The application is not running at $BASE_URL (start it with ./mvnw spring-boot:run or docker compose up)." >&2
  exit 1
fi

echo "Courier $COURIER_ID around Ataşehir MMM Migros (radius 100 m, re-entry cooldown 60 s)"
echo
send   0 300 "approaching, outside"
send  20 150 "still outside"
send  40  50 "crosses into the radius: ENTRANCE #1"
send  50  20 "stays inside: no new entrance"
send  60 200 "leaves the store"
send  70  50 "back 30 s after #1: NOT counted"
send  90 200 "leaves again"
send 160  50 "back 2 min after #1: ENTRANCE #2"

echo
echo "Store entrances (expected: 10:00:40Z and 10:02:40Z):"
curl -s "$BASE_URL/api/v1/couriers/$COURIER_ID/store-entrances" | pretty
echo
echo "Total distance (expected: about 910 m = 150 + 100 + 30 + 180 + 150 + 150 + 150):"
curl -s "$BASE_URL/api/v1/couriers/$COURIER_ID/total-distance" | pretty
echo
echo "The application log shows each decision: search it (console, or docker compose logs) for $COURIER_ID"
