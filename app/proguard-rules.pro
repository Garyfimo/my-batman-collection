# Keep Kotlinx Serialization metadata for @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.garyfimo.batmancollection.**$$serializer { *; }
-keepclassmembers class com.garyfimo.batmancollection.** {
    *** Companion;
}
-keepclasseswithmembers class com.garyfimo.batmancollection.** {
    kotlinx.serialization.KSerializer serializer(...);
}
