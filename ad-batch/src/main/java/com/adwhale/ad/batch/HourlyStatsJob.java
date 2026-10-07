package com.adwhale.ad.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 시간별 집계 배치
 *
 * 5분마다 "최근 N시간"(기본 2시간 = 이전 시간대 + 현재 시간대)을 ad_event에서 다시 계산해서
 * ad_stats_hourly에 덮어쓴다.
 *
 *   왜 "+1 누적"이 아니라 "다시 계산 후 덮어쓰기"인가?
 *   → 몇 번을 다시 돌려도 결과가 같다(멱등). 배치가 중간에 죽거나 두 번 돌아도 숫자가 틀어지지 않는다.
 *
 *   왜 최근 2시간만?
 *   → 집계 기준이 server_ts(서버 수신 시각)라서, 늦게 도착한 이벤트도 "도착한 시간대"에 들어간다.
 *     따라서 지난 시간대는 시간이 지나면 더 이상 바뀌지 않고, 정각 직후 이전 시간대 마무리분만 챙기면 된다.
 */
@Component
public class HourlyStatsJob {

    private static final Logger log = LoggerFactory.getLogger(HourlyStatsJob.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * MySQL 8의 INSERT ... SELECT ... ON DUPLICATE KEY UPDATE
     * - SELECT로 시간·광고·지면별 건수를 계산하고
     * - PK(stat_hour, ad_id, placement_id)가 이미 있으면 UPDATE, 없으면 INSERT
     * - SELECT를 서브쿼리(agg)로 감싸면 UPDATE 절에서 계산된 컬럼 이름(req, imp, clk)을 바로 쓸 수 있다
     *   (예전 문법 VALUES(col)은 MySQL 8에서 deprecated)
     */
    private static final String UPSERT_SQL = """
            INSERT INTO ad_stats_hourly (stat_hour, ad_id, placement_id, requests, impressions, clicks)
            SELECT * FROM (
                SELECT DATE_FORMAT(server_ts, '%Y-%m-%d %H:00:00') AS h,
                       ad_id                                       AS a,
                       placement_id                                AS p,
                       SUM(event_type = 'request')                 AS req,
                       SUM(event_type = 'impression')              AS imp,
                       SUM(event_type = 'click')                   AS clk
                FROM ad_event
                WHERE server_ts >= ?
                  AND ad_id IS NOT NULL
                GROUP BY h, a, p
            ) AS agg
            ON DUPLICATE KEY UPDATE requests    = req,
                                    impressions = imp,
                                    clicks      = clk
            """;

    private static final String COUNT_SQL =
            "SELECT COUNT(*) FROM ad_stats_hourly WHERE stat_hour >= ?";

    private final JdbcTemplate jdbc;
    private final int lookbackHours;

    public HourlyStatsJob(JdbcTemplate jdbc, @Value("${adwhale.batch.lookback-hours:2}") int lookbackHours) {
        this.jdbc = jdbc;
        this.lookbackHours = lookbackHours;
    }

    /**
     * fixedDelay: 이전 실행이 "끝난 뒤" 5분 기다렸다가 다음 실행 → 실행이 길어져도 겹치지 않음
     * initialDelay: 앱 시작 5초 뒤 첫 실행 (바로 결과를 볼 수 있게)
     */
    @Scheduled(initialDelayString = "${adwhale.batch.initial-delay:PT5S}",
               fixedDelayString = "${adwhale.batch.interval:PT5M}")
    public void run() {
        // 예: 지금 15:23, lookback 2 → 14:00부터 다시 계산 (14시대 + 15시대)
        LocalDateTime from = LocalDateTime.now(KST).truncatedTo(ChronoUnit.HOURS).minusHours(lookbackHours - 1L);
        long start = System.currentTimeMillis();

        try {
            jdbc.update(UPSERT_SQL, from);
            Integer rows = jdbc.queryForObject(COUNT_SQL, Integer.class, from);
            log.info("시간별 집계 완료: {} 부터 / 집계 행 {}개 / {}ms",
                    from, rows, System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 실패해도 다음 주기에 같은 구간을 다시 계산하므로 데이터가 틀어지지 않음
            log.error("시간별 집계 실패 ({} 부터): {}", from, e.getMessage());
        }
    }
}
