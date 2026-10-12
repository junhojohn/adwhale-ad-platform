package com.adwhale.ad.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 광고 통계 대시보드용 조회 API (스펙: docs/dashboard-spec.md)
 * ad_stats_hourly / placement_stats_hourly 를 읽기만 한다.
 */
@SpringBootApplication
public class DashboardApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApiApplication.class, args);
    }
}
