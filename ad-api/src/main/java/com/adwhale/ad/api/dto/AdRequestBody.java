package com.adwhale.ad.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * SDK → 서버: 광고 요청 (POST /v1/ad/request)
 *
 * @param placementId 광고 지면 ID (앱 안의 광고 자리)
 * @param appId       매체 앱 ID
 * @param deviceId    기기 식별자 (ADID 등)
 */
public record AdRequestBody(
        @NotBlank String placementId,
        @NotBlank String appId,
        @NotBlank String deviceId
) {
}
