package com.adwhale.ad.dashboard;

import com.adwhale.ad.dashboard.dto.BreakdownResponse;
import com.adwhale.ad.dashboard.dto.FiltersResponse;
import com.adwhale.ad.dashboard.dto.SummaryResponse;
import com.adwhale.ad.dashboard.dto.TimeseriesResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 대시보드 통계 API (docs/dashboard-spec.md 6번)
 *
 *   GET /v1/stats/filters
 *   GET /v1/stats/summary    ?from&to[&adId][&placementId]
 *   GET /v1/stats/timeseries ?from&to&granularity=hour|day[&adId][&placementId]
 *   GET /v1/stats/breakdown  ?from&to&by=ad|placement|ad_placement[&adId][&placementId]
 */
@RestController
@RequestMapping("/v1/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping("/filters")
    public FiltersResponse filters() {
        return service.filters();
    }

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String adId,
            @RequestParam(required = false) String placementId) {
        return service.summary(query(from, to, adId, placementId));
    }

    @GetMapping("/timeseries")
    public TimeseriesResponse timeseries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) String adId,
            @RequestParam(required = false) String placementId) {
        return service.timeseries(query(from, to, adId, placementId), granularity);
    }

    @GetMapping("/breakdown")
    public BreakdownResponse breakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ad_placement") String by,
            @RequestParam(required = false) String adId,
            @RequestParam(required = false) String placementId) {
        return service.breakdown(query(from, to, adId, placementId), by);
    }

    /** 빈 문자열 필터(adId=)는 "전체"로 취급 */
    private static StatsQuery query(LocalDate from, LocalDate to, String adId, String placementId) {
        return new StatsQuery(from, to, blankToNull(adId), blankToNull(placementId));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
