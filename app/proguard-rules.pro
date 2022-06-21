# Retrofit
# =====================
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# Moshi
# =====================
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* *;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Room
# =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# SQLCipher
# ==========
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.* { *; }


# Neurotechnology
# =====================
-keepclasseswithmembers class * {
    @com.neurotec.* <methods>;
}
-keep class com.sun.jna.* {
    *;
}
-keep class com.neurotec.** {
    *;
}

# Cookie Repository
# =====================
-keep class com.jnj.vaccinetracker.common.data.models.SerializableCookie {
    *;
}

# SQLCipher
# =====================
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }