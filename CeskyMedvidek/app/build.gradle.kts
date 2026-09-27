plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cz.ceskymedvidek.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "cz.berialo.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 12
        versionName = "1.8.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
