package com.adwhale.ad.dashboard;

import com.adwhale.ad.dashboard.StatsRepository.AdTotals;
import com.adwhale.ad.dashboard.StatsRepository.GroupRow;
import com.adwhale.ad.dashboard.StatsRepository.PlacementTotals;
import com.adwhale.ad.dashboard.dto.BreakdownResponse;
import com.adwhale.ad.dashboard.dto.FiltersResponse;
import com.adwhale.ad.dashboard.dto.SummaryResponse;
import com.adwhale.ad.dashboard.dto.TimeseriesResponse;
import com.adwhale.ad.dashboard.exception.InvalidParamException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 스펙(docs/dashboard-spec.md)의 규칙을 담당: 파라미터 검증, 지표 계산, 빈 구간 채우기
 */
@Service
public class StatsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_DAYS = 31;        // 전체 조회 최대 범위
    private static final int MAX_HOURLY_DAYS = 2;  // 시간별 추이 최대 범위
    private static final Set<String> BREAKDOWN_BY = Set.of("ad", "placement", "ad_placement");

    private final StatsRepository repository;

    public StatsService(StatsRepository repository) {
        this.repository = repository;
    }

    public FiltersResponse filters() {
        LocalDateTime since = LocalDate.now(KST).minusDays(MAX_DAYS - 1L).atStartOfDay();
        return new FiltersResponse(repository.findAdIds(since), repository.findPlacementIds(since));
    }

    public SummaryResponse summary(StatsQuery q) {
        validate(q);
        AdTotals ad = repository.sumAd(q);

        Long totalRequests = null;
        Double fillRate = null;
        LocalDateTime updatedAt = ad.updatedAt();

        if (q.adId() == null) {   // 광고 필터가 없을 때만 필레이트 (스펙 3번)
            PlacementTotals pl = repository.sumPlacement(q);
            totalRequests = pl.requests();
            fillRate = Metrics.percent(pl.filled(), pl.requests());
            updatedAt = max(updatedAt, pl.updatedAt());
        }

        return new SummaryResponse(q.from(), q.to(),
                totalRequests, ad.requests(), fillRate,
                ad.impressions(), ad.clicks(),
                Metrics.percent(ad.clicks(), ad.impressions()),
                Metrics.percent(ad.impressions(), ad.requests()),
                updatedAt);
    }

    public TimeseriesResponse timeseries(StatsQuery q, String granularity) {
        validate(q);
        boolean daily = switch (granularity) {
            case "day" -> true;
            case "hour" -> {
                if (q.days() > MAX_HOURLY_DAYS) {
                    throw new InvalidParamException("granularity=hour 는 최대 " + MAX_HOURLY_DAYS + "일까지 조회할 수 있습니다");
                }
                yield false;
            }
            default -> throw new InvalidParamException("granularity 는 hour 또는 day 여야 합니다: " + granularity);
        };

        Map<LocalDateTime, AdTotals> ad = repository.adSeries(q, daily);
        Map<LocalDateTime, PlacementTotals> pl = q.adId() == null ? repository.placementSeries(q, daily) : Map.of();

        // 데이터가 없는 시간/날짜도 0으로 채워서 차트가 끊기지 않게 (스펙 6-3)
        List<TimeseriesResponse.Point> points = new ArrayList<>();
        for (LocalDateTime t = q.start(); t.isBefore(q.endExclusive()); t = daily ? t.plusDays(1) : t.plusHours(1)) {
            AdTotals a = ad.getOrDefault(t, new AdTotals(0, 0, 0, null));
            Long totalRequests = null;
            Double fillRate = null;
            if (q.adId() == null) {
                PlacementTotals p = pl.getOrDefault(t, new PlacementTotals(0, 0, null));
                totalRequests = p.requests();
                fillRate = Metrics.percent(p.filled(), p.requests());
            }
            points.add(new TimeseriesResponse.Point(t, totalRequests, fillRate,
                    a.impressions(), a.clicks(), Metrics.percent(a.clicks(), a.impressions())));
        }
        return new TimeseriesResponse(granularity, points);
    }

    public BreakdownResponse breakdown(StatsQuery q, String by) {
        validate(q);
        if (!BREAKDOWN_BY.contains(by)) {
            throw new InvalidParamException("by 는 ad, placement, ad_placement 중 하나여야 합니다: " + by);
        }

        List<GroupRow> groups = repository.breakdown(q, by);
        boolean withFill = by.equals("placement") && q.adId() == null;
        Map<String, PlacementTotals> fill = withFill ? repository.placementTotalsByPlacement(q) : Map.of();

        List<BreakdownResponse.Row> rows = new ArrayList<>();
        for (GroupRow g : groups) {
            Long totalRequests = null;
            Double fillRate = null;
            if (withFill) {
                PlacementTotals p = fill.getOrDefault(g.placementId(), new PlacementTotals(0, 0, null));
                totalRequests = p.requests();
                fillRate = Metrics.percent(p.filled(), p.requests());
            }
            rows.add(new BreakdownResponse.Row(g.adId(), g.placementId(),
                    g.requests(), g.impressions(), g.clicks(),
                    Metrics.percent(g.clicks(), g.impressions()),
                    Metrics.percent(g.impressions(), g.requests()),
                    totalRequests, fillRate));
        }
        rows.sort(Comparator.comparingLong(BreakdownResponse.Row::impressions).reversed());
        return new BreakdownResponse(by, rows);
    }

    private static void validate(StatsQuery q) {
        if (q.from().isAfter(q.to())) {
            throw new InvalidParamException("from 이 to 보다 늦습니다: " + q.from() + " > " + q.to());
        }
        if (q.days() > MAX_DAYS) {
            throw new InvalidParamException("조회 기간은 최대 " + MAX_DAYS + "일입니다 (요청: " + q.days() + "일)");
        }
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
