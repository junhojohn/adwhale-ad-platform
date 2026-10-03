// 루트 빌드 파일: 모든 모듈에 공통으로 적용할 설정만 둔다.

plugins {
    // 버전은 여기서 한 번만 정하고, 실제 적용은 각 앱 모듈(ad-api, ad-log-consumer)에서 한다
    id("org.springframework.boot") version "4.1.1" apply false
}

subprojects {
    apply(plugin = "java")

    group = "com.adwhale.ad"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
