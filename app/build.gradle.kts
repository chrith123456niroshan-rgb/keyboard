import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load local.properties to read API keys securely
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val translationApiKey = localProperties.getProperty("TRANSLATION_API_KEY") ?: ""

android {
    namespace = "com.sintrans.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sintrans.keyboard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Inject the API key into BuildConfig securely
        buildConfigField("String", "TRANSLATION_API_KEY", "\"$translationApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp for networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JUnit for testing
    testImplementation("junit:junit:4.13.2")
}
