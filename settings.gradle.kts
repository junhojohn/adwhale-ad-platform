plugins {
    // JDK 25가 Mac에 없으면 Gradle이 자동으로 내려받아 사용하도록 해주는 플러그인
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "adwhale-ad-platform"

include(
    "ad-common",        // 공통 코드 (토픽 이름, Kafka 메시지 형식)
    "ad-api",           // 광고 API 서버 (Producer)
    "ad-log-consumer",  // 로그 저장 앱 (Consumer)
    "ad-batch",         // 집계 배치 (ad_event → ad_stats_hourly)
)
