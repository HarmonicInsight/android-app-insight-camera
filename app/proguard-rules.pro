# CameraX - keep extension mode detection via reflection
-keep class androidx.camera.** { *; }

# Compose
-dontwarn androidx.compose.**

# Coil - keep image decoder implementations
-keep class coil.** { *; }
-dontwarn coil.**

# Accompanist
-dontwarn com.google.accompanist.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep MediaStore content values used via reflection
-keep class android.provider.MediaStore$* { *; }
