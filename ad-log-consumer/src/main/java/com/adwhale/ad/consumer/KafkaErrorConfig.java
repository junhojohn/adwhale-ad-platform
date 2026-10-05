package com.adwhale.ad.consumer;

import com.adwhale.ad.common.kafka.AdTopics;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Consumer 에러 처리 규칙
 *
 *   consume()에서 예외 발생
 *     ├─ InvalidAdEventException (메시지가 잘못됨)  → 재시도 없이 즉시 DLQ
 *     └─ 그 외 (디스크 오류 등 일시적일 수 있음)    → 1초 간격 2번 재시도(총 3번) → 그래도 실패하면 DLQ
 *
 *   DLQ로 보낸 뒤에는 offset을 넘겨서 다음 메시지를 계속 처리한다.
 *   → 메시지 하나 때문에 Consumer 전체가 멈추는 일(poison pill)을 막는다.
 *
 * 이 빈을 등록하면 Spring Boot가 모든 @KafkaListener에 자동으로 적용한다.
 */
@Configuration
public class KafkaErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        // 실패한 메시지를 원래 토픽 이름 + "-dlq" 토픽으로 보냄 (파티션 -1 = Kafka가 알아서 지정)
        // 원본 토픽/파티션/offset, 예외 메시지 등은 Kafka 헤더(kafka_dlt-*)에 자동으로 붙음
        DeadLetterPublishingRecoverer dlqPublisher = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(AdTopics.dlq(record.topic()), -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, ex) -> {
                    log.warn("DLQ로 이동 [{}] partition={} offset={} 원인={} value={}",
                            record.topic(), record.partition(), record.offset(),
                            rootMessage(ex), record.value());
                    dlqPublisher.accept(record, ex);
                },
                new FixedBackOff(1000L, 2L));   // 1초 간격, 재시도 2번

        handler.addNotRetryableExceptions(InvalidAdEventException.class);
        return handler;
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
