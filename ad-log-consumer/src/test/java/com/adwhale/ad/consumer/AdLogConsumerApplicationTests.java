package com.adwhale.ad.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 테스트에서는 Kafka Consumer를 자동 시작하지 않음 → Docker(Kafka)가 꺼져 있어도 테스트 통과
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class AdLogConsumerApplicationTests {

    @Test
    void contextLoads() {
    }
}
