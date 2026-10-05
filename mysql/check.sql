-- 5-2단계 확인용: ad_event에 쌓인 데이터 보기
-- 실행: docker exec -i adwhale-mysql mysql -uadwhale -padwhale -t adwhale < mysql/check.sql

-- 이벤트 종류별 건수
SELECT event_type, COUNT(*) AS cnt, MIN(server_ts) AS first_ts, MAX(server_ts) AS last_ts
FROM ad_event GROUP BY event_type;

-- event_id 중복이 하나도 없는지 (결과가 비어 있어야 정상 — UNIQUE라 애초에 불가능)
SELECT event_id, COUNT(*) FROM ad_event GROUP BY event_id HAVING COUNT(*) > 1;

-- 최근 10건
SELECT id, event_type, event_id, request_id, ad_id, placement_id, client_ts, server_ts
FROM ad_event ORDER BY id DESC LIMIT 10;

-- 광고 요청 하나의 흐름 (가장 최근 클릭이 있는 request_id 기준)
SELECT event_type, event_id, client_ts, server_ts
FROM ad_event
WHERE request_id = (SELECT request_id FROM ad_event WHERE event_type = 'click' ORDER BY id DESC LIMIT 1)
ORDER BY server_ts;
