plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "cz.berialo.clean"
    compileSdk = 35
    defaultConfig {
        applicationId = "cz.berialo.clean"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
