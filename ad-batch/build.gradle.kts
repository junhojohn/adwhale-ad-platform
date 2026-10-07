import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot")   // 버전은 루트 build.gradle.kts에서 지정
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    // 웹 없음: 정해진 주기마다 SQL을 실행하는 배치 전용 앱
    implementation("org.springframework.boot:spring-boot-starter")         // 스케줄러(@Scheduled) 포함 기본 스타터
    implementation("org.springframework.boot:spring-boot-starter-jdbc")    // JdbcTemplate + HikariCP
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
