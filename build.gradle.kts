import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.compose") version "1.10.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    jvmToolchain(17)
}

group = "de.thake"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    
    // Explicitly add macos targets if you want to cross-compile or be explicit, 
    // but currentOs is usually enough for local dev. 
    // To align with "compose.material notation", let's use the accessors if possible, 
    // although they might be deprecated. The user explicitly asked for alignment.
    // If we use specific versions we risk misalignment.
    
    implementation(compose.material)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0") // Upgraded to 1.10.0
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("androidx.collection:collection:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")
    implementation("org.apache.logging.log4j:log4j-core:2.25.3")
    implementation("org.apache.logging.log4j:log4j-api:2.25.3")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.3")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")

    // Testing
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BetreuungXMLTool"
            packageVersion = "1.0.0"
        }
    }
}
