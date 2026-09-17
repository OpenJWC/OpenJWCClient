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

-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

-keep class * {
    @androidx.room.Database *;
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-keep class **.*_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * {
    @androidx.room.TypeConverter *;
}

-dontwarn androidx.room.paging.**

-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

-keep class org.openjwc.client.net.models.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

-keepclassmembers class org.openjwc.client.ui.timetable.load.WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# QuickJS 脚本桥：JS 侧按「方法名」调用（http.get / dom.query / params.crawlCutoffDate / util.resolveUrl…），
# 而绑定走的是反射（QuickJs.set 用 Class.getMethods() 取名字）。R8 会重命名/移除未保留的接口方法，
# 导致 release 包里脚本报 "xxx is not a function"。必须保留接口及其方法名。
-keep interface org.openjwc.client.script.ScriptHttpApi { *; }
-keep interface org.openjwc.client.script.ScriptHtmlApi { *; }
-keep interface org.openjwc.client.script.ScriptUtilApi { *; }
-keep interface org.openjwc.client.script.ScriptConsoleApi { *; }
-keep interface org.openjwc.client.script.ScriptReportApi { *; }
-keep interface org.openjwc.client.script.ScriptParamsApi { *; }
