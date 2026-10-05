#!/usr/bin/env bash
# 잘못된 메시지를 ad-impression 토픽에 직접 넣어서 DLQ 동작을 확인한다.
# (ad-api는 검증을 하므로 이런 메시지를 만들 수 없음 → console-producer로 직접 주입)
set -euo pipefail
TOPIC=${1:-ad-impression}

printf '%s\n' \
  'not-json-at-all' \
  '{"adId":"A001","placementId":"P01"}' \
  '[1,2,3]' \
| docker exec -i adwhale-kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 --topic "$TOPIC"

echo "잘못된 메시지 3건을 $TOPIC 에 보냈습니다. → $TOPIC-dlq 에 3건이 쌓여야 정상"
echo
echo "DLQ 내용 보기 (헤더 포함, Ctrl+C로 종료):"
echo "  docker exec -it adwhale-kafka /opt/kafka/bin/kafka-console-consumer.sh \\"
echo "    --bootstrap-server localhost:9092 --topic $TOPIC-dlq --from-beginning \\"
echo "    --property print.headers=true"
