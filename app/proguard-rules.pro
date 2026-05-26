# ── Room entities ────────────────────────────────────────────────────────────
-keep class com.example.scamshield.data.db.ThreatEntity { *; }
-keep class com.example.scamshield.data.db.CallEntity { *; }
-keep class com.example.scamshield.data.db.BlockedNumberEntity { *; }
-keep class com.example.scamshield.data.db.TrustedContactEntity { *; }
-keep class com.example.scamshield.data.db.FeedbackEntity { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Retrofit / GSON models ────────────────────────────────────────────────────
-keep class com.example.scamshield.model.ScamAnalysisRequest { *; }
-keep class com.example.scamshield.model.ScamAnalysisResponse { *; }
-keep class com.example.scamshield.model.NotificationData { *; }
# Generic GSON rule: keep all serialized/deserialized field names
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Retrofit internals ────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Firebase / Crashlytics ────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-dontwarn com.google.firebase.**

# ── Preserve stack traces for Crashlytics ─────────────────────────────────────
-renamesourcefileattribute SourceFile
