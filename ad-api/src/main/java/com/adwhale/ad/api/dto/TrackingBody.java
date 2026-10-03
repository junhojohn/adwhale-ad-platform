package com.adwhale.ad.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * SDK → 서버: 노출/클릭 이벤트 (POST /v1/ad/impression, /v1/ad/click)
 *
 * @param eventId     SDK가 이벤트마다 새로 만드는 UUID. 재전송·중복 저장 시 집계에서 한 번만 세기 위한 키
 * @param requestId   광고 응답에서 받은 값. 요청 → 노출 → 클릭을 하나로 이어주는 키
 * @param adId        노출/클릭된 광고 ID
 * @param placementId 광고 지면 ID
 * @param deviceId    기기 식별자
 * @param clientTs    이벤트가 SDK에서 실제로 발생한 시각 (epoch millis)
 */
public record TrackingBody(
        @NotBlank String eventId,
        @NotBlank String requestId,
        @NotBlank String adId,
        @NotBlank String placementId,
        @NotBlank String deviceId,
        @NotNull Long clientTs
) {
}
