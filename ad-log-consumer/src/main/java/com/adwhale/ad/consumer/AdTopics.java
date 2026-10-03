package com.adwhale.ad.consumer;

/**
 * Kafka 토픽 이름 모음. docker-compose.yml의 kafka-init에서 만든 토픽과 이름이 같아야 한다.
 * ad-api의 AdTopics와 값이 같아야 한다 (앱이 분리되어 각자 복사본을 가짐).
 */
public final class AdTopics {

    public static final String REQUEST = "ad-request";
    public static final String IMPRESSION = "ad-impression";
    public static final String CLICK = "ad-click";

    private AdTopics() {
    }
}
