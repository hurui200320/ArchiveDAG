import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:${Versions.protobufGradlePluginVersion}")
    }
}

// All plugins' version are listed here
// See buildSrc for version management
plugins {
    // for all projects
    id("java")
    id("idea")
    id("com.adarshr.test-logger") version Versions.testLoggerPluginVersion
    kotlin("jvm") version Versions.kotlinVersion

    // only for certain subprojects
    kotlin("plugin.spring") version Versions.kotlinVersion apply false
    kotlin("plugin.jpa") version Versions.kotlinVersion apply false
    kotlin("kapt") version Versions.kotlinVersion apply false
    id("com.google.protobuf") version Versions.protobufGradlePluginVersion apply false
    id("org.openjfx.javafxplugin") version Versions.javaFxPluginVersion apply false
    id("org.springframework.boot") version Versions.springbootPluginVersion apply false
    id("io.spring.dependency-management") version Versions.springDependencyManagementPluginVersion apply false
}

allprojects {
    group = "info.skyblond"
    version = "0.0.1-SNAPSHOT"

    apply {
        plugin("java")
        plugin("idea")
        plugin("com.adarshr.test-logger")
        plugin("org.jetbrains.kotlin.jvm")
    }

    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17

    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains:annotations")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        minHeapSize = "1024M"
        maxHeapSize = "32768M"
    }

    testlogger {
        showFullStackTraces = true
        slowThreshold = 5000
    }
}
