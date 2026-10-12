package com.adwhale.ad.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 집계 테이블 조회 SQL 모음 (조회 전용)
 *
 *   ad_stats_hourly        : 시간 × 광고 × 지면 → 광고 요청·노출·클릭
 *   placement_stats_hourly : 시간 × 지면        → 전체 요청·광고 응답 요청 (필레이트)
 */
@Repository
public class StatsRepository {

    /** 광고 단위 합계 */
    public record AdTotals(long requests, long impressions, long clicks, LocalDateTime updatedAt) {
    }

    /** 지면 단위 합계 (필레이트용) */
    public record PlacementTotals(long requests, long filled, LocalDateTime updatedAt) {
    }

    private final JdbcTemplate jdbc;

    public StatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── filters ──────────────────────────────────────────────────────

    public List<String> findAdIds(LocalDateTime since) {
        return jdbc.queryForList(
                "SELECT DISTINCT ad_id FROM ad_stats_hourly WHERE stat_hour >= ? ORDER BY ad_id",
                String.class, since);
    }

    public List<String> findPlacementIds(LocalDateTime since) {
        // 노필만 있는 지면도 목록에 나오도록 두 테이블을 합친다 (UNION = 중복 제거)
        return jdbc.queryForList("""
                SELECT placement_id FROM ad_stats_hourly        WHERE stat_hour >= ?
                UNION
                SELECT placement_id FROM placement_stats_hourly WHERE stat_hour >= ?
                ORDER BY placement_id
                """, String.class, since, since);
    }

    // ── summary ──────────────────────────────────────────────────────

