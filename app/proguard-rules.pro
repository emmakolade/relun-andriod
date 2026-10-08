# kotlinx.serialization: keep generated serializers for DTOs and navigation routes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.relun.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.relun.app.**$$serializer { *; }

# Retrofit interfaces are used reflectively.
-keep,allowobfuscation interface com.relun.app.data.network.ApiService
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# socket.io / engine.io
-keep class io.socket.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
