-- 5-3단계 확인용: 집계 결과 vs 원본 비교
-- 실행: docker exec -i adwhale-mysql mysql -uadwhale -padwhale -t adwhale < mysql/stats.sql

-- 1) 집계 테이블 (대시보드가 볼 값)
SELECT stat_hour, ad_id, placement_id, requests, impressions, clicks,
       ROUND(clicks / NULLIF(impressions, 0) * 100, 2) AS ctr_percent,
       updated_at
FROM ad_stats_hourly
ORDER BY stat_hour DESC, ad_id, placement_id
LIMIT 20;

-- 2) 같은 값을 원본에서 직접 계산 (1번과 숫자가 같아야 정상)
SELECT DATE_FORMAT(server_ts, '%Y-%m-%d %H:00:00') AS stat_hour, ad_id, placement_id,
       SUM(event_type = 'request') AS requests,
       SUM(event_type = 'impression') AS impressions,
       SUM(event_type = 'click') AS clicks
FROM ad_event
WHERE ad_id IS NOT NULL
GROUP BY 1, ad_id, placement_id
ORDER BY 1 DESC, ad_id, placement_id
LIMIT 20;
