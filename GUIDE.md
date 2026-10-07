# adwhale-ad-platform 복귀 가이드

> 잠시 SDK 작업으로 넘어갔다가 돌아왔을 때 **이 파일 하나만 보고** 다시 실행·테스트할 수 있도록 정리한 문서.
> 마지막 작업: **5-3단계 (집계 배치 `ad-batch`)** — 코드 작성 완료, **아직 실행 검증 전**.

---

## 0. 돌아오면 제일 먼저 (3분)

```bash
cd ~/workspace/01.repository/WEB/adwhale-ad-platform
git status                 # 커밋 안 된 변경이 있는지
```

1. **Docker Desktop 켜기** (메뉴바 고래 아이콘이 뜰 때까지)
2. 아래 3개 명령이면 끝:

```bash
./scripts/dev.sh up        # 인프라 + 앱 4개 전부 실행 (1~2분)
./scripts/dev.sh test      # 요청→노출→클릭 20회 → DB·집계 결과 출력 (약 1분)
./scripts/dev.sh down      # 다 끄기 (데이터는 유지)
```

| 명령 | 하는 일 |
|---|---|
| `./scripts/dev.sh up` | `docker compose up -d` → Kafka·MySQL 준비 대기 → 앱 빌드 → ad-api 2대·ad-log-consumer·ad-batch 백그라운드 실행 |
| `./scripts/dev.sh test` | 이벤트 20세트 전송 → `ad_event` 건수 → 35초 뒤 `ad_stats_hourly` 집계 결과 |
| `./scripts/dev.sh status` | 컨테이너·앱 상태, 로드밸런서 응답 |
| `./scripts/dev.sh logs ad-batch` | 앱 로그 실시간 보기 (`ad-api-8081`, `ad-api-8082`, `ad-log-consumer`, `ad-batch`) |
| `./scripts/dev.sh restart` | 코드 고친 뒤 앱만 다시 빌드·재시작 |
| `./scripts/dev.sh down` | 앱 종료 + 컨테이너 정지 |

> `dev.sh up`에서 ad-batch는 테스트 편의상 **30초 주기**로 뜬다 (원래 설정은 5분).
> 앱 로그는 `.run/*.log`에 쌓인다.

---

## 1. 전체 구조 (지금까지 만든 것)

```
                          ┌──────────── Docker ────────────────────────────────────┐
SDK(flow.sh)              │                                                        │
   │ POST :8080           │  adwhale-lb (nginx :8080)                              │
   └─────────────────────▶│      │ 라운드로빈                                       │
                          └──────┼─────────────────────────────────────────────────┘
                                 ▼
             ┌── ad-api :8081 ──┐   ┌── ad-api :8082 ──┐        (Mac에서 실행)
             │ /v1/ad/request   │   │                  │
             │ /impression,click│   │                  │
             └────────┬─────────┘   └────────┬─────────┘
                      └──────────┬───────────┘ Producer
                                 ▼
          ┌──────── Docker: adwhale-kafka :9092 ─────────────────────────┐
          │ ad-request / ad-impression / ad-click   (파티션 3, 7일 보관)   │
          │ ad-*-dlq                                (파티션 1, 30일 보관)  │
          └──────────┬─────────────────────────────────┬─────────────────┘
       group: ad-log-writer                      group: ad-db-writer
                     ▼                                 ▼
        ┌──────────── ad-log-consumer (Mac) ──────────────────────┐
        │ AdLogConsumer → 로그 파일             AdEventDbConsumer  │
        │ (실패 → DLQ)  ad-log-consumer/logs/   (500건 배치)        │
        └──────────────────────────────────────────────┬──────────┘
                                                       ▼ INSERT IGNORE
                                  ┌── Docker: adwhale-mysql :3306 ──┐
                                  │ ad_event (원본)                 │
          ad-batch (Mac) ────────▶│   ↓ 5분마다 최근 2시간 GROUP BY  │
          (집계 배치)              │ ad_stats_hourly (시간별 집계)    │
                                  └─────────────────────────────────┘
```

### 모듈 (Gradle 멀티 모듈, 루트에서 `./gradlew ...`)

| 모듈 | 역할 | 포트 |
|---|---|---|
| `ad-common` | 토픽 이름(`AdTopics`), Kafka 메시지 형식(`AdRequestEvent`, `AdTrackingEvent`) | - |
| `ad-api` | 광고 요청/노출/클릭 API → Kafka 전송 | 8081, 8082 |
| `ad-log-consumer` | Kafka → 로그 파일(`ad-log-writer`) + MySQL(`ad-db-writer`) | - |
| `ad-batch` | `ad_event` → `ad_stats_hourly` 집계 | - |

### Docker 컨테이너 (`docker-compose.yml`)

| 컨테이너 | 주소 | 비고 |
|---|---|---|
| `adwhale-lb` (nginx) | http://localhost:8080 | SDK가 호출하는 주소. 설정: `nginx/ad-api.conf` |
| `adwhale-kafka` | localhost:9092 | 데이터: Docker 볼륨 `adwhale_kafka-data` |
| `adwhale-kafka-ui` | http://localhost:8989 | 토픽 메시지, Consumer 그룹 lag 확인 |
| `adwhale-kafka-init` | - | 토픽 생성 후 종료되는 게 정상 (Exited 0) |
| `adwhale-mysql` | localhost:3306 | `adwhale` / `adwhale`, DB `adwhale`. 스키마: `mysql/init/01-schema.sql` |

---

## 2. 단계별로 무엇을 했고 어떻게 확인하나

