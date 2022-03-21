import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.jpa") version "1.6.10"
    kotlin("kapt") version "1.6.10"
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val grpcVersion = "1.44.1"
val grpcStarterVersion = "2.13.1.RELEASE"
val postgresqlVersion = "42.3.3"
val multihashVersion = "v1.3.0"
val bouncyCastleVersion = "1.70"
val jjwtVersion = "0.11.2"
val awsJavaSdkVersion = "2.17.100"
val jetcdVersion = "0.6.1"

//val h2Version = "2.1.210"
val h2Version = "1.4.200"

dependencies {
    // basic
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains:annotations")
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
    implementation("net.devh:grpc-spring-boot-starter:$grpcStarterVersion")

    // Postgresql jdbc driver
    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")

    // jetcd
    implementation("io.etcd:jetcd-core:$jetcdVersion")

    // multihash and crypto
    implementation("com.github.multiformats:java-multihash:$multihashVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    // JJWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:$awsJavaSdkVersion"))
    implementation("software.amazon.awssdk:s3")

    implementation(project(":grpc-interface"))
    implementation(project(":commons"))

    // -------------------- TEST --------------------

    // spring boot test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // spring security test
    testImplementation("org.springframework.security:spring-security-test")
    // H2 for test database
    testImplementation("com.h2database:h2:$h2Version")
    // grpc test
    testImplementation("io.grpc:grpc-testing:$grpcVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}
