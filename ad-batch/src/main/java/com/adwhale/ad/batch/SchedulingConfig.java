package com.adwhale.ad.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 를 켜는 설정. 테스트에서는 adwhale.batch.enabled=false 로 꺼서 DB 없이도 테스트가 돌게 한다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "adwhale.batch.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
