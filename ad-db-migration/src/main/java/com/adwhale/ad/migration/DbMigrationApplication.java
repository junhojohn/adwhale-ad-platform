package com.adwhale.ad.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * DB 스키마 마이그레이션 앱.
 *
 * Spring Boot가 시작하면서 Flyway를 자동 실행 → src/main/resources/db/migration/V*.sql 중
 * 아직 적용 안 된 파일만 버전 순서대로 실행한다. 할 일이 끝나면 앱은 그대로 종료된다
 * (웹 서버나 스케줄러가 없어서 붙잡고 있는 스레드가 없음).
 *
 * 스키마의 "주인"을 이 앱 하나로 정해두고, 다른 앱(ad-log-consumer, ad-batch, ad-dashboard-api)은
 * 테이블을 쓰기만 한다 → 여러 앱이 각자 스키마를 바꾸다 충돌하는 일을 막음.
 */
@SpringBootApplication
public class DbMigrationApplication {

    private static final Logger log = LoggerFactory.getLogger(DbMigrationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DbMigrationApplication.class, args);
    }

    /** 마이그레이션이 끝난 뒤 현재 상태를 표로 출력 */
    @Bean
    CommandLineRunner printMigrationInfo(ObjectProvider<Flyway> flywayProvider) {
        return args -> {
            Flyway flyway = flywayProvider.getIfAvailable();
            if (flyway == null) {   // spring.flyway.enabled=false (테스트 등)
                return;
            }
            MigrationInfo current = flyway.info().current();
            log.info("────────── Flyway 마이그레이션 상태 ──────────");
            for (MigrationInfo info : flyway.info().all()) {
                log.info("  V{} | {} | {} | {}",
                        info.getVersion(), info.getDescription(), info.getState(), info.getInstalledOn());
            }
            log.info("현재 스키마 버전: {}", current == null ? "없음" : current.getVersion());
        };
    }
}
