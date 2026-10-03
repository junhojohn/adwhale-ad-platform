package com.adwhale.ad.api.controller;

import com.adwhale.ad.api.dto.AdRequestBody;
import com.adwhale.ad.api.dto.AdResponse;
import com.adwhale.ad.api.dto.TrackingBody;
import com.adwhale.ad.api.service.AdService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SDK가 호출하는 광고 API
 *
 * - 필수값이 빠지면 @Valid 가 자동으로 400 Bad Request 응답
 * - 노출/클릭은 Kafka에 넣기만 하고 바로 202 Accepted (= "받았고 처리는 나중에 함")
 */
@RestController
@RequestMapping("/v1/ad")
public class AdController {

    private final AdService adService;

    public AdController(AdService adService) {
        this.adService = adService;
    }

    @PostMapping("/request")
    public AdResponse request(@Valid @RequestBody AdRequestBody body) {
        return adService.serve(body);
    }

    @PostMapping("/impression")
    public ResponseEntity<Void> impression(@Valid @RequestBody TrackingBody body) {
        adService.trackImpression(body);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/click")
    public ResponseEntity<Void> click(@Valid @RequestBody TrackingBody body) {
        adService.trackClick(body);
        return ResponseEntity.accepted().build();
    }
}
