plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.12"
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("info.skyblond.archivedag.apwiho.Main")
}

val grpcVersion = "1.45.0"
val multihashVersion = "v1.3.0"
val logbackVersion = "1.2.11"

dependencies {
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation(project(":grpc-interface"))
    implementation(project(":commons"))
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation("com.github.multiformats:java-multihash:$multihashVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
