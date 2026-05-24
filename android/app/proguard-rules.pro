# Preserve line numbers for readable crash reports (Sentry / Play Console)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson — generic signatures required for TypeToken.getParameterized
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room + local models
-keep class dev.abbasian.data.local.** { *; }
-keep class dev.abbasian.domain.model.** { *; }

# Retrofit / OkHttp
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep class dev.abbasian.data.remote.dto.** { *; }
-keep class dev.abbasian.data.remote.api.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