    public AdTotals sumAd(StatsQuery q) {
        Where w = adWhere(q);
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(requests), 0)    AS req,
                       COALESCE(SUM(impressions), 0) AS imp,
                       COALESCE(SUM(clicks), 0)      AS clk,
                       MAX(updated_at)               AS upd
                FROM ad_stats_hourly
                WHERE \s""" + w.sql(),   // \s = 공백 유지 (텍스트 블록은 줄 끝 공백을 지워서 "WHEREstat_hour"가 됐었음)
                (rs, i) -> new AdTotals(rs.getLong("req"), rs.getLong("imp"), rs.getLong("clk"),
                        rs.getObject("upd", LocalDateTime.class)),
                w.args());
    }

    public PlacementTotals sumPlacement(StatsQuery q) {
        Where w = placementWhere(q);
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(requests), 0) AS req,
                       COALESCE(SUM(filled), 0)   AS fil,
                       MAX(updated_at)            AS upd
                FROM placement_stats_hourly
                WHERE \s""" + w.sql(),   // \s = 공백 유지 (텍스트 블록은 줄 끝 공백을 지워서 "WHEREstat_hour"가 됐었음)
                (rs, i) -> new PlacementTotals(rs.getLong("req"), rs.getLong("fil"),
                        rs.getObject("upd", LocalDateTime.class)),
                w.args());
    }

    // ── timeseries ───────────────────────────────────────────────────

    /** @param daily true면 일별(그날 00:00 기준으로 묶음), false면 시간별 */
    public Map<LocalDateTime, AdTotals> adSeries(StatsQuery q, boolean daily) {
        Where w = adWhere(q);
        String bucket = bucketExpr(daily);
        Map<LocalDateTime, AdTotals> result = new LinkedHashMap<>();
        jdbc.query("SELECT " + bucket + " AS t, SUM(requests) AS req, SUM(impressions) AS imp, SUM(clicks) AS clk"
                        + " FROM ad_stats_hourly WHERE " + w.sql() + " GROUP BY t",
                rs -> {
                    result.put(rs.getObject("t", LocalDateTime.class),
                            new AdTotals(rs.getLong("req"), rs.getLong("imp"), rs.getLong("clk"), null));
                },
                w.args());
        return result;
    }

    public Map<LocalDateTime, PlacementTotals> placementSeries(StatsQuery q, boolean daily) {
        Where w = placementWhere(q);
        String bucket = bucketExpr(daily);
        Map<LocalDateTime, PlacementTotals> result = new LinkedHashMap<>();
        jdbc.query("SELECT " + bucket + " AS t, SUM(requests) AS req, SUM(filled) AS fil"
                        + " FROM placement_stats_hourly WHERE " + w.sql() + " GROUP BY t",
                rs -> {
                    result.put(rs.getObject("t", LocalDateTime.class),
                            new PlacementTotals(rs.getLong("req"), rs.getLong("fil"), null));
                },
                w.args());
        return result;
    }

    // ── breakdown ────────────────────────────────────────────────────

    public record GroupRow(String adId, String placementId, long requests, long impressions, long clicks) {
    }

    /** @param by ad | placement | ad_placement (Service에서 검증된 값만 들어옴) */
    public List<GroupRow> breakdown(StatsQuery q, String by) {
        Where w = adWhere(q);
        String adCol = by.equals("placement") ? "NULL" : "ad_id";
        String placementCol = by.equals("ad") ? "NULL" : "placement_id";
        String groupBy = switch (by) {
            case "ad" -> "ad_id";
            case "placement" -> "placement_id";
            default -> "ad_id, placement_id";
        };
        return jdbc.query("SELECT " + adCol + " AS a, " + placementCol + " AS p,"
                        + " SUM(requests) AS req, SUM(impressions) AS imp, SUM(clicks) AS clk"
                        + " FROM ad_stats_hourly WHERE " + w.sql()
                        + " GROUP BY " + groupBy
                        + " ORDER BY imp DESC",
                (rs, i) -> new GroupRow(rs.getString("a"), rs.getString("p"),
                        rs.getLong("req"), rs.getLong("imp"), rs.getLong("clk")),
                w.args());
    }

    public Map<String, PlacementTotals> placementTotalsByPlacement(StatsQuery q) {
        Where w = placementWhere(q);
        Map<String, PlacementTotals> result = new LinkedHashMap<>();
        jdbc.query("SELECT placement_id AS p, SUM(requests) AS req, SUM(filled) AS fil"
                        + " FROM placement_stats_hourly WHERE " + w.sql() + " GROUP BY placement_id",
                rs -> {
                    result.put(rs.getString("p"), new PlacementTotals(rs.getLong("req"), rs.getLong("fil"), null));
                },
                w.args());
        return result;
    }

    // ── WHERE 조립 ───────────────────────────────────────────────────
    // 값은 항상 ? 바인딩으로 전달 (문자열로 SQL에 이어붙이지 않음 → SQL 인젝션 방지)

    private record Where(String sql, Object[] args) {
    }

    private static Where adWhere(StatsQuery q) {
        StringBuilder sql = new StringBuilder("stat_hour >= ? AND stat_hour < ?");
        List<Object> args = new ArrayList<>(List.of(q.start(), q.endExclusive()));
        if (q.adId() != null) {
            sql.append(" AND ad_id = ?");
            args.add(q.adId());
        }
        if (q.placementId() != null) {
            sql.append(" AND placement_id = ?");
            args.add(q.placementId());
        }
        return new Where(sql.toString(), args.toArray());
    }

    /** placement_stats_hourly에는 ad_id가 없으므로 지면 필터만 적용 */
    private static Where placementWhere(StatsQuery q) {
        StringBuilder sql = new StringBuilder("stat_hour >= ? AND stat_hour < ?");
        List<Object> args = new ArrayList<>(List.of(q.start(), q.endExclusive()));
        if (q.placementId() != null) {
            sql.append(" AND placement_id = ?");
            args.add(q.placementId());
        }
        return new Where(sql.toString(), args.toArray());
    }

    private static String bucketExpr(boolean daily) {
        // stat_hour는 이미 "시간 단위"라 시간별은 그대로, 일별은 그날 00:00으로 내림
        return daily ? "TIMESTAMP(DATE(stat_hour))" : "stat_hour";
    }
}
