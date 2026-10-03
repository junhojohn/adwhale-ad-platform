# adwhale-ad-platform

adwhale-sdk-kotlin 리뉴얼과 함께 만드는 광고 서버 (요청 / 노출 / 클릭).

## 목표 구조

```
SDK ──HTTP──▶ API 서버 ──▶ Kafka (ad-request / ad-impression / ad-click)
                                  │
                                  ▼
                           Consumer ──▶ 디스크 로그 파일 ──▶ (이후) 저장소 ──▶ Presto
Dashboard ──▶ API 서버 ──▶ Redis(캐시) ──miss──▶ Presto
```

## 진행 단계

- [x] 1. Kafka 로컬 실행 + 토픽 3개 생성 (Docker Compose)
- [x] 2. Spring Boot 프로젝트 `ad-api` 생성 (Java 25 + Spring Boot 4.1 + Gradle KTS)
- [ ] 3. Consumer: 토픽 메시지를 디스크 로그 파일로 저장
- [ ] 4. API 서버: /request, /impression, /click → Kafka Producer
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
cd ad-api
./gradlew bootRun          # 첫 실행은 Gradle 9.5 + 의존성(+필요하면 JDK 25) 다운로드로 몇 분 걸림
```

다른 터미널에서:
```bash
curl http://localhost:8080/ping              # pong
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

테스트: `./gradlew test`
