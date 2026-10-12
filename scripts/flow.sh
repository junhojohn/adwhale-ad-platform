#!/usr/bin/env bash
# 광고 요청 → 노출 → 클릭 흐름을 N번 흉내낸다 (SDK가 하는 일을 curl로 대신)
# 사용법: ./scripts/flow.sh [반복횟수=1]
#
#   요청: 지면 P01 / P02 중 랜덤
#   응답: 200(광고 있음) 또는 204(노필 → 노출·클릭 없음)
#   노출: 광고를 받은 것 중 90%만 (화면을 바로 떠나 노출이 안 되는 경우 흉내)
#   클릭: 노출의 30%
set -euo pipefail
API=${API:-http://localhost:8080}
N=${1:-1}

uuid() { uuidgen | tr 'A-Z' 'a-z'; }
now()  { python3 -c 'import time; print(int(time.time()*1000))'; }
json() { python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"; }

for i in $(seq 1 "$N"); do
  DEVICE="device-$((RANDOM % 5))"
  PLACEMENT="P0$((RANDOM % 2 + 1))"

  # 1) 광고 요청 — 마지막 줄에 HTTP 상태코드를 붙여서 받음
  OUT=$(curl -s -w '\n%{http_code}' -X POST "$API/v1/ad/request" -H 'Content-Type: application/json' \
    -d "{\"placementId\":\"$PLACEMENT\",\"appId\":\"APP01\",\"deviceId\":\"$DEVICE\"}")
  CODE=$(echo "$OUT" | tail -n 1)
  RES=$(echo "$OUT" | sed '$d')

  if [[ "$CODE" == "204" ]]; then
    echo "[$i] request  → $PLACEMENT 노필(204)"
    continue
  elif [[ "$CODE" != "200" ]]; then
    echo "[$i] request  → 실패 HTTP $CODE $RES" >&2
    exit 1
  fi

  REQ_ID=$(echo "$RES" | json requestId)
  AD_ID=$(echo "$RES" | json adId)
  echo "[$i] request  → $PLACEMENT requestId=$REQ_ID adId=$AD_ID"

  BODY() { echo "{\"eventId\":\"$(uuid)\",\"requestId\":\"$REQ_ID\",\"adId\":\"$AD_ID\",\"placementId\":\"$PLACEMENT\",\"deviceId\":\"$DEVICE\",\"clientTs\":$(now)}"; }

  # 2) 노출 (90%)
  if (( RANDOM % 10 < 9 )); then
    curl -sf -o /dev/null -X POST "$API/v1/ad/impression" -H 'Content-Type: application/json' -d "$(BODY)"
    echo "[$i] impression"

    # 3) 클릭 (노출의 30%)
    if (( RANDOM % 10 < 3 )); then
      curl -sf -o /dev/null -X POST "$API/v1/ad/click" -H 'Content-Type: application/json' -d "$(BODY)"
      echo "[$i] click"
    fi
  fi
done
