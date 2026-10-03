import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
}

group = "com.adwhale.ad"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot BOM: 아래 starter들의 버전을 Spring Boot 4.1.1에 맞춰 자동으로 정해줌 (Android의 Compose BOM과 같은 개념)
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")   // HTTP API (내장 Tomcat)
    implementation("org.springframework.boot:spring-boot-starter-actuator") // /actuator/health 같은 운영용 엔드포인트
    implementation("org.springframework.boot:spring-boot-starter-kafka")    // Kafka Producer/Consumer (spring-kafka)

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
