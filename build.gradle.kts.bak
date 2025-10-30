// Top-level Gradle build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // ✅ Using Version Catalog aliases for Android & Kotlin
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // ✅ Firebase / Google services plugin (for Firestore, Analytics, Auth, etc.)
    id("com.google.gms.google-services") version "4.4.4" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
