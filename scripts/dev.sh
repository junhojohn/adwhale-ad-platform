#!/usr/bin/env bash
# =====================================================================
# adwhale 로컬 개발 환경을 명령 하나로 켜고 / 끄고 / 테스트한다.
#
#   ./scripts/dev.sh up       인프라(Docker) + 앱 4개 전부 실행
#   ./scripts/dev.sh test     요청→노출→클릭 20회 보내고 DB·집계 결과 확인
#   ./scripts/dev.sh migrate  DB 스키마 마이그레이션(Flyway)만 실행
#   ./scripts/dev.sh status   지금 무엇이 떠 있는지
#   ./scripts/dev.sh logs X   앱 로그 보기 (X = ad-api-8081 | ad-api-8082 | ad-log-consumer | ad-batch | ad-dashboard-api)
#   ./scripts/dev.sh restart  코드 수정 후 앱만 다시 빌드·재시작
#   ./scripts/dev.sh down     앱 종료 + Docker 컨테이너 정지 (데이터는 유지)
#
# 앱 로그/PID는 .run/ 폴더에 저장된다 (git 제외).
# =====================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"
MYSQL_PW="${MYSQL_PASSWORD:-adwhale}"
APPS="ad-api-8081 ad-api-8082 ad-log-consumer ad-batch ad-dashboard-api"
mkdir -p "$RUN"
cd "$ROOT"

