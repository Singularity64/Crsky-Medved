plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "cz.ceskymedvidek.app"
    compileSdk = 35
    defaultConfig { applicationId = "cz.ceskymedvidek.app"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "0.1" }
}
