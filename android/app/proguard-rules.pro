# Preserve line numbers for readable crash reports (Sentry / Play Console)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class dev.abbasian.data.remote.dto.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
