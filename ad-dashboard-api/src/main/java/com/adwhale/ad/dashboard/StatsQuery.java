package com.adwhale.ad.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 모든 통계 API의 공통 조회 조건
 *
 * @param from        시작일 (포함)
 * @param to          종료일 (포함)  예: from=10-01, to=10-07 → 7일
 * @param adId        광고 필터 (null = 전체)
 * @param placementId 지면 필터 (null = 전체)
 */
public record StatsQuery(LocalDate from, LocalDate to, String adId, String placementId) {

    /** SQL 조건 시작: from 00:00 (포함) */
    public LocalDateTime start() {
        return from.atStartOfDay();
    }

    /** SQL 조건 끝: to 다음날 00:00 (미포함) → stat_hour < end */
    public LocalDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay();
    }

    public long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
