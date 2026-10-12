package com.adwhale.ad.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GET /v1/stats/summary — KPI 카드
 *
 * totalRequests / fillRate 는 광고 필터(adId)를 걸면 null
 * (노필 요청은 어떤 광고에도 속하지 않기 때문)
 */
public record SummaryResponse(
        LocalDate from,
        LocalDate to,
        Long totalRequests,
        long filledRequests,
        Double fillRate,
        long impressions,
        long clicks,
        Double ctr,
        Double impressionRate,
        LocalDateTime dataUpdatedAt
) {
}
