# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /tools/proguard/proguard-android-optimize.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# If you use Gson, you might need rules like this:
#-keep class com.google.gson.stream.** { *; }
#-keep class com.yourpackage.yourmodel.** { *; } # Keep your model classes

# If you use Retrofit/OkHttp, you might need rules for models and interfaces.

# If you use Room, specific rules might be needed if you use advanced features.

# Default rules provided by Android Gradle Plugin are usually sufficient for common libraries.

# 保留所有 ContentProvider 的子类及其构造函数
# -keep public class * extends android.content.ContentProvider {
#     <init>();
# }

# --- 添加更强力的 AppContentProvider 保留规则 ---
-keep public class com.mobileplatform.creator.data.AppContentProvider {
    public <init>(); # 显式保留构造函数
    *; # 保留所有其他成员
}

# --- 保留 FileProvider (通常是必要的) ---
# -keep class androidx.core.content.FileProvider { *; }
-keep public class androidx.core.content.FileProvider {
    public <init>();
    *;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile 