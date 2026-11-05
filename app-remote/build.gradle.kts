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
        versionCode = 110
        versionName = "1.10"
    }

    signingConfigs {
        getByName("debug") {
            val keystoreFile = rootProject.file("anypay-release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "anypay123456"
                keyAlias = "anypay_key"
                keyPassword = "anypay123456"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Şefim POS MS SQL Sürücüsü
    implementation("net.sourceforge.jtds:jtds:1.3.1")
}
