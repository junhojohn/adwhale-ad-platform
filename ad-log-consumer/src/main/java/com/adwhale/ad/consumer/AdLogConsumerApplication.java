package com.adwhale.ad.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka 토픽(요청/노출/클릭)을 읽어서 로그 파일로 저장하는 전용 앱.
 * HTTP 서버가 없어서 포트를 쓰지 않고, Kafka 리스너 스레드가 살아 있는 동안 계속 실행된다.
 */
@SpringBootApplication
public class AdLogConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdLogConsumerApplication.class, args);
    }
}
