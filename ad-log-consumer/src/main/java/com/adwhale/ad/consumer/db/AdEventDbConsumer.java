package com.adwhale.ad.consumer.db;

import com.adwhale.ad.common.kafka.AdTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 요청/노출/클릭 토픽을 읽어서 MySQL ad_event 테이블에 저장한다.
 *
 * - groupId = ad-db-writer : 파일 저장(ad-log-writer)과 다른 그룹 → 같은 메시지를 각자 처음부터 끝까지 따로 읽음
 *                            (서로의 진행 위치/장애에 영향 없음)
 * - 배치 리스너 : 메시지를 1건씩이 아니라 최대 500건씩 받아서 한 번에 INSERT → DB 부하 감소
 * - 잘못된 메시지는 건너뜀 : DLQ 격리는 파일 저장 쪽(ad-log-writer)이 이미 담당
 * - DB 오류는 예외를 던짐 : DbConsumerConfig의 에러 핸들러가 DB가 살아날 때까지 무한 재시도
 *   → 같은 배치가 다시 들어와도 INSERT IGNORE라 중복 저장되지 않음
 */
@Component
public class AdEventDbConsumer {

    private static final Logger log = LoggerFactory.getLogger(AdEventDbConsumer.class);

    private final AdEventRepository repository;
    private final JsonMapper jsonMapper;

    public AdEventDbConsumer(AdEventRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(
            id = "ad-db-writer",
            groupId = "ad-db-writer",
            topics = {AdTopics.REQUEST, AdTopics.IMPRESSION, AdTopics.CLICK},
            containerFactory = DbConsumerConfig.DB_BATCH_FACTORY)
    public void consume(List<ConsumerRecord<String, String>> records) {
        List<AdEventRow> rows = new ArrayList<>(records.size());
        int invalid = 0;

        for (ConsumerRecord<String, String> record : records) {
            AdEventRow row = record.value() == null ? null
                    : AdEventRow.from(record.topic(), record.value(), jsonMapper);
            if (row == null) {
                invalid++;
                log.debug("DB 저장 제외(형식 오류) [{}] offset={} value={}",
                        record.topic(), record.offset(), record.value());
            } else {
                rows.add(row);
            }
        }

        int inserted = repository.insertIgnore(rows);   // DB 오류 시 여기서 예외 → 배치 전체 재시도

        log.info("DB 저장: 받음 {}건 → 신규 {}건 / 중복 무시 {}건 / 형식 오류 {}건",
                records.size(), inserted, rows.size() - inserted, invalid);
    }
}
