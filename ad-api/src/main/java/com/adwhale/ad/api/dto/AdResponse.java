package com.adwhale.ad.api.dto;

/**
 * 서버 → SDK: 광고 응답
 * SDK는 여기서 받은 requestId / adId 를 노출·클릭 이벤트에 그대로 담아 보낸다.
 */
public record AdResponse(
        String requestId,
        String adId,
        String placementId,
        String creativeUrl,
        String landingUrl
) {
}
