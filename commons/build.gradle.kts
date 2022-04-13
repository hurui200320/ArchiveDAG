plugins {
    id("java")
    kotlin("jvm")
}

dependencies {
    // multihash and crypto
    implementation("com.github.multiformats:java-multihash:${Versions.multihashVersion}")
    implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastleVersion}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${Versions.bouncyCastleVersion}")

    // -------------------- TEST --------------------

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junitVersion}")
}
