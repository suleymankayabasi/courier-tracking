#!/usr/bin/env bash
set -uo pipefail

BASE_URL="${1:-http://localhost:8080}"
BULK_COURIERS="${2:-20}"
RUN="$(date +%s)"
METERS_PER_DEGREE_OF_LATITUDE=111194.93
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

ATASEHIR_LAT=40.9923307; ATASEHIR_LNG=29.1244229
NOVADA_LAT=40.986106;    NOVADA_LNG=29.1161293
ORTAKOY_LAT=41.055783;   ORTAKOY_LNG=29.0210292

lat_north() { awk -v l="$1" -v m="$2" -v s="$METERS_PER_DEGREE_OF_LATITUDE" 'BEGIN { printf "%.7f", l + m / s }'; }
event_time() { printf '2026-01-01T10:%02d:%02dZ' $(($1 / 60 % 60)) $(($1 % 60)); }

post() {
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE_URL/api/v1/couriers/locations" \
    -H 'Content-Type: application/json' \
    -d "{\"time\":\"$(event_time "$2")\",\"courierId\":\"$1\",\"lat\":$(lat_north "$3" "$5"),\"lng\":$4}"
}

entrances() { curl -s "$BASE_URL/api/v1/couriers/$1/store-entrances"; }
distance()  { curl -s "$BASE_URL/api/v1/couriers/$1/total-distance" | grep -o '"totalDistanceMeters":[0-9.]*' | cut -d: -f2; }
count_of()  { grep -o '"entranceTime"' | wc -l | tr -d ' '; }

PASS=0; FAIL=0
check() {
  if [ "$2" = "$3" ]; then PASS=$((PASS + 1)); printf '  ok    %-52s %s\n' "$1" "$3"
  else FAIL=$((FAIL + 1)); printf '  FAIL  %-52s expected %s, got %s\n' "$1" "$2" "$3"; fi
}
check_near() {
  if awk -v e="$2" -v a="${3:-0}" -v t="$4" 'BEGIN { d = a - e; if (d < 0) d = -d; exit !(d <= e * t / 100) }'; then
    PASS=$((PASS + 1)); printf '  ok    %-52s %s m\n' "$1" "$3"
  else FAIL=$((FAIL + 1)); printf '  FAIL  %-52s expected ~%s m, got %s\n' "$1" "$2" "$3"; fi
}

if ! curl -sf "$BASE_URL/actuator/health/readiness" >/dev/null; then
  echo "The application is not running at $BASE_URL (./mvnw spring-boot:run or docker compose up)." >&2
  exit 1
fi


s_cooldown() {
  local c="cooldown-$RUN"
  for step in "0 300" "20 150" "40 50" "50 20" "60 200" "70 50" "90 200" "160 50"; do
    set -- $step; post "$c" "$1" $ATASEHIR_LAT $ATASEHIR_LNG "$2"; sleep 0.05
  done > "$OUT/cooldown.codes"
}

s_two_stores() {
  local c="twostores-$RUN"
  {
    post "$c" 0   $NOVADA_LAT   $NOVADA_LNG   400
    post "$c" 30  $NOVADA_LAT   $NOVADA_LNG   50
    post "$c" 60  $NOVADA_LAT   $NOVADA_LNG   300
    post "$c" 300 $ATASEHIR_LAT $ATASEHIR_LNG 300
    post "$c" 330 $ATASEHIR_LAT $ATASEHIR_LNG 40
  } > "$OUT/twostores.codes"
}

s_outside() {
  local c="outside-$RUN"
  for t in 0 20 40 60 80; do post "$c" "$t" $ORTAKOY_LAT $ORTAKOY_LNG 150; sleep 0.05; done > "$OUT/outside.codes"
}

s_out_of_order() {
  local c="late-$RUN"
  {
    post "$c" 0  $ORTAKOY_LAT $ORTAKOY_LNG 500
    post "$c" 60 $ORTAKOY_LAT $ORTAKOY_LNG 400
    post "$c" 30 $ORTAKOY_LAT $ORTAKOY_LNG 50
    post "$c" 90 $ORTAKOY_LAT $ORTAKOY_LNG 300
  } > "$OUT/late.codes"
}

s_batch() {
  local c="batch-$RUN" body=""
  for step in "100 300" "80 50" "60 300" "0 400"; do
    set -- $step
    body+="{\"time\":\"$(event_time "$1")\",\"courierId\":\"$c\",\"lat\":$(lat_north $NOVADA_LAT "$2"),\"lng\":$NOVADA_LNG},"
  done
  curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE_URL/api/v1/couriers/locations/batch" \
    -H 'Content-Type: application/json' -d "{\"locations\":[${body%,}]}" > "$OUT/batch.codes"
}

s_same_courier_race() {
  local c="race-$RUN" t
  for t in $(seq 0 15); do
    ( post "$c" $((t * 10)) $ATASEHIR_LAT $ATASEHIR_LNG $((t % 2 == 0 ? 40 : 300)); ) >> "$OUT/race.codes" &
  done
  wait
}

s_bulk() {
  local i
  for i in $(seq 1 "$BULK_COURIERS"); do
    (
      c="bulk-$RUN-$i"
      post "$c" 0  $ORTAKOY_LAT $ORTAKOY_LNG 400
      post "$c" 20 $ORTAKOY_LAT $ORTAKOY_LNG 60
      post "$c" 40 $ORTAKOY_LAT $ORTAKOY_LNG 400
    ) >> "$OUT/bulk.codes" &
  done
  wait
}

s_invalid() {
  local body code
  local url="$BASE_URL/api/v1/couriers/locations"
  for body in \
    '{"time":"2026-01-01T10:00:00Z","lat":40.99,"lng":29.12}' \
    '{"time":"2026-01-01T10:00:00Z","courierId":"x","lat":91,"lng":29.12}' \
    '{"time":"2026-01-01T10:00:00Z","courierId":"x","lat":40.99,"lng":181}' \
    '{"time":"2026-01-01T10:00:00Z","courierId":"bad id\nforged line","lat":40.99,"lng":29.12}' \
    '{"time":1767261600,"courierId":"x","lat":40.99,"lng":29.12}' \
    '{"courierId":"x","lat":40.99,"lng":29.12}' \
    'not json'; do
    curl -s -o /dev/null -w '%{http_code}\n' -X POST "$url" -H 'Content-Type: application/json' -d "$body"
  done > "$OUT/invalid.codes"
  curl -s -o /dev/null -w '%{http_code}\n' "$BASE_URL/api/v1/couriers/never-seen-$RUN/total-distance" > "$OUT/unknown.codes"
}

echo "Streaming to $BASE_URL: 7 scenario couriers + $BULK_COURIERS bulk couriers + invalid requests, all in parallel (run $RUN)"
START=$(date +%s)
s_cooldown & s_two_stores & s_outside & s_out_of_order & s_batch & s_same_courier_race & s_bulk & s_invalid &
wait
echo "Stream finished in $(( $(date +%s) - START )) s. Checking results (async listeners may need a moment)..."
sleep 2


echo; echo "HTTP codes"
all_codes() { sort "$OUT/$1.codes" | uniq -c | awk '{printf "%s x%s ", $2, $1}'; }
check "valid streams answer 202 only" "0" "$(cat "$OUT"/{cooldown,twostores,outside,late,batch,race,bulk}.codes | grep -vc '^202$')"
check "invalid payloads answer 400 (7 of 7)" "7" "$(grep -c '^400$' "$OUT/invalid.codes")"
check "unknown courier answers 404" "404" "$(tr -d '\n' < "$OUT/unknown.codes")"
echo "        cooldown: $(all_codes cooldown)| race: $(all_codes race)"

echo; echo "Entrances"
check "cooldown: 2 (re-entry after 30 s ignored)" "2" "$(entrances "cooldown-$RUN" | count_of)"
two=$(entrances "twostores-$RUN")
check "two stores: 2 entrances" "2" "$(echo "$two" | count_of)"
check "two stores: Novada then Ataşehir" "yes" "$(echo "$two" | tr -d '\n' | grep -q 'Novada.*Ataşehir' && echo yes || echo no)"
check "outside: 0 entrances (404 or empty = none)" "0" "$(entrances "outside-$RUN" | count_of)"
check "out-of-order: late event creates no entrance" "0" "$(entrances "late-$RUN" | count_of)"
check "batch in reverse order: 1 entrance" "1" "$(entrances "batch-$RUN" | count_of)"
bulk_ok=0
for i in $(seq 1 "$BULK_COURIERS"); do [ "$(entrances "bulk-$RUN-$i" | count_of)" = "1" ] && bulk_ok=$((bulk_ok + 1)); done
check "bulk: every courier has exactly 1 entrance" "$BULK_COURIERS" "$bulk_ok"
race_codes=$(grep -c '^5' "$OUT/race.codes")
check "same-courier race: no 5xx" "0" "$race_codes"
echo "        race entrances: $(entrances "race-$RUN" | count_of) (depends on arrival order, only reported)"

echo; echo "Distances"
check_near "cooldown: 150+100+30+180+150+150+150 = 910"  910 "$(distance "cooldown-$RUN")" 2
check_near "outside: 4 hops at 150 m ring, same side = 0"  0 "$(distance "outside-$RUN")" 2
check_near "out-of-order: 100 + 100 (late hop excluded)" 200 "$(distance "late-$RUN")" 2
check_near "batch (sorted): 100 + 250 + 250" 600 "$(distance "batch-$RUN")" 2

echo; echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
