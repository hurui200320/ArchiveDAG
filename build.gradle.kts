buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("java")
    id("idea")
    id("com.adarshr.test-logger") version "3.1.0"
}

allprojects {
    group = "info.skyblond"
    version = "0.0.1-SNAPSHOT"

    // set testing for all project
    apply {
        plugin("com.adarshr.test-logger")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        minHeapSize = "1024M"
        maxHeapSize = "8192M"
    }

    testlogger {
        showFullStackTraces = true
        slowThreshold = 5000
    }
}

subprojects {
    // set Java for all sub projects

    apply {
        plugin("java")
        plugin("idea")
    }

    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17

    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }

}

