plugins {
    id("com.google.protobuf")
}

val protobufVersion: String by rootProject.extra
val grpcVersion: String by rootProject.extra

dependencies {
    // api 로 노출 — 소비 리포(gateway/chat/api)가 별도 선언 없이 쓸 수 있게
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    api("com.google.protobuf:protobuf-java-util:$protobufVersion")   // JsonFormat
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-stub:$grpcVersion")

    // gRPC 생성 코드가 참조하는 @Generated
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

sourceSets {
    main {
        proto {
            // ★ 스키마 단일 원천. gen-ts 도 같은 디렉토리를 읽습니다.
            srcDir("${rootDir}/schema")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}