자세한 명령은 `README.md`의 같은 단계 섹션 참고.

| 단계 | 내용 | 핵심 확인 |
|---|---|---|
| 1 | Kafka(KRaft) Docker + 토픽 3개 | Kafka UI에 토픽 보임 |
| 2 | Spring Boot 4.1 / Java 25 프로젝트 | `curl localhost:8080/ping` |
| 3 | Consumer → 날짜별 로그 파일 | `ad-log-consumer/logs/{토픽}/` |
| 4 | API(`/v1/ad/*`) → Kafka Producer | `./scripts/flow.sh 10` |
| 4-1 | Consumer 별도 앱 분리 | ad-api만 꺼도 / consumer만 꺼도 서로 무관 |
| 4-2 | 멀티 모듈 + `ad-common` | `./gradlew build` |
| 4-3 | DLQ (`*-dlq` 토픽) | `./scripts/poison.sh` → Kafka UI `ad-impression-dlq` |
| 4-4 | Kafka 장애 시 노출/클릭 503 | `docker compose stop kafka` 후 노출 요청 → 503 |
| 4-5 | nginx 로드밸런서 | `./scripts/lb-test.sh` → 8081/8082 번갈아 응답 |
| 5-1 | MySQL + 스키마 | `mysql/practice.sql` |
| 5-2 | Kafka → MySQL (`ad-db-writer`, INSERT IGNORE) | `mysql/check.sql` |
| 5-3 | 집계 배치 `ad-batch` | `mysql/stats.sql` (집계 = 원본 직접 계산) ← **미검증** |

---

## 3. 테스트 시나리오 (실험 모음)

`./scripts/dev.sh up` 상태에서:

| 실험 | 방법 | 기대 결과 |
|---|---|---|
| 기본 흐름 | `./scripts/dev.sh test` | ad_event·ad_stats_hourly 숫자 증가 |
| ad-api 한 대 장애 | `kill $(cat .run/ad-api-8082.pid)` → `./scripts/lb-test.sh` | 전부 8081이 응답, 에러 없음 |
| Consumer 장애 | `kill $(cat .run/ad-log-consumer.pid)` → `flow.sh 5` → `dev.sh restart` | 재시작 후 밀린 5세트 저장 |
| Kafka 장애 | `docker compose stop kafka` → 노출 API 호출 | 3초 뒤 503 + Retry-After. `start kafka` 후 정상 |
| MySQL 장애 | `docker compose stop mysql` → `flow.sh 5` → `start mysql` | consumer 로그 "재시도 N회째" → 복구 후 자동 저장 |
| 잘못된 메시지 | `./scripts/poison.sh` | `ad-impression-dlq`에 3건 |
| 중복 처리 | ad-db-writer offset 처음으로 되돌리기 (README 5-2) | 로그 "신규 0건 / 중복 무시 N건" |
| 과거 재집계 | `./gradlew :ad-batch:bootRun --args='--adwhale.batch.lookback-hours=168'` | 7일치 ad_stats_hourly 생성 |

> 실험 후에는 `./scripts/dev.sh status`로 다 살아 있는지 확인.

---

## 4. 막히면 (트러블슈팅)

| 증상 | 원인 / 해결 |
|---|---|
| `Connection to node -1 (localhost:9092) could not be established` 반복 | Kafka가 안 떠 있음 → Docker Desktop 켜고 `docker compose up -d` |
| `Cannot connect to the Docker daemon` | Docker Desktop 꺼짐 |
| `UnsupportedClassVersionError ... 69.0 ... 65.0` | 터미널 java(21)로 실행함 → `dev.sh` / `run-api.sh` 사용 (빌드한 JDK 25로 실행) |
| `Port 8080/3306 already in use` | 다른 프로그램이 포트 사용 중 → `lsof -i :8080` 으로 확인 후 종료 |
| 스키마 고쳤는데 반영 안 됨 | `mysql/init/`은 볼륨이 비었을 때 1번만 실행 → README 5-1의 볼륨 삭제 명령 (데이터 삭제됨) |
| `Another git process seems to be running` | `rm .git/index.lock` |
| 앱이 안 뜸 | `./scripts/dev.sh logs <앱이름>` 로그 확인 |
| 완전히 처음부터 | `./scripts/dev.sh down && docker compose down -v` (Kafka·MySQL 데이터 전부 삭제) |

---

## 5. 남은 일 (다음에 할 것)

1. **5-3 검증 & 커밋** — `dev.sh up` → `dev.sh test` → `mysql/stats.sql`에서 1번(집계)과 2번(원본 계산) 숫자 일치 확인
2. **5-4 대시보드 통계 API** — `GET /v1/stats/...` 로 `ad_stats_hourly` 조회 (+ 필요하면 Redis 캐시)
3. **6단계 Presto** — MinIO(로컬 S3)에 원본을 파일로 쌓고 Presto로 대량 집계 → 결과를 MySQL로
4. 정리 거리
   - GitHub 원격 주소에 토큰이 남아 있으면 제거: `git remote set-url origin https://github.com/junhojohn/adwhale-ad-platform.git` (토큰은 GitHub에서 폐기)
   - 구조도 그림에 `adwhale-lb`, `adwhale-mysql`, `ad-batch` 추가

### Claude에게 이어서 요청할 때

> "adwhale-ad-platform 의 GUIDE.md 읽고, 5-3 검증 결과 보여줄게 / 5-4 진행해줘"

처럼 GUIDE.md를 먼저 읽게 하면 맥락을 바로 이어갈 수 있다.