say()  { printf '\n\033[1m▶ %s\033[0m\n' "$*"; }
ok()   { printf '  \033[32m✔\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*"; }
fail() { printf '  \033[31m✘\033[0m %s\n' "$*"; }

# ── 공통 함수 ────────────────────────────────────────────────────────
check_docker() {
  if ! docker info >/dev/null 2>&1; then
    fail "Docker가 꺼져 있습니다. Docker Desktop을 켜고 메뉴바 고래 아이콘이 뜬 뒤 다시 실행하세요."
    exit 1
  fi
}

wait_healthy() {   # 컨테이너 이름, 최대 대기 초
  local name=$1 timeout=$2 i=0 state
  while (( i < timeout )); do
    state=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$name" 2>/dev/null || echo none)
    if [[ "$state" == "healthy" || "$state" == "running" ]]; then ok "$name: $state"; return 0; fi
    sleep 2; (( i += 2 ))
  done
  fail "$name: ${timeout}초 안에 준비되지 않음 (현재: $state) → docker compose logs ${name#adwhale-}"
  return 1
}

wait_http() {      # URL, 최대 대기 초
  local url=$1 timeout=$2 i=0
  while (( i < timeout )); do
    curl -sf -m 2 "$url" >/dev/null 2>&1 && return 0
    sleep 2; (( i += 2 ))
  done
  return 1
}

is_running() { [[ -f "$RUN/$1.pid" ]] && kill -0 "$(cat "$RUN/$1.pid")" 2>/dev/null; }

start_app() {      # 이름, 모듈, [추가 인자...]
  local name=$1 module=$2; shift 2
  if is_running "$name"; then warn "$name: 이미 실행 중"; return; fi
  # 모듈 폴더에서 실행 → ad-log-consumer의 로그 파일이 ad-log-consumer/logs 에 쌓임 (기존과 동일)
  ( cd "$ROOT/$module" && nohup "$JAVA" -jar "build/libs/$module-0.0.1-SNAPSHOT.jar" "$@" \
      > "$RUN/$name.log" 2>&1 & echo $! > "$RUN/$name.pid" )
  ok "$name 시작 (로그: .run/$name.log)"
}

stop_app() {
  local name=$1 pid i=0
  if ! is_running "$name"; then rm -f "$RUN/$name.pid"; return; fi
  pid=$(cat "$RUN/$name.pid"); kill "$pid" 2>/dev/null
  while kill -0 "$pid" 2>/dev/null && (( i < 15 )); do sleep 1; (( i++ )); done
  kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
  rm -f "$RUN/$name.pid"; ok "$name 종료"
}

build_apps() {
  say "앱 빌드 (변경 없으면 금방 끝남)"
  ./gradlew -q :ad-api:bootJar :ad-log-consumer:bootJar :ad-batch:bootJar :ad-db-migration:bootJar :ad-dashboard-api:bootJar || { fail "빌드 실패"; exit 1; }
  JAVA=$(./gradlew -q :ad-api:printJavaLauncher)
  ok "빌드 완료 / java: $JAVA"
}

run_migration() {   # 앱보다 먼저: 테이블이 준비돼 있어야 consumer/batch가 정상 동작
  say "DB 마이그레이션 (Flyway)"
  if ( cd "$ROOT/ad-db-migration" && "$JAVA" -jar build/libs/ad-db-migration-0.0.1-SNAPSHOT.jar ) > "$RUN/ad-db-migration.log" 2>&1; then
    grep -E "  V|현재 스키마 버전" "$RUN/ad-db-migration.log" | sed 's/.*: /  /'
    ok "마이그레이션 완료 (로그: .run/ad-db-migration.log)"
  else
    fail "마이그레이션 실패 → .run/ad-db-migration.log 확인"; tail -n 20 "$RUN/ad-db-migration.log"; exit 1
  fi
}

start_apps() {
  stop_apps        # 실행 중인 jar를 덮어쓰지 않도록 "먼저 끄고 → 빌드 → 실행"
  check_ports
  build_apps
  run_migration
  say "앱 실행"
  start_app ad-api-8081     ad-api          --server.port=8081
  start_app ad-api-8082     ad-api          --server.port=8082
  start_app ad-log-consumer ad-log-consumer
  start_app ad-batch        ad-batch        --adwhale.batch.interval=PT30S   # 테스트 편하게 30초 주기
  start_app ad-dashboard-api ad-dashboard-api

  say "ad-api 준비 대기"
  for port in 8081 8082; do
    if wait_http "localhost:$port/actuator/health" 60; then ok "ad-api :$port UP"
    else fail "ad-api :$port 응답 없음 → ./scripts/dev.sh logs ad-api-$port"; fi
  done
  if wait_http "localhost:8090/actuator/health" 60; then ok "ad-dashboard-api :8090 UP"
  else fail "ad-dashboard-api :8090 응답 없음 → ./scripts/dev.sh logs ad-dashboard-api"; fi
}

stop_apps() {
  say "앱 종료"
  for a in $APPS; do stop_app "$a"; done
  kill_strays
}

# dev.sh가 모르는 "떠돌이" 프로세스 정리 (run-api.sh / 직접 java -jar 로 띄운 것, 이전 실행에서 남은 것)
# → 이게 포트를 잡고 있으면 새 앱이 "Port already in use"로 못 뜨고,
#   jar를 다시 빌드하면 옛 프로세스가 사라진 클래스를 찾다가 빈 응답(ERR_EMPTY_RESPONSE)을 냄
kill_strays() {
  local pids
  pids=$(pgrep -f 'build/libs/ad-[a-z-]+-0\.0\.1-SNAPSHOT\.jar' || true)
  if [[ -n "$pids" ]]; then
    warn "이전에 띄운 앱 프로세스 정리: $(echo $pids)"
    kill $pids 2>/dev/null || true
    sleep 3
    pids=$(pgrep -f 'build/libs/ad-[a-z-]+-0\.0\.1-SNAPSHOT\.jar' || true)
    [[ -n "$pids" ]] && kill -9 $pids 2>/dev/null || true
  fi
}

# 포트를 다른 프로그램(IntelliJ에서 실행한 앱 등)이 쓰고 있으면 시작 전에 알려줌
check_ports() {
  local busy=0 port info
  for port in 8081 8082 8090; do
    info=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | awk 'NR==2 {print $1" (pid "$2")"}')
    if [[ -n "$info" ]]; then fail "포트 $port 사용 중: $info → 종료 후 다시 실행 (IntelliJ에서 띄운 앱이면 IntelliJ에서 Stop)"; busy=1; fi
  done
  (( busy == 0 )) || exit 1
}

# ── 명령 ─────────────────────────────────────────────────────────────
cmd_up() {
  check_docker
  say "인프라 실행 (Kafka, Kafka UI, MySQL, 로드밸런서)"
  docker compose up -d
  wait_healthy adwhale-kafka 90 || exit 1
  wait_healthy adwhale-mysql 90 || exit 1
  start_apps
  cmd_status
  say "다음: ./scripts/dev.sh test"
}

