package com.adwhale.ad.batch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 테스트에서는 스케줄러를 끔 → MySQL이 꺼져 있어도 테스트 통과
@SpringBootTest(properties = "adwhale.batch.enabled=false")
class AdBatchApplicationTests {

    @Test
    void contextLoads() {
    }
}
