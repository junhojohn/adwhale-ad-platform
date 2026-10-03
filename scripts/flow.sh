#!/usr/bin/env bash
# 광고 요청 → 노출 → 클릭 흐름을 N번 흉내낸다 (SDK가 하는 일을 curl로 대신)
# 사용법: ./scripts/flow.sh [반복횟수=1]
set -euo pipefail
API=${API:-http://localhost:8080}
N=${1:-1}

uuid() { uuidgen | tr 'A-Z' 'a-z'; }
now()  { python3 -c 'import time; print(int(time.time()*1000))'; }

for i in $(seq 1 "$N"); do
  DEVICE="device-$((RANDOM % 5))"

  # 1) 광고 요청
  RES=$(curl -sf -X POST "$API/v1/ad/request" -H 'Content-Type: application/json' \
    -d "{\"placementId\":\"P01\",\"appId\":\"APP01\",\"deviceId\":\"$DEVICE\"}")
  REQ_ID=$(echo "$RES" | python3 -c 'import sys,json; print(json.load(sys.stdin)["requestId"])')
  AD_ID=$(echo "$RES"  | python3 -c 'import sys,json; print(json.load(sys.stdin)["adId"])')
  echo "[$i] request  → requestId=$REQ_ID adId=$AD_ID"

  BODY() { echo "{\"eventId\":\"$(uuid)\",\"requestId\":\"$REQ_ID\",\"adId\":\"$AD_ID\",\"placementId\":\"P01\",\"deviceId\":\"$DEVICE\",\"clientTs\":$(now)}"; }

  # 2) 노출
  curl -sf -o /dev/null -X POST "$API/v1/ad/impression" -H 'Content-Type: application/json' -d "$(BODY)"
  echo "[$i] impression"

  # 3) 클릭 (노출의 30%만 클릭한다고 가정)
  if (( RANDOM % 10 < 3 )); then
    curl -sf -o /dev/null -X POST "$API/v1/ad/click" -H 'Content-Type: application/json' -d "$(BODY)"
    echo "[$i] click"
  fi
done
