#!/usr/bin/env bash
# 6-1단계 확인: 샘플 이벤트 파일을 로컬 S3(SeaweedFS)에 올리고 → Trino로 테이블 정의 → SQL로 조회
# 사용법: ./scripts/lake-test.sh   (docker compose up -d 로 seaweedfs, trino가 떠 있어야 함)
set -euo pipefail
cd "$(dirname "$0")/.."
DT=$(date +%F); HOUR=$(date +%H); NOW_MS=$(python3 -c 'import time; print(int(time.time()*1000))')

trino_sql() { docker exec -i adwhale-trino trino --output-format ALIGNED "$@"; }

echo "▶ 1) 샘플 파일 업로드 → s3://adwhale-lake/raw/events/event_type=*/dt=$DT/hour=$HOUR/"
# 진짜 AWS에서 쓰는 aws cli 그대로 (endpoint만 로컬 S3로). "s3 cp -" = 표준입력 내용을 파일로 업로드
aws_s3() {
  docker run --rm -i --network adwhale_default \
    -e AWS_ACCESS_KEY_ID=adwhale -e AWS_SECRET_ACCESS_KEY=adwhale-secret -e AWS_DEFAULT_REGION=us-east-1 \
    amazon/aws-cli --endpoint-url http://seaweedfs:8333 s3 "$@"
}
upload() {   # event_type, 내용
  echo "$2" | aws_s3 cp - "s3://adwhale-lake/raw/events/event_type=$1/dt=$DT/hour=$HOUR/sample.json"
}
upload request "$(cat <<JSON
{"eventId":"s-req-1","requestId":"s-r1","placementId":"P01","appId":"APP01","deviceId":"device-0","adId":"A001","serverTs":$NOW_MS}
{"eventId":"s-req-2","requestId":"s-r2","placementId":"P01","appId":"APP01","deviceId":"device-1","adId":null,"serverTs":$NOW_MS}
JSON
)"
upload impression "$(cat <<JSON
{"eventId":"s-imp-1","eventType":"impression","requestId":"s-r1","adId":"A001","placementId":"P01","deviceId":"device-0","clientTs":$NOW_MS,"serverTs":$NOW_MS}
{"eventId":"s-imp-1","eventType":"impression","requestId":"s-r1","adId":"A001","placementId":"P01","deviceId":"device-0","clientTs":$NOW_MS,"serverTs":$NOW_MS}
JSON
)"
upload click "$(cat <<JSON
{"eventId":"s-clk-1","eventType":"click","requestId":"s-r1","adId":"A001","placementId":"P01","deviceId":"device-0","clientTs":$NOW_MS,"serverTs":$NOW_MS}
JSON
)"

echo; echo "▶ 2) 스키마/테이블 정의 (trino/sql/01-lake-tables.sql)"
trino_sql < trino/sql/01-lake-tables.sql

echo; echo "▶ 3) 새 폴더(파티션)를 테이블에 등록"
trino_sql --execute "CALL lake.system.sync_partition_metadata('ads', 'raw_events', 'ADD')"

echo; echo "▶ 4) 조회: 파일을 SQL로"
trino_sql --execute "
SELECT event_type, dt, hour,
       count(*)                AS rows_in_files,
       count(DISTINCT eventid) AS unique_events,   -- 중복 줄(s-imp-1 두 번) 제거된 건수
       count_if(event_type = 'request' AND adid IS NULL) AS no_fill
FROM lake.ads.raw_events
WHERE dt = '$DT'
GROUP BY 1, 2, 3
ORDER BY 1"

echo; echo "▶ 5) 페더레이션: 레이크(파일)와 MySQL을 한 쿼리로"
trino_sql --execute "
SELECT 'lake(파일)' AS source, count(*) AS cnt FROM lake.ads.raw_events WHERE dt = '$DT'
UNION ALL
SELECT 'mysql.ad_event', count(*) FROM mysql.adwhale.ad_event"

echo; echo "▶ 6) 버킷 안 파일 목록"
aws_s3 ls --recursive s3://adwhale-lake/raw/

echo; echo "확인: 파일 보기 http://localhost:8888/buckets/adwhale-lake/ / Trino UI http://localhost:8085 (아무 이름으로 로그인)"
