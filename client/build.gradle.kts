plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin")
}

javafx {
    version = Versions.javaFxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("info.skyblond.archivedag.apwiho.Main")
    executableDir = ""
}

dependencies {
    runtimeOnly("io.grpc:grpc-netty-shaded:${Versions.grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${Versions.grpcVersion}")
    implementation("io.grpc:grpc-stub:${Versions.grpcVersion}")
    implementation(project(":grpc-interface"))
    implementation(project(":commons"))
    implementation("com.github.multiformats:java-multihash:${Versions.multihashVersion}")
    implementation("ch.qos.logback:logback-classic:${Versions.logbackVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Versions.junitVersion}")
}
