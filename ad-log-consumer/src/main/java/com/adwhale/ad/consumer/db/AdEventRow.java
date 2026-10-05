package com.adwhale.ad.consumer.db;

import com.adwhale.ad.common.event.AdRequestEvent;
import com.adwhale.ad.common.event.AdTrackingEvent;
import com.adwhale.ad.common.kafka.AdTopics;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * ad_event 테이블 한 행. Kafka 메시지(JSON) → 이 객체 → INSERT
 */
public record AdEventRow(
        String eventId,
        String eventType,
        String requestId,
        String adId,
        String placementId,
        String appId,
        String deviceId,
        LocalDateTime clientTs,
        LocalDateTime serverTs
) {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 토픽에 맞는 ad-common 이벤트 클래스로 JSON을 읽어서 행으로 변환한다.
     * 필수값이 없거나 JSON이 아니면 null (→ 저장하지 않고 건너뜀)
     */
    public static AdEventRow from(String topic, String json, JsonMapper jsonMapper) {
        try {
            AdEventRow row = switch (topic) {
                case AdTopics.REQUEST -> {
                    AdRequestEvent e = jsonMapper.readValue(json, AdRequestEvent.class);
                    yield new AdEventRow(e.eventId(), "request", e.requestId(), e.adId(), e.placementId(),
                            e.appId(), e.deviceId(), null, toKst(e.serverTs()));
                }
                case AdTopics.IMPRESSION, AdTopics.CLICK -> {
                    AdTrackingEvent e = jsonMapper.readValue(json, AdTrackingEvent.class);
                    String type = topic.equals(AdTopics.IMPRESSION) ? "impression" : "click";
                    yield new AdEventRow(e.eventId(), type, e.requestId(), e.adId(), e.placementId(),
                            null, e.deviceId(), toKst(e.clientTs()), toKst(e.serverTs()));
                }
                default -> null;
            };
            return row != null && row.isValid() ? row : null;
        } catch (RuntimeException e) {   // Jackson 3의 JacksonException은 RuntimeException
            return null;
        }
    }

    private boolean isValid() {
        return notBlank(eventId) && notBlank(requestId) && notBlank(placementId)
                && notBlank(deviceId) && serverTs != null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** epoch millis → KST 시각. 0(값 없음)이면 null */
    private static LocalDateTime toKst(long epochMillis) {
        return epochMillis <= 0 ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), KST);
    }
}
