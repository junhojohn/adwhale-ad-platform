package com.adwhale.ad.api.kafka;

/**
 * Kafka 토픽 이름 모음. docker-compose.yml의 kafka-init에서 만든 토픽과 이름이 같아야 한다.
 * 4단계에서 Producer(API)도 이 상수를 그대로 쓴다.
 */
public final class AdTopics {

    public static final String REQUEST = "ad-request";
    public static final String IMPRESSION = "ad-impression";
    public static final String CLICK = "ad-click";

    private AdTopics() {
    }
}
