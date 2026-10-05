package com.adwhale.ad.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버가 떠 있는지 확인하는 가장 단순한 엔드포인트.
 * 로드밸런서 뒤에서 어느 인스턴스가 응답했는지 알 수 있도록 포트 번호를 같이 돌려준다.
 * curl http://localhost:8080/ping  →  pong (port=8081)
 */
@RestController
public class PingController {

    private final String port;

    public PingController(@Value("${server.port}") String port) {
        this.port = port;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong (port=" + port + ")";
    }
}
