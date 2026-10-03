package com.adwhale.ad.consumer;

import com.adwhale.ad.common.kafka.AdTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 요청/노출/클릭 토픽을 구독해서, 받은 메시지를 그대로 로그 파일에 한 줄씩 저장한다.
 *
 * 흐름: Kafka 토픽 → (spring-kafka가 메시지를 꺼내서) consume() 호출 → AdLogFileWriter로 파일에 append
 * Android로 치면 BroadcastReceiver의 onReceive()가 메시지 올 때마다 불리는 것과 비슷하다.
 */
@Component
public class AdLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AdLogConsumer.class);

    private final AdLogFileWriter fileWriter;

    public AdLogConsumer(AdLogFileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    @KafkaListener(topics = {AdTopics.REQUEST, AdTopics.IMPRESSION, AdTopics.CLICK})
    public void consume(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            return; // 빈 메시지는 저장하지 않음
        }

        log.info("[{}] partition={} offset={} value={}",
                record.topic(), record.partition(), record.offset(), record.value());

        fileWriter.append(record.topic(), record.value());
    }
}
