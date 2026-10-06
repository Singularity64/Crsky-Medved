plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "cz.berialo.clean"
    compileSdk = 35
    defaultConfig {
        applicationId = "cz.berialo.clean"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }
}
