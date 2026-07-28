# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
}

# Keep local and API data models
-keepclassmembers class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.api.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# OkHttp & Retrofit rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# WorkManager rules
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