cmd_status() {
  say "Docker 컨테이너"
  docker compose ps --format 'table {{.Name}}\t{{.Status}}' 2>/dev/null || warn "Docker 꺼짐"
  say "앱"
  for a in $APPS; do
    if is_running "$a"; then ok "$a (pid $(cat "$RUN/$a.pid"))"; else fail "$a 꺼짐"; fi
  done
  say "로드밸런서 → ad-api"
  for i in 1 2; do
    curl -s -m 2 localhost:8080/ping && echo || fail "localhost:8080 응답 없음"
  done
  say "주소"
  echo "  API(로드밸런서) http://localhost:8080   대시보드 http://localhost:8090   Kafka UI http://localhost:8989   MySQL localhost:3306 (adwhale/$MYSQL_PW)"
}

cmd_test() {
  say "요청 → 노출 → 클릭 20회 (로드밸런서 경유)"
  ./scripts/flow.sh 20 | tail -n 3
  echo "  ..."

  say "5초 대기 후 DB 원본(ad_event) 건수"
  sleep 5
  docker exec -i adwhale-mysql mysql -uadwhale -p"$MYSQL_PW" -t adwhale 2>/dev/null \
    -e "SELECT event_type, COUNT(*) AS cnt, MAX(server_ts) AS last_ts FROM ad_event GROUP BY event_type;"

  say "집계 배치(30초 주기) 대기 35초 후 ad_stats_hourly"
  sleep 35
  docker exec -i adwhale-mysql mysql -uadwhale -p"$MYSQL_PW" -t adwhale 2>/dev/null -e "
    SELECT stat_hour, ad_id, placement_id, requests, impressions, clicks,
           ROUND(clicks / NULLIF(impressions, 0) * 100, 2) AS ctr_pct, updated_at
    FROM ad_stats_hourly ORDER BY stat_hour DESC LIMIT 5;"

  say "지면별 필레이트 (placement_stats_hourly)"
  docker exec -i adwhale-mysql mysql -uadwhale -p"$MYSQL_PW" -t adwhale 2>/dev/null -e "
    SELECT stat_hour, placement_id, requests, filled,
           ROUND(filled / NULLIF(requests, 0) * 100, 2) AS fill_rate_pct
    FROM placement_stats_hourly ORDER BY stat_hour DESC, placement_id LIMIT 4;"

  say "대시보드 API: 오늘 summary"
  curl -s "localhost:8090/v1/stats/summary?from=$(date +%F)&to=$(date +%F)" | python3 -m json.tool --no-ensure-ascii 2>/dev/null \
    || warn "ad-dashboard-api 응답 없음"
  echo "  전체 API 확인: ./scripts/dashboard-test.sh"
  echo "  대시보드 화면: http://localhost:8090"

  say "로그 파일 (ad-log-consumer/logs)"
  ls -1 ad-log-consumer/logs/*/ 2>/dev/null | tail -n 6
  echo
  echo "  더 보기: Kafka UI http://localhost:8989 → Consumers (ad-log-writer / ad-db-writer)"
}

cmd_logs() {
  local name=${1:-}
  if [[ -z "$name" || ! -f "$RUN/$name.log" ]]; then
    echo "사용법: ./scripts/dev.sh logs <ad-api-8081 | ad-api-8082 | ad-log-consumer | ad-batch>"; exit 1
  fi
  tail -n 50 -f "$RUN/$name.log"
}

cmd_restart() { check_docker; start_apps; }

cmd_down() {
  stop_apps
  say "Docker 컨테이너 정지 (데이터 유지. 완전 삭제는 docker compose down -v)"
  docker compose stop
}

case "${1:-}" in
  up)      cmd_up ;;
  test)    cmd_test ;;
  migrate) check_docker; build_apps; run_migration ;;
  status)  cmd_status ;;
  logs)    cmd_logs "${2:-}" ;;
  restart) cmd_restart ;;
  down)    cmd_down ;;
  *)       sed -n '3,12p' "$0" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
