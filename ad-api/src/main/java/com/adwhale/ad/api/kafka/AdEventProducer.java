package com.adwhale.ad.api.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 이벤트 객체를 JSON 문자열로 바꿔 Kafka 토픽에 넣는다.
 *
 * key = requestId → 같은 광고 요청의 요청·노출·클릭 이벤트는 항상 같은 파티션으로 감 (파티션 안에서는 순서 보장)
 */
@Component
public class AdEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AdEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public AdEventProducer(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void send(String topic, String key, Object event) {
        String json = jsonMapper.writeValueAsString(event);

        // send()는 비동기: 메시지를 내부 버퍼에 넣고 바로 리턴. 실제 전송 결과는 나중에 콜백으로 옴
        kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
            if (ex != null) {
                // 실무에선 여기서 실패 이벤트를 로컬 파일에 따로 쌓아두고 재전송하기도 함
                log.error("Kafka 전송 실패 topic={} key={} value={}", topic, key, json, ex);
            } else {
                log.debug("Kafka 전송 성공 topic={} partition={} offset={}",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
