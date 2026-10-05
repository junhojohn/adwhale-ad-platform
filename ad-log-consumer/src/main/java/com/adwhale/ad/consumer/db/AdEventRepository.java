package com.adwhale.ad.consumer.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

/**
 * ad_event 테이블 저장소. JPA 대신 JdbcTemplate으로 SQL을 직접 쓴다 (INSERT IGNORE를 그대로 쓰기 위해).
 */
@Repository
public class AdEventRepository {

    /**
     * INSERT IGNORE: event_id(UNIQUE)가 이미 있으면 에러 없이 무시 → 같은 이벤트가 몇 번 와도 1행만 남는다.
     */
    private static final String INSERT_SQL = """
            INSERT IGNORE INTO ad_event
                (event_id, event_type, request_id, ad_id, placement_id, app_id, device_id, client_ts, server_ts)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    public AdEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 여러 행을 한 번에 INSERT (JDBC batch → DB 왕복 횟수를 줄임)
     *
     * @return 실제로 새로 저장된 행 수 (중복으로 무시된 행은 제외)
     */
    public int insertIgnore(List<AdEventRow> rows) {
        if (rows.isEmpty()) {
            return 0;
        }

        int[][] results = jdbc.batchUpdate(INSERT_SQL, rows, rows.size(), (ps, row) -> {
            ps.setString(1, row.eventId());
            ps.setString(2, row.eventType());
            ps.setString(3, row.requestId());
            ps.setString(4, row.adId());
            ps.setString(5, row.placementId());
            ps.setString(6, row.appId());
            ps.setString(7, row.deviceId());
            if (row.clientTs() != null) {
                ps.setObject(8, row.clientTs());
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }
            ps.setObject(9, row.serverTs());
        });

        int inserted = 0;
        for (int[] batch : results) {
            for (int count : batch) {
                if (count > 0) {   // 1 = 새로 저장, 0 = 중복이라 무시됨
                    inserted++;
                }
            }
        }
        return inserted;
    }
}
