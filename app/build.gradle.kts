plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // ✅ Required for Firebase
}

android {
    namespace = "com.vxsudev.androidsdk"
    compileSdk = 36 // Android 15+ for AndroidX 1.17.x compatibility

    defaultConfig {
        applicationId = "com.vxsudev.androidsdk"
        minSdk = 26
        targetSdk = 36 // Matches compileSdk for full compatibility
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO: Configure signing before release:
            // 1. Generate keystore: keytool -genkey -v -keystore release.jks -alias release -keyalg RSA -keysize 2048 -validity 10000
            // 2. Add keystore file (keep it secure, don't commit to git)
            // 3. Uncomment and configure signingConfig below
            // signingConfig = signingConfigs.getByName("release")
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
