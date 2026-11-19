import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    // ---- Targets: one Kotlin codebase, compiled once per target ----
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm() // Extra target so commonMain/commonTest also run as plain JVM tests (fast, no emulator).

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"   // Swift: `import Shared`
            isStatic = true
        }
    }

    // ---- Source sets: commonMain is the hub; platform sets add what only that platform has ----
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)          // StateFlow appears in the public API
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)      // HTTP engine for Android
            implementation(libs.kotlinx.coroutines.android) // Dispatchers.Main on Android
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)      // HTTP engine for iOS (Apple's URLSession)
        }
    }
}

android {
    namespace = "com.rakesh1308.hnreader.shared"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
