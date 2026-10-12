package com.adwhale.ad.api.service;

import com.adwhale.ad.api.dto.AdRequestBody;
import com.adwhale.ad.api.dto.AdResponse;
import com.adwhale.ad.api.dto.TrackingBody;
import com.adwhale.ad.api.kafka.AdEventProducer;
import com.adwhale.ad.common.kafka.AdTopics;
import com.adwhale.ad.common.event.AdRequestEvent;
import com.adwhale.ad.common.event.AdTrackingEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AdService {

    private final AdEventProducer producer;
    private final double noFillRate;

    public AdService(AdEventProducer producer, @Value("${adwhale.ad.no-fill-rate:0.1}") double noFillRate) {
        this.producer = producer;
        this.noFillRate = noFillRate;
    }

    /** 더미 광고 후보. 실제로는 캠페인/타게팅/입찰로 고름 */
    private static final List<String> DUMMY_ADS = List.of("A001", "A002");

    /**
     * 광고 요청 처리: 광고를 고르고(지금은 더미) → 요청 로그를 Kafka로 보내고 → 응답
     *
     * @return 광고가 있으면 Optional.of(광고), 노필(보여줄 광고 없음)이면 Optional.empty() → 컨트롤러가 204 응답
     */
    public Optional<AdResponse> serve(AdRequestBody body) {
        String requestId = UUID.randomUUID().toString();

        // 노필 흉내: 설정한 비율(기본 10%)만큼 광고 없음. 실제로는 "조건에 맞는 광고가 없을 때" 발생
        boolean noFill = ThreadLocalRandom.current().nextDouble() < noFillRate;

        AdResponse ad = noFill ? null : pickAd(requestId, body.placementId());

        // 노필이어도 요청 로그는 남긴다 (adId = null) → 필레이트 = 광고 응답된 요청 / 전체 요청
        // 요청 로그는 비동기: Kafka가 잠깐 죽어도 광고는 내보낸다 (매출 우선, 요청 로그 일부 유실 허용)
        producer.sendAsync(AdTopics.REQUEST, requestId, new AdRequestEvent(
                UUID.randomUUID().toString(),
                requestId,
                body.placementId(),
                body.appId(),
                body.deviceId(),
                ad == null ? null : ad.adId(),
                System.currentTimeMillis()));

        return Optional.ofNullable(ad);
    }

    private AdResponse pickAd(String requestId, String placementId) {
        String adId = DUMMY_ADS.get(ThreadLocalRandom.current().nextInt(DUMMY_ADS.size()));
        return new AdResponse(
                requestId,
                adId,
                placementId,
                "https://cdn.example.com/creative/" + adId + ".png",
                "https://example.com/landing/" + adId);
    }

    public void trackImpression(TrackingBody body) {
        track(AdTopics.IMPRESSION, "impression", body);
    }

    public void trackClick(TrackingBody body) {
        track(AdTopics.CLICK, "click", body);
    }

    /** 노출/클릭은 동기: 정산에 쓰이는 데이터라 Kafka 저장 실패 시 503 → SDK가 재전송 */
    private void track(String topic, String eventType, TrackingBody body) {
        producer.sendSync(topic, body.requestId(), new AdTrackingEvent(
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
