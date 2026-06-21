plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ringer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ringer"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.google.android.material:material:1.11.0")
}
