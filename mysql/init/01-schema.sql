-- =====================================================================
-- adwhale 광고 이벤트 스키마
-- docker 볼륨(mysql-data)이 비어 있을 때 한 번만 자동 실행된다.
-- 수정 후 다시 적용하려면: docker compose rm -sf mysql && docker volume rm adwhale_mysql-data && docker compose up -d mysql
-- =====================================================================

USE adwhale;

-- ---------------------------------------------------------------------
-- 1) ad_event : 원본 이벤트 (요청 / 노출 / 클릭)
--    Kafka의 ad-request / ad-impression / ad-click 메시지가 1건당 1행으로 들어온다.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_event (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '내부 PK. 순서대로 증가 → INSERT가 항상 끝에 붙어서 빠름',
    event_id      VARCHAR(36)     NOT NULL COMMENT '이벤트 고유 ID(UUID). UNIQUE → 같은 이벤트가 또 와도 1번만 저장',
    event_type    VARCHAR(16)     NOT NULL COMMENT 'request / impression / click',
    request_id    VARCHAR(36)     NOT NULL COMMENT '광고 요청 ID. 요청 → 노출 → 클릭을 이어주는 키',
    ad_id         VARCHAR(32)     NULL     COMMENT '광고 ID (요청인데 광고가 없으면 NULL = 노필)',
    placement_id  VARCHAR(32)     NOT NULL COMMENT '광고 지면 ID',
    app_id        VARCHAR(32)     NULL     COMMENT '매체 앱 ID (요청 이벤트에만 있음)',
    device_id     VARCHAR(64)     NOT NULL COMMENT '기기 식별자',
    client_ts     DATETIME(3)     NULL     COMMENT 'SDK에서 이벤트가 발생한 시각 (노출/클릭만)',
    server_ts     DATETIME(3)     NOT NULL COMMENT '서버가 받은 시각 (집계 기준 시각)',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'DB에 저장된 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_event_id (event_id),                 -- 중복 제거의 핵심
    KEY idx_server_ts_ad (server_ts, ad_id),           -- "최근 N시간" 집계 조회용
    KEY idx_request_id (request_id),                   -- 요청 하나의 노출/클릭 추적용
    CONSTRAINT chk_event_type CHECK (event_type IN ('request', 'impression', 'click'))
) ENGINE = InnoDB COMMENT = '광고 원본 이벤트';

-- ---------------------------------------------------------------------
-- 2) ad_stats_hourly : 시간별 · 광고별 · 지면별 집계 (대시보드가 읽는 테이블)
--    ad_event를 GROUP BY 해서 "덮어쓰기" 방식으로 채운다 (몇 번 다시 돌려도 결과 동일).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ad_stats_hourly (
    stat_hour     DATETIME        NOT NULL COMMENT '집계 시간 (예: 2026-10-06 15:00:00 = 15시대)',
    ad_id         VARCHAR(32)     NOT NULL,
    placement_id  VARCHAR(32)     NOT NULL,
    requests      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    impressions   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    clicks        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
                                  COMMENT '마지막 집계 시각',

    PRIMARY KEY (stat_hour, ad_id, placement_id)        -- 같은 시간·광고·지면은 1행만 → ON DUPLICATE KEY UPDATE로 덮어쓰기
) ENGINE = InnoDB COMMENT = '시간별 광고 집계';
