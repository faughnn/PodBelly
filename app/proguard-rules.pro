# Keep annotation attributes for runtime reflection
-keepattributes *Annotation*

# Keep Moshi-generated adapters and network model classes
-keep class com.podbelly.core.network.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Kotlin metadata (needed for Moshi code-gen and reflection)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Media3 / ExoPlayer ─────────────────────────────────────────────────
# Keep PlaybackService so the media notification can find it.
# (Pocket Casts has an equivalent rule for their PlaybackService.)
-keep class com.podbelly.core.playback.PlaybackService { *; }

# Keep all Media3 session and exoplayer classes used via reflection
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }

# Keep MediaSessionService metadata
-keep class * extends androidx.media3.session.MediaSessionService { *; }

# Suppress warnings for Media3 internal classes
-dontwarn androidx.media3.**
