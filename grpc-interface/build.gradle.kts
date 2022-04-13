import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf")
}

dependencies {
    implementation("io.grpc:grpc-protobuf:${Versions.grpcVersion}")
    implementation("io.grpc:grpc-stub:${Versions.grpcVersion}")
    implementation("io.grpc:grpc-netty-shaded:${Versions.grpcVersion}")
    implementation("com.google.protobuf:protobuf-java:${Versions.protobufVersion}")
    implementation("javax.annotation:javax.annotation-api:${Versions.javaxAnnotationApiVersion}")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.protobufVersion}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpcVersion}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
