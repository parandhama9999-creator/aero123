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
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}
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

# --- Licence system hardening ---
# Obfuscation raises the cost of reading the licence flow; it does not prevent it, and it is not
# what the system rests on. Authority lives in the Cloud Functions and the database rules, where a
# patched client cannot reach it. These rules keep R8 from breaking Firebase, and strip the
# names an attacker would grep for first.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Firebase and Play Services reflect over these
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Callable function payloads are plain maps, so no model classes need keeping. The licence classes
# themselves carry no secret and are deliberately left fully obfuscated and inlinable.
-assumenosideeffects class android.util.Log {
    public static int w(...);
}
