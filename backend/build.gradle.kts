plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    // basic
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.code.gson:gson")

    // spring boot web
    implementation("org.springframework.boot:spring-boot-starter-web")
    // spring boot jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // spring boot security
    implementation("org.springframework.boot:spring-boot-starter-security")
    // spring boot configuration
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    // this is not required when using kapt
    // but IDEA will complain about it if missing this
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // grpc spring boot starter
    implementation("net.devh:grpc-spring-boot-starter:${Versions.springbootGrpcStarterVersion}")

    // Postgresql jdbc driver
    runtimeOnly("org.postgresql:postgresql:${Versions.postgresqlVersion}")

    // jetcd
    implementation("io.etcd:jetcd-core:${Versions.jetcdVersion}")

    // multihash and crypto
    implementation("com.github.multiformats:java-multihash:${Versions.multihashVersion}")
    implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastleVersion}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${Versions.bouncyCastleVersion}")

    // JJWT
    implementation("io.jsonwebtoken:jjwt-api:${Versions.jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.jjwtVersion}")

    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:${Versions.awsJavaSdkVersion}"))
    implementation("software.amazon.awssdk:s3")

    implementation(project(":grpc-interface"))
    implementation(project(":commons"))

    // -------------------- TEST --------------------

    // spring boot test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // spring security test
    testImplementation("org.springframework.security:spring-security-test")
    // H2 for test database
    testImplementation("com.h2database:h2:${Versions.h2Version}")
    // grpc test
    testImplementation("io.grpc:grpc-testing:${Versions.grpcVersion}")
}
