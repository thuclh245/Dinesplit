# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Memory Optimization - Aggressive Shrinking
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Coil image loading optimization
-keep class coil.** { *; }
-dontwarn coil.**
-keepclasseswithmembernames class coil.** { *; }

# Firebase - keep required classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# OkHttp compatibility
-dontwarn com.squareup.okhttp.CipherSuite
-dontwarn com.squareup.okhttp.ConnectionSpec
-dontwarn com.squareup.okhttp.TlsVersion

# Java reflection
-dontwarn java.lang.reflect.AnnotatedType

# Remove unused resources
-dontshrink
-optimizationpasses 5
-verbose

# Optimization passes
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
