#!/usr/bin/env bash
# 대시보드 API 4개를 오늘 날짜 기준으로 호출해서 결과를 보기 좋게 출력
# 사용법: ./scripts/dashboard-test.sh [adId]
API=${DASHBOARD_API:-http://localhost:8090}
TODAY=$(date +%F)
WEEK_AGO=$(date -v-6d +%F 2>/dev/null || date -d '6 days ago' +%F)
AD=${1:-}
AD_PARAM=${AD:+&adId=$AD}

call() {
  echo; echo "▶ GET $1"
  curl -s "$API$1" | python3 -m json.tool --no-ensure-ascii 2>/dev/null || echo "  (응답 없음 — ad-dashboard-api 실행 중인지 확인)"
}

call "/v1/stats/filters"
call "/v1/stats/summary?from=$WEEK_AGO&to=$TODAY$AD_PARAM"
call "/v1/stats/timeseries?from=$TODAY&to=$TODAY&granularity=hour$AD_PARAM" | head -n 40
call "/v1/stats/breakdown?from=$WEEK_AGO&to=$TODAY&by=placement$AD_PARAM"
call "/v1/stats/breakdown?from=$WEEK_AGO&to=$TODAY&by=ad_placement$AD_PARAM"

echo; echo "▶ 에러 케이스 (400 이어야 정상)"
curl -s -o /dev/null -w "  from > to            → %{http_code}\n" "$API/v1/stats/summary?from=$TODAY&to=$WEEK_AGO"
curl -s -o /dev/null -w "  hour 로 7일 조회      → %{http_code}\n" "$API/v1/stats/timeseries?from=$WEEK_AGO&to=$TODAY&granularity=hour"
curl -s -o /dev/null -w "  날짜 형식 오류        → %{http_code}\n" "$API/v1/stats/summary?from=2026-13-01&to=$TODAY"
curl -s -o /dev/null -w "  by 값 오류            → %{http_code}\n" "$API/v1/stats/breakdown?from=$TODAY&to=$TODAY&by=device"
