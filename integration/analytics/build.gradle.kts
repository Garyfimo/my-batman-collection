plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.garyfimo.batmancollection.integration.analytics"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
}

dependencies {
    val firebaseBom = platform(libs.firebase.bom)
    api(firebaseBom)
    api(libs.firebase.analytics)

    testImplementation(libs.junit)
}
