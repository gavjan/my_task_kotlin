import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "1.9.0" // Add Kotlin Serialization plugin
}

group = "org.asyncapp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    //implementation("io.ktor:ktor-client-core:2.0.0") // For HTTP requests
    implementation("io.ktor:ktor-client-cio:2.0.0") // For CIO engine

    implementation("io.ktor:ktor-client-content-negotiation:2.0.0") // Content negotiation
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.0") // Kotlinx JSON support

    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "asyncApp"
            packageVersion = "1.0.0"
        }
    }
}
