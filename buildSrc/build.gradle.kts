plugins {
    id("java")
    kotlin("jvm") version "1.6.20"
}

group = "info.skyblond"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
