package com.adwhale.ad.consumer;

import com.adwhale.ad.common.kafka.AdTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 요청/노출/클릭 토픽을 구독해서, 받은 메시지를 그대로 로그 파일에 한 줄씩 저장한다.
 *
 * 흐름: Kafka 토픽 → consume() → 검증 → AdLogFileWriter로 파일에 append
 * 여기서 예외를 던지면 KafkaErrorConfig 규칙에 따라 재시도 또는 DLQ로 보내진다.
 */
@Component
public class AdLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AdLogConsumer.class);

    private final AdLogFileWriter fileWriter;
    private final JsonMapper jsonMapper;

    public AdLogConsumer(AdLogFileWriter fileWriter, JsonMapper jsonMapper) {
        this.fileWriter = fileWriter;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = {AdTopics.REQUEST, AdTopics.IMPRESSION, AdTopics.CLICK})
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[{}] partition={} offset={} value={}",
                record.topic(), record.partition(), record.offset(), record.value());

        validate(record.value());
        fileWriter.append(record.topic(), record.value());
    }

    /** 잘못된 메시지면 InvalidAdEventException → 재시도 없이 바로 DLQ */
    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAdEventException("빈 메시지");
        }

        JsonNode node;
        try {
            node = jsonMapper.readTree(value);
        } catch (Exception e) {
            throw new InvalidAdEventException("JSON 형식이 아님", e);
        }

        if (!node.isObject()) {
            throw new InvalidAdEventException("JSON 객체가 아님");
        }
        JsonNode eventId = node.get("eventId");
        if (eventId == null || eventId.isNull() || eventId.asString().isBlank()) {
            throw new InvalidAdEventException("eventId 없음");
        }
    }
}
