#!/usr/bin/env bash
# 로드밸런서(8080)로 /ping을 N번 보내서 어느 ad-api가 응답했는지 출력
# 사용법: ./scripts/lb-test.sh [횟수=6]
N=${1:-6}
for i in $(seq 1 "$N"); do
  RES=$(curl -s -m 3 -D - localhost:8080/ping)
  BODY=$(echo "$RES" | tail -n 1)
  UPSTREAM=$(echo "$RES" | grep -i '^X-Upstream:' | tr -d '\r' | cut -d' ' -f2-)
  printf '[%d] %-22s upstream=%s\n' "$i" "$BODY" "$UPSTREAM"
  sleep 0.3
done
