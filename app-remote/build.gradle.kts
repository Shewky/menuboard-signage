plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.anypay.remote"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anypay.remote"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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

    // QR Tarayıcı
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // HTTP İletişimi & Dosya Yükleme (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Ayrıştırma
    implementation("com.google.code.gson:gson:2.10.1")
implementation("com.github.bumptech.glide:glide:4.16.0")
// CameraX Kütüphaneleri
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
}
