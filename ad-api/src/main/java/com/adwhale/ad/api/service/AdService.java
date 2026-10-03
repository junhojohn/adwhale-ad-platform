package com.adwhale.ad.api.service;

import com.adwhale.ad.api.dto.AdRequestBody;
import com.adwhale.ad.api.dto.AdResponse;
import com.adwhale.ad.api.dto.TrackingBody;
import com.adwhale.ad.api.kafka.AdEventProducer;
import com.adwhale.ad.common.kafka.AdTopics;
import com.adwhale.ad.common.event.AdRequestEvent;
import com.adwhale.ad.common.event.AdTrackingEvent;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdService {

    private final AdEventProducer producer;

    public AdService(AdEventProducer producer) {
        this.producer = producer;
    }

    /**
     * 광고 요청 처리: 광고를 고르고(지금은 고정 더미) → 요청 로그를 Kafka로 보내고 → 응답
     */
    public AdResponse serve(AdRequestBody body) {
        String requestId = UUID.randomUUID().toString();

        // TODO: 실제로는 캠페인/타게팅/입찰로 광고를 고름. 지금은 고정 광고 하나
        AdResponse ad = new AdResponse(
                requestId,
                "A001",
                body.placementId(),
                "https://cdn.example.com/creative/A001.png",
                "https://example.com/landing/A001");

        producer.send(AdTopics.REQUEST, requestId, new AdRequestEvent(
                UUID.randomUUID().toString(),
                requestId,
                body.placementId(),
                body.appId(),
                body.deviceId(),
                ad.adId(),
                System.currentTimeMillis()));

        return ad;
    }

    public void trackImpression(TrackingBody body) {
        track(AdTopics.IMPRESSION, "impression", body);
    }

    public void trackClick(TrackingBody body) {
        track(AdTopics.CLICK, "click", body);
    }

    private void track(String topic, String eventType, TrackingBody body) {
        producer.send(topic, body.requestId(), new AdTrackingEvent(
                body.eventId(),
                eventType,
                body.requestId(),
                body.adId(),
                body.placementId(),
                body.deviceId(),
                body.clientTs(),
                System.currentTimeMillis()));
    }
}
