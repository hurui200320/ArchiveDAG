import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.17")
    }
}

plugins {
    id("java")
    id("idea")
    kotlin("jvm") version "1.5.31"
    id("org.springframework.boot") version "2.5.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.5.31"
    kotlin("plugin.jpa") version "1.5.31"
    id("com.google.protobuf") version "0.8.17"
    id("com.adarshr.test-logger") version "3.0.0"
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

dependencies {
    // basic
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains:annotations:22.0.0")
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")
    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    // protobuf
    implementation("com.google.protobuf:protobuf-java:3.19.1")
    implementation("com.google.protobuf:protobuf-java-util:3.19.1")
    // multihash and crypto
    implementation("com.github.multiformats:java-multihash:v1.3.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    // spring boot web
    implementation("org.springframework.boot:spring-boot-starter-web:2.5.6")
    // spring boot jpa
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.5.6")
    runtimeOnly("org.postgresql:postgresql:42.3.1")
    // redis lock
    implementation("org.springframework.boot:spring-boot-starter-integration:2.5.6")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:2.5.6")
    implementation("org.springframework.integration:spring-integration-redis:5.5.4")
    // spring boot configuration
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:2.5.6")
    // spring boot test
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.6")
    // springfox swagger
    implementation("io.springfox:springfox-boot-starter:3.0.0")
    implementation("io.springfox:springfox-swagger-ui:3.0.0")

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
    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:3.19.1"
        }
    }
}

testlogger {
    showFullStackTraces = true
    slowThreshold = 5000
}
