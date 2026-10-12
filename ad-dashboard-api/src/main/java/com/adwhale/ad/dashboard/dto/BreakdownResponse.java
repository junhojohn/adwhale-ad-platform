package com.adwhale.ad.dashboard.dto;

import java.util.List;

/** GET /v1/stats/breakdown — 상세 테이블 (노출 수 내림차순) */
public record BreakdownResponse(String by, List<Row> rows) {

    public record Row(
            String adId,          // by=placement 이면 null
            String placementId,   // by=ad 이면 null
            long requests,
            long impressions,
            long clicks,
            Double ctr,
            Double impressionRate,
            Long totalRequests,   // by=placement 일 때만
            Double fillRate       // by=placement 일 때만
    ) {
    }
}
