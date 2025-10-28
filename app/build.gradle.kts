plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // ✅ Required for Firebase
}

android {
    namespace = "com.example.sdk"
    compileSdk = 36 // ✅ Updated to latest API as required

    defaultConfig {
        applicationId = "com.example.sdk"
        minSdk = 26
        targetSdk = 36 // ✅ Matches compileSdk for full compatibility
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ✅ Firebase dependencies (via BoM)
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")

    // ✅ Google Fit APIs
    implementation("com.google.android.gms:play-services-fitness:21.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ✅ MPAndroidChart for charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ✅ Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ AndroidX + Material dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ✅ Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
