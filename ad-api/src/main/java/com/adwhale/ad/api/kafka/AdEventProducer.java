package com.adwhale.ad.api.kafka;

import com.adwhale.ad.api.exception.EventPublishException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

/**
 * 이벤트 객체를 JSON 문자열로 바꿔 Kafka 토픽에 넣는다.
 *
 * key = requestId → 같은 광고 요청의 요청·노출·클릭 이벤트는 항상 같은 파티션으로 감 (파티션 안에서는 순서 보장)
 *
 * 전송 방식 2가지
 *  - sendAsync: 결과를 기다리지 않음. 빠르지만 실패해도 호출한 쪽은 모름 (광고 요청 로그용)
 *  - sendSync : Kafka가 "저장 완료"라고 답할 때까지 기다림. 실패하면 예외 → API가 503 응답 (노출/클릭용)
 */
@Component
public class AdEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AdEventProducer.class);

    /** sendSync가 Kafka 응답을 기다리는 최대 시간. 넘기면 실패로 보고 503 */
    private static final long SYNC_TIMEOUT_SECONDS = 3;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public AdEventProducer(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    /** 비동기 전송: 메시지를 내부 버퍼에 넣고 바로 리턴. 결과는 나중에 콜백으로 로그만 남김 */
    public void sendAsync(String topic, String key, Object event) {
        String json = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka 전송 실패(async) topic={} key={} value={}", topic, key, json, ex);
            } else {
                log.debug("Kafka 전송 성공 topic={} partition={} offset={}",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * 동기 전송: Kafka가 저장 완료(acks=all)라고 답할 때까지 최대 3초 기다린다.
     * 가상 스레드 위에서 돌기 때문에 기다리는 동안 OS 스레드를 붙잡지 않는다.
     *
     * 주의: 3초 타임아웃으로 실패 처리했는데 실제로는 그 뒤에 Kafka에 들어가는 경우도 있다.
     *       → SDK가 재전송하면 중복이 생길 수 있고, eventId로 집계 시 걸러낸다.
     */
    public void sendSync(String topic, String key, Object event) {
        String json = jsonMapper.writeValueAsString(event);

        try {
            var result = kafkaTemplate.send(topic, key, json).get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Kafka 전송 성공 topic={} partition={} offset={}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishException(topic, e);
        } catch (Exception e) {
            // ExecutionException(전송 실패), TimeoutException(3초 초과), KafkaException 등
            log.error("Kafka 전송 실패(sync) topic={} key={} value={}", topic, key, json, e);
            throw new EventPublishException(topic, e);
        }
    }
}
