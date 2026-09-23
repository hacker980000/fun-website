# Stage 25.6 production shrinker policy.
# Keep line information for retrace while replacing source filenames in release APK/AAB.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp can optionally integrate with Conscrypt when present; it is not required here.
-dontwarn org.conscrypt.**
