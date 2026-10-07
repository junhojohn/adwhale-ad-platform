package com.adwhale.ad.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 집계 배치 앱. ad_event(원본)를 주기적으로 GROUP BY 해서 ad_stats_hourly(집계)를 갱신한다.
 * HTTP 서버 없이 스케줄러 스레드가 살아 있는 동안 계속 실행된다.
 */
@SpringBootApplication
public class AdBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdBatchApplication.class, args);
    }
}
