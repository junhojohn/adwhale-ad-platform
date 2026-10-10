import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot")   // 버전은 루트 build.gradle.kts에서 지정
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    // DB 스키마 마이그레이션 전용 앱: 실행 → 아직 안 돌린 V*.sql 적용 → 종료
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.flywaydb:flyway-mysql")     // Flyway의 MySQL 지원 모듈 (Flyway 10부터 DB별로 분리됨)
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
