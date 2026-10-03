package com.adwhale.ad.common.kafka;

/**
 * Kafka 토픽 이름 모음. docker-compose.yml의 kafka-init에서 만든 토픽과 이름이 같아야 한다.
 * ad-api(Producer)와 ad-log-consumer(Consumer)가 함께 사용한다.
 */
public final class AdTopics {

    public static final String REQUEST = "ad-request";
    public static final String IMPRESSION = "ad-impression";
    public static final String CLICK = "ad-click";

    private AdTopics() {
    }
}
