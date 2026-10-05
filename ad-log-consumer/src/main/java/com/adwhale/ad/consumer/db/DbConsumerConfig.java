package com.adwhale.ad.consumer.db;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * DB 저장 리스너 전용 설정 (파일 저장 리스너와 에러 처리 방식이 다름)
 *
 *               파일 저장 (KafkaErrorConfig)         DB 저장 (여기)
 *  받는 단위     1건씩                               최대 500건씩 (배치)
 *  실패 시       2번 재시도 후 DLQ                    DB가 살아날 때까지 무한 재시도 (1초 → 2초 → 4초 ... 최대 30초 간격)
 *  이유          메시지 자체가 문제일 수 있음          메시지는 정상, DB가 잠깐 죽은 것 → DLQ로 버리면 안 됨
 */
@Configuration
public class DbConsumerConfig {

    public static final String DB_BATCH_FACTORY = "dbBatchListenerFactory";

    private static final Logger log = LoggerFactory.getLogger(DbConsumerConfig.class);

    @Bean(DB_BATCH_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> dbBatchListenerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(30_000L);   // 재시도 간격은 최대 30초, 횟수 제한 없음

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int attempt) {
                log.warn("DB 저장 실패 → 재시도 {}회째: {}", attempt, rootMessage(ex));
            }

            @Override   // 배치 리스너는 이 메서드가 호출됨
            public void failedDelivery(ConsumerRecords<?, ?> records, Exception ex, int attempt) {
                log.warn("DB 저장 실패({}건 배치) → 재시도 {}회째: {}", records.count(), attempt, rootMessage(ex));
            }
        });

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);                // List<ConsumerRecord>로 여러 건을 한 번에 받음
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
