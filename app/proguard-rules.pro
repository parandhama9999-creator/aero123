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


# --- AeroFast Pro release hardening ---

# Saved sessions store enum names (fastener type, standard, class, coating): keep them readable by valueOf()
-keepclassmembers enum com.aistudio.aerofast.trmst.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# Data classes serialized to JSON via Moshi — keep their fields
-keep class com.aistudio.aerofast.trmst.data.** { *; }
-keep class com.aistudio.aerofast.trmst.config.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.** { *; }

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * { @com.squareup.moshi.Json <fields>; }
-keep @com.squareup.moshi.JsonQualifier @interface *

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin metadata for Compose stability
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# Move every obfuscated class into one flat package and keep line numbers for Play crash deobfuscation
-repackageclasses ''
-allowaccessmodification
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip debug / verbose logging from the release build
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
