plugins {
    `java-library`
    `maven-publish`
    id("com.google.protobuf") version "0.9.4" apply false
}

// 모든 하위 모듈이 공유하는 버전
//
// ★ grpcVersion 은 net.devh:grpc-spring-boot-starter 3.1.0.RELEASE 가 번들하는
//   버전과 반드시 일치해야 합니다. 어긋나면 런타임에
//   NoClassDefFoundError: io/grpc/InternalGlobalInterceptors 가 납니다.
//   (grpc-api 는 새 버전, grpc-core 는 옛 버전이 섞이면서 발생)
//   스타터를 올릴 때 이 값도 같이 올리세요.
extra["protobufVersion"] = "3.25.5"
extra["grpcVersion"] = "1.63.0"

allprojects {
    group = "com.kakaoclone"
    // 로컬 개발 중에는 SNAPSHOT 고정 — 태그 없이 publishToMavenLocal 반복
    version = System.getenv("RELEASE_VERSION") ?: "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            // 이 PC에 설치된 JDK 는 17. Java 21 로 올리려면 이 값만 바꾸면 됩니다.
            languageVersion.set(JavaLanguageVersion.of(17))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
