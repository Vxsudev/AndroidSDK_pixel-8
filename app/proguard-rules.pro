# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep classes accessed via reflection in GoogleFitManager
-keep class com.google.android.gms.fitness.data.HealthDataTypes { *; }
-keepclassmembers class com.google.android.gms.fitness.data.HealthDataTypes {
    public static ** TYPE_BODY_TEMPERATURE;
    public static ** TYPE_OXYGEN_SATURATION;
}

# Keep Google Fit API classes
-keep class com.google.android.gms.fitness.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep data model classes (used with Firebase/Gson)
-keep class com.vxsudev.androidsdk.SmartWatchData { *; }
-keep class com.vxsudev.androidsdk.** { *; }
-keepclassmembers class com.vxsudev.androidsdk.** {
    <fields>;
    <methods>;
}

# Keep Gson specific classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile