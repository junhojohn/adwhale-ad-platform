# adwhale-ad-platform

adwhale-sdk-kotlin 리뉴얼과 함께 만드는 광고 서버 (요청 / 노출 / 클릭).

## 목표 구조

```
SDK ──HTTP──▶ ad-api ──▶ Kafka (ad-request / ad-impression / ad-click)
                                  │
                                  ▼
                           ad-log-consumer ──▶ 디스크 로그 파일 ──▶ (이후) 저장소 ──▶ Presto
Dashboard ──▶ API 서버 ──▶ Redis(캐시) ──miss──▶ Presto
```

## 진행 단계

- [x] 1. Kafka 로컬 실행 + 토픽 3개 생성 (Docker Compose)
- [x] 2. Spring Boot 프로젝트 `ad-api` 생성 (Java 25 + Spring Boot 4.1 + Gradle KTS)
- [x] 3. Consumer: 토픽 메시지를 디스크 로그 파일로 저장 (`ad-api/logs/{토픽}/{토픽}-{날짜}.log`)
- [x] 4. API 서버: /v1/ad/request, /impression, /click → Kafka Producer
- [x] 4-1. Consumer 분리: `ad-log-consumer` 별도 앱 (ad-api는 Producer만)
- [x] 4-2. Gradle 멀티 모듈 + 공통 모듈 `ad-common`
- [ ] 5. Redis
- [ ] 6. 집계 / Presto / 대시보드

## 1단계: Kafka 실행

```bash
docker compose up -d
docker compose ps                     # kafka: healthy, kafka-init: exited (0) 이면 정상
docker compose logs kafka-init        # ad-click / ad-impression / ad-request 가 보이면 토픽 생성 완료
```

Kafka UI: http://localhost:8989

### CLI로 메시지 주고받아 보기

터미널 A (Consumer — 들어오는 메시지를 계속 출력):
```bash
docker exec -it adwhale-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic ad-impression --from-beginning
```

터미널 B (Producer — 한 줄 입력할 때마다 메시지 1건 전송):
```bash
docker exec -it adwhale-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic ad-impression
```
위 명령을 실행하면 `>` 프롬프트가 뜨고, 그 **안에서** 아래 JSON을 입력한다.
(`>` 는 프롬프트 표시라 직접 치지 않는다. 일반 쉘에서 `> {...}` 를 치면 쉘이 파일을 만들어버림)
```
{"adId":"A001","placementId":"P01","ts":1759450000000}
```

종료: `docker compose down` (데이터까지 지우려면 `docker compose down -v`)

## 2단계: ad-api 실행

```bash
./gradlew :ad-api:bootRun   # 루트에서 실행. 첫 실행은 Gradle 9.5 + 의존성(+필요하면 JDK 25) 다운로드로 몇 분 걸림
```

다른 터미널에서:
```bash
curl http://localhost:8080/ping              # pong
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

테스트: `./gradlew test` (전체 모듈)

## 3단계: Consumer → 로그 파일

1. `docker compose up -d` 로 Kafka 실행
2. `ad-api` 실행 (IntelliJ ▶ 또는 `./gradlew bootRun`)
   - 시작 로그에 `광고 로그 저장 경로: .../logs` 가 찍힘
   - `auto-offset-reset: earliest` 라서 1단계에서 보낸 테스트 메시지가 먼저 저장됨
3. console-producer로 메시지 전송 (1단계 명령 그대로, 토픽만 바꿔가며)
4. 파일 확인
```bash
ls -R ad-log-consumer/logs
tail -f ad-log-consumer/logs/ad-impression/ad-impression-*.log   # 실시간으로 쌓이는 것 보기
```

## 4단계: API → Kafka → 로그 파일

| API | 하는 일 | 응답 |
|---|---|---|
| `POST /v1/ad/request` | 광고 1개 응답 + `ad-request` 토픽에 기록 | 200 + 광고 정보(requestId 포함) |
| `POST /v1/ad/impression` | `ad-impression` 토픽에 기록 | 202 |
| `POST /v1/ad/click` | `ad-click` 토픽에 기록 | 202 |

`scripts/flow.sh` : 요청 → 노출 → 클릭을 한 번에 보내는 테스트 스크립트

```bash
./scripts/flow.sh          # 1회
./scripts/flow.sh 20       # 20회 반복
```

필수값 누락 테스트 (400 응답):
```bash
curl -i -X POST localhost:8080/v1/ad/impression -H 'Content-Type: application/json' -d '{}'
```

## 4-1단계: Consumer 분리

| 앱 | 역할 | 포트 |
|---|---|---|
| `ad-api` | HTTP 받아서 Kafka로 전송 (Producer) | 8080 |
| `ad-log-consumer` | Kafka에서 읽어서 로그 파일 저장 (Consumer) | 없음 |

IntelliJ에서 루트 폴더(adwhale-ad-platform)를 열면 두 앱이 한 창에서 보임. 또는 터미널 2개에서:
```bash
./gradlew :ad-api:bootRun
./gradlew :ad-log-consumer:bootRun
```
로그 파일 위치: `ad-log-consumer/logs/...`

### Consumer 2개 띄워서 파티션 나눠 받기
```bash
./gradlew :ad-log-consumer:bootJar
cd ad-log-consumer
java -jar build/libs/ad-log-consumer-0.0.1-SNAPSHOT.jar   # 터미널 A
java -jar build/libs/ad-log-consumer-0.0.1-SNAPSHOT.jar   # 터미널 B
```
- 두 번째가 뜨는 순간 리밸런싱 로그(`partitions assigned`)가 찍히고 파티션이 둘로 나뉨
- `../scripts/flow.sh 20` 실행 → 각 터미널에 서로 다른 partition 번호의 메시지만 찍힘
- 하나를 Ctrl+C로 끄면 남은 하나가 모든 파티션을 다시 가져감
- Kafka UI → Consumers → `ad-log-writer` 에서 멤버별 파티션 할당 확인

## 4-2단계: 멀티 모듈 구조

```
adwhale-ad-platform/          ← Gradle 루트 (wrapper, settings, 공통 설정)
├── ad-common/                ← 공통 코드 (순수 Java, Spring 의존성 없음)
│   ├── kafka/AdTopics            토픽 이름
│   └── event/AdRequestEvent      Kafka 메시지 형식
│       event/AdTrackingEvent
├── ad-api/                   ← implementation(project(":ad-common"))
└── ad-log-consumer/          ← implementation(project(":ad-common"))
```

| 명령 (루트에서) | 의미 |
|---|---|
| `./gradlew build` | 전체 모듈 빌드 + 테스트 |
| `./gradlew :ad-api:bootRun` | ad-api만 실행 |
| `./gradlew :ad-log-consumer:bootRun` | ad-log-consumer만 실행 |
| `./gradlew :ad-api:dependencies` | ad-api 의존성 트리 확인 |

