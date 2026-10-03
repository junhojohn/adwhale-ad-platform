package com.adwhale.ad.common.event;

/**
 * Kafka ad-request 토픽에 들어가는 메시지 (JSON으로 변환되어 한 줄로 저장됨)
 */
public record AdRequestEvent(
        String eventId,
        String requestId,
        String placementId,
        String appId,
        String deviceId,
        String adId,       // 이 요청에 응답한 광고 (없으면 null = 노필)
        long serverTs
) {
}
