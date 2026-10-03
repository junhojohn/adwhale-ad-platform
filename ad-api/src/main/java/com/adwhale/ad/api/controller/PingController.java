package com.adwhale.ad.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버가 떠 있는지 확인하는 가장 단순한 엔드포인트.
 * curl http://localhost:8080/ping  →  pong
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
