# ------------------------------------------------------------------------------
# kotlinx.serialization
#
# kotlinx.serialization looks up a generated `$serializer` class and a
# `Companion` object by reflection the first time a @Serializable type is
# touched. R8 doesn't see those references, so without these rules the
# generated serializers are stripped and every @Serializable type crashes at
# runtime with a SerializationException. Scoped to the project package so we
# don't hold library internals.
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.garyfimo.batmancollection.**$$serializer { *; }
-keepclassmembers class com.garyfimo.batmancollection.** {
    *** Companion;
}
-keepclasseswithmembers class com.garyfimo.batmancollection.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable data objects (HomeKey, AddFigureKey, LoginKey, …) survive as
# their generated $serializer, but navigation3 also stores them as live
# singletons on the back stack and compares by identity. Keep the `INSTANCE`
# field so the object survives shrinking even when no other code references it
# by name.
-keepclassmembers @kotlinx.serialization.Serializable class com.garyfimo.batmancollection.** {
    public static ** INSTANCE;
}

# ------------------------------------------------------------------------------
# AndroidX Navigation3
#
# Navigation3 1.2.0 ships consumer rules, but NavKey implementations are the
# user's own types — nothing but the serializer references them by name, so
# keep the whole hierarchy to be safe. Class-only (no `{ *; }`) so members
# still shrink.
# ------------------------------------------------------------------------------
-keep class * implements androidx.navigation3.runtime.NavKey
