import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot")   // 버전은 루트 build.gradle.kts에서 지정
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    // 대시보드 화면이 호출하는 통계 조회 API. SDK용 ad-api와 분리 (트래픽·장애·인증 정책이 다름)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
