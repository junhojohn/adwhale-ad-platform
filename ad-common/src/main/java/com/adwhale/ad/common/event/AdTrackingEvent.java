package com.adwhale.ad.common.event;

/**
 * Kafka ad-impression / ad-click 토픽에 들어가는 메시지
 *
 * @param eventType "impression" 또는 "click" — 나중에 두 토픽 데이터를 합쳐 볼 때 구분용
 * @param clientTs  SDK에서 이벤트가 발생한 시각
 * @param serverTs  서버가 받은 시각 (clientTs와 차이가 크면 SDK가 오프라인 재전송한 것)
 */
public record AdTrackingEvent(
        String eventId,
        String eventType,
        String requestId,
        String adId,
        String placementId,
        String deviceId,
        long clientTs,
        long serverTs
) {
}
