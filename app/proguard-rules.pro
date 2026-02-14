# CameraX Extensions - keep extension mode detection via reflection
# (core/camera2/lifecycle/view/video は AAR の consumer rules で処理される)
-keep class androidx.camera.extensions.** { *; }

# Compose
-dontwarn androidx.compose.**

# Coil - AAR の consumer rules で処理される
-dontwarn coil.**

# Accompanist
-dontwarn com.google.accompanist.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep MediaStore content values used via reflection
-keep class android.provider.MediaStore$* { *; }
