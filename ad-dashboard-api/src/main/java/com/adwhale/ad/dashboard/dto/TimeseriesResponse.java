package com.adwhale.ad.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

/** GET /v1/stats/timeseries — 추이 차트 (데이터 없는 구간도 0으로 채움) */
public record TimeseriesResponse(String granularity, List<Point> points) {

    public record Point(
            LocalDateTime time,
            Long totalRequests,   // adId 필터 시 null
            Double fillRate,      // adId 필터 시 null
            long impressions,
            long clicks,
            Double ctr
    ) {
    }
}
