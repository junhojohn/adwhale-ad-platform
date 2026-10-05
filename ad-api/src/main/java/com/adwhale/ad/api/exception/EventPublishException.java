package com.adwhale.ad.api.exception;

/**
 * 이벤트를 Kafka에 저장하지 못했을 때. API는 503으로 응답하고 SDK는 잠시 후 재전송한다.
 */
public class EventPublishException extends RuntimeException {

    public EventPublishException(String topic, Throwable cause) {
        super("Kafka 전송 실패: " + topic, cause);
    }
}
