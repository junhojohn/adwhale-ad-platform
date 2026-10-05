import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot")   // 버전은 루트 build.gradle.kts에서 지정
}

dependencies {
    implementation(project(":ad-common"))   // 토픽 이름, Kafka 메시지 형식

    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    // 웹(HTTP) 의존성 없음 → 포트를 열지 않는 순수 Consumer 앱. 여러 개 동시에 띄워도 포트 충돌 없음
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-jackson")   // 메시지가 올바른 JSON인지 검사

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
