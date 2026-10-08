#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
COURIER_ID="demo-$(date +%s)"
STORE_LAT=40.9923307
STORE_LNG=29.1244229

north_of_store() { awk -v lat="$STORE_LAT" -v m="$1" 'BEGIN { printf "%.7f", lat + m / 111194.93 }'; }

send() {
  local time="2026-01-01T10:$1Z" lat
  lat=$(north_of_store "$2")
  printf '  %s  %3s m from the store  %-36s HTTP ' "$time" "$2" "$3"
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE_URL/api/couriers/locations" \
    -H 'Content-Type: application/json' \
    -d "{\"time\":\"$time\",\"courierId\":\"$COURIER_ID\",\"lat\":$lat,\"lng\":$STORE_LNG}"
}

if ! curl -s -o /dev/null "$BASE_URL"; then
  echo "The application is not running at $BASE_URL (start it with ./mvnw spring-boot:run)." >&2
  exit 1
fi

echo "Courier $COURIER_ID around Ataşehir MMM Migros (radius 100 m, re-entry threshold 1 min)"
send 00:00 300 "outside"
send 00:20  50 "moves in: ENTRANCE #1"
send 00:40  20 "stays inside: no new entrance"
send 01:00 200 "leaves"
send 01:10  50 "back 50 s after #1: not counted"
send 01:30 200 "leaves again"
send 00:50 500 "late (out of order): ignored"
send 02:30  50 "back 2 min after #1: ENTRANCE #2"

echo
echo "Store entrances (expected 10:00:20Z and 10:02:30Z):"
curl -s "$BASE_URL/api/couriers/$COURIER_ID/store-entrances"; echo
echo "Total distance (expected about 250+30+180+150+150+150 = 910 m):"
curl -s "$BASE_URL/api/couriers/$COURIER_ID/total-distance"; echo
echo "Invalid location (lat 91):"
curl -s -X POST "$BASE_URL/api/couriers/locations" -H 'Content-Type: application/json' \
  -d "{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"$COURIER_ID\",\"lat\":91,\"lng\":29}"; echo
echo "Unknown courier:"
curl -s "$BASE_URL/api/couriers/nobody-$COURIER_ID/total-distance"; echo
