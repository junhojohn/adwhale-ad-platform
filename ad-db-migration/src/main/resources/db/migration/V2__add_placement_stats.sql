-- =====================================================================
-- V2: 지면별 시간 집계 테이블 (필레이트용)
--
-- 필레이트 = 광고가 응답된 요청 / 전체 요청(노필 포함)
-- 노필 요청은 ad_id가 없어서 ad_stats_hourly(PK에 ad_id 포함)에 넣을 수 없음 → 지면 단위 테이블을 따로 둔다.
-- 채우는 쪽: ad-batch (ad_event의 request 이벤트를 GROUP BY)
-- =====================================================================

CREATE TABLE placement_stats_hourly (
    stat_hour     DATETIME        NOT NULL COMMENT '집계 시간 (예: 2026-10-10 15:00:00 = 15시대)',
    placement_id  VARCHAR(32)     NOT NULL,
    requests      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '전체 요청 (노필 포함)',
    filled        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '광고가 응답된 요청',
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
                                  COMMENT '마지막 집계 시각',

    PRIMARY KEY (stat_hour, placement_id)
) ENGINE = InnoDB COMMENT = '시간별 지면 집계 (필레이트)';
