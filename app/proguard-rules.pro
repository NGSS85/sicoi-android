# Add project specific ProGuard rules here.

# Supabase/Ktor
-keep class io.github.jan.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class br.com.sicoi.mobile.**$$serializer { *; }
-keepclassmembers class br.com.sicoi.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.sicoi.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# Data classes
-keep class br.com.sicoi.mobile.data.model.** { *; }
