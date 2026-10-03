import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot")   // 버전은 루트 build.gradle.kts에서 지정
}

dependencies {
    implementation(project(":ad-common"))   // 토픽 이름, Kafka 메시지 형식

    // Spring Boot BOM: 아래 starter들의 버전을 Spring Boot 버전에 맞춰 자동으로 정해줌 (Android의 Compose BOM과 같은 개념)
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")     // HTTP API (내장 Tomcat)
    implementation("org.springframework.boot:spring-boot-starter-actuator")   // /actuator/health 같은 운영용 엔드포인트
    implementation("org.springframework.boot:spring-boot-starter-kafka")      // Kafka Producer (spring-kafka)
    implementation("org.springframework.boot:spring-boot-starter-validation") // 요청 값 검증 (@NotBlank 등)

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
