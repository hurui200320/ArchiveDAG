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

val grpcVersion = "1.44.1"

dependencies {
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
//    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
    implementation(project(":grpc-interface"))
    compileOnly("org.jetbrains:annotations:23.0.0")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
