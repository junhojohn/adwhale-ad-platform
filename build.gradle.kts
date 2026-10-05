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

    // 이 모듈을 빌드한 JDK(25)의 java 실행 파일 경로를 출력 → scripts/run-api.sh 에서 java -jar 실행할 때 사용
    // (터미널의 기본 java 버전과 상관없이 항상 빌드와 같은 JDK로 실행하기 위함)
    val javaLauncher = extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
    }
    tasks.register("printJavaLauncher") {
        val launcher = javaLauncher   // ↑ 프로젝트(모듈)의 toolchain 서비스에서 가져옴 (task 안의 extensions는 task 자신의 것이라 없음)
        doLast {
            println(launcher.get().executablePath.asFile.absolutePath)
        }
    }
}
