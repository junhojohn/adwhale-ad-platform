-- =====================================================================
-- 5-1단계 손으로 해보는 연습 쿼리 (Consumer 붙이기 전에 스키마 동작 확인용)
-- 실행: docker exec -i adwhale-mysql mysql -uadwhale -padwhale adwhale < mysql/practice.sql
-- 또는 접속해서 한 줄씩: docker exec -it adwhale-mysql mysql -uadwhale -padwhale adwhale
-- =====================================================================

-- 1) 요청 1건 + 노출 1건 + 클릭 1건
INSERT IGNORE INTO ad_event (event_id, event_type, request_id, ad_id, placement_id, app_id, device_id, client_ts, server_ts)
VALUES ('practice-req-1', 'request',    'practice-r1', 'A001', 'P01', 'APP01', 'device-0', NULL,                     NOW(3)),
       ('practice-imp-1', 'impression', 'practice-r1', 'A001', 'P01', NULL,    'device-0', NOW(3) - INTERVAL 1 SECOND, NOW(3)),
       ('practice-clk-1', 'click',      'practice-r1', 'A001', 'P01', NULL,    'device-0', NOW(3),                   NOW(3));

-- 2) 같은 노출(event_id 동일)을 또 넣어봄 → INSERT IGNORE라 에러 없이 무시됨 (affected rows = 0)
INSERT IGNORE INTO ad_event (event_id, event_type, request_id, ad_id, placement_id, device_id, client_ts, server_ts)
VALUES ('practice-imp-1', 'impression', 'practice-r1', 'A001', 'P01', 'device-0', NOW(3), NOW(3));
SELECT ROW_COUNT() AS '중복 INSERT 결과 (0이면 무시됨)';

-- 3) 원본 확인
SELECT id, event_id, event_type, request_id, ad_id, server_ts FROM ad_event ORDER BY id DESC LIMIT 10;

-- 4) 집계: 최근 2시간치를 다시 계산해서 덮어쓰기 (5-3단계 배치가 할 일을 손으로)
INSERT INTO ad_stats_hourly (stat_hour, ad_id, placement_id, requests, impressions, clicks)
SELECT DATE_FORMAT(server_ts, '%Y-%m-%d %H:00:00') AS stat_hour,
       ad_id,
       placement_id,
       SUM(event_type = 'request')    AS requests,
       SUM(event_type = 'impression') AS impressions,
       SUM(event_type = 'click')      AS clicks
FROM ad_event
WHERE server_ts >= DATE_FORMAT(NOW() - INTERVAL 2 HOUR, '%Y-%m-%d %H:00:00')
  AND ad_id IS NOT NULL
GROUP BY stat_hour, ad_id, placement_id
ON DUPLICATE KEY UPDATE requests    = VALUES(requests),
                        impressions = VALUES(impressions),
                        clicks      = VALUES(clicks);

-- 5) 대시보드가 볼 결과 (CTR = 클릭 / 노출)
SELECT stat_hour, ad_id, placement_id, requests, impressions, clicks,
       ROUND(clicks / NULLIF(impressions, 0) * 100, 2) AS ctr_percent
FROM ad_stats_hourly
ORDER BY stat_hour DESC, ad_id;

-- 6) 연습 데이터 정리
-- DELETE FROM ad_event WHERE event_id LIKE 'practice-%';
-- DELETE FROM ad_stats_hourly;
