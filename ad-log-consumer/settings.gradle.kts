plugins {
    // JDK 25가 Mac에 없으면 Gradle이 자동으로 내려받아 사용하도록 해주는 플러그인
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ad-log-consumer"
