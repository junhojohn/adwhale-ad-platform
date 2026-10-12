-- =====================================================================
-- 레이크 스키마 / 원본 이벤트 테이블 정의 (Trino)
-- 실행: ./scripts/lake-test.sh  (또는 docker exec -i adwhale-trino trino < trino/sql/01-lake-tables.sql)
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS lake.ads
WITH (location = 's3://adwhale-lake/warehouse/ads/');

-- 원본 이벤트: Kafka 메시지(JSON 한 줄 = 1건)를 그대로 쌓은 파일들
--
--   s3://adwhale-lake/raw/events/event_type=impression/dt=2026-10-12/hour=15/xxxx.json
--                                └─────────── 파티션 (폴더 이름 = 컬럼 값) ─────────┘
--
-- 파티션: 폴더 단위로 데이터를 나눠두는 것. WHERE dt = '2026-10-12' 로 조회하면
--         그 날짜 폴더의 파일만 읽음 → 데이터가 많아도 필요한 만큼만 읽어서 빠름
--
-- external_location: Trino가 만든 파일이 아니라 "이미 있는(앱이 쓴) 파일"을 읽는 테이블
-- 컬럼 이름: Trino는 소문자로 저장. JSON 키(eventId)와는 대소문자 구분 없이 매칭됨
CREATE TABLE IF NOT EXISTS lake.ads.raw_events (
    eventid      VARCHAR,
    requestid    VARCHAR,
    adid         VARCHAR,
    placementid  VARCHAR,
    appid        VARCHAR,     -- 요청 이벤트에만 있음
    deviceid     VARCHAR,
    clientts     BIGINT,      -- epoch millis (노출/클릭만)
    serverts     BIGINT,      -- epoch millis
    event_type   VARCHAR,     -- ↓ 파티션 컬럼 (반드시 맨 뒤, 순서대로)
    dt           VARCHAR,
    hour         VARCHAR
)
WITH (
    external_location = 's3://adwhale-lake/raw/events/',
    format = 'JSON',
    partitioned_by = ARRAY['event_type', 'dt', 'hour']
);
