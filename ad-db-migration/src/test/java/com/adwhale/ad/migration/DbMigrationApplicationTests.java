package com.adwhale.ad.migration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 테스트에서는 Flyway를 끔 → MySQL이 꺼져 있어도 테스트 통과
@SpringBootTest(properties = "spring.flyway.enabled=false")
class DbMigrationApplicationTests {

    @Test
    void contextLoads() {
    }
}
