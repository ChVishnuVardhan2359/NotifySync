# Moshi / Retrofit
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keep class com.notifysync.app.data.api.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
