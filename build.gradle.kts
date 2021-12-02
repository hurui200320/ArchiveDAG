@file:Suppress("GradlePackageUpdate")

import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    }
}

plugins {
    id("java")
    id("idea")
    id("org.springframework.boot") version "2.6.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.protobuf") version "0.8.18"
    id("com.adarshr.test-logger") version "3.0.0"
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.spring") version "1.6.0"
    kotlin("plugin.jpa") version "1.6.0"
}

group = "info.skyblond"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

val grpcVersion = "1.42.1"
val protobufVersion = "3.19.1"
val grpcStarterVersion = "2.12.0.RELEASE"
val postgresqlVersion = "42.3.1"
val multihashVersion = "v1.3.0"
val bouncyCastleVersion = "1.69"
val jjwtVersion = "0.11.2"

val embeddedRedisVersion = "0.7.3"
val h2Version = "1.4.200"

dependencies {
    // basic
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains:annotations")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // spring boot web
    implementation("org.springframework.boot:spring-boot-starter-web")
    // spring boot jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // spring boot security
    implementation("org.springframework.boot:spring-boot-starter-security")
    // spring boot integration and data redis
    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // spring boot configuration
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // spring integration redis
    implementation("org.springframework.integration:spring-integration-redis")

    // grpc spring boot starter
    implementation("net.devh:grpc-spring-boot-starter:$grpcStarterVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    // ssl for grpc
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    // Postgresql jdbc driver
    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")

    // protobuf
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")

    // multihash and crypto
    implementation("com.github.multiformats:java-multihash:$multihashVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    // JJWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // -------------------- TEST --------------------

    // spring boot test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // spring security test
    testImplementation("org.springframework.security:spring-security-test")
    // H2 for test database
    testImplementation("com.h2database:h2:$h2Version")
    // embedded redis
    testImplementation("it.ozimov:embedded-redis:$embeddedRedisVersion") {
        exclude("org.slf4j", "slf4j-simple")
    }
    // grpc test
    testImplementation("io.grpc:grpc-testing:$grpcVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "512M"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
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

testlogger {
    showFullStackTraces = true
    slowThreshold = 5000
}
