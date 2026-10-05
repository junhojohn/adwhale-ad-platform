#!/usr/bin/env bash
# ad-api를 지정한 포트로 실행 (로드밸런서 뒤에 여러 대 띄우기용)
# 사용법: ./scripts/run-api.sh 8081   (다른 터미널에서) ./scripts/run-api.sh 8082
set -euo pipefail
PORT=${1:-8081}
cd "$(dirname "$0")/.."

./gradlew -q :ad-api:bootJar   # 코드가 바뀌었으면 다시 빌드 (안 바뀌었으면 바로 끝남)
exec java -jar ad-api/build/libs/ad-api-0.0.1-SNAPSHOT.jar --server.port="$PORT"
