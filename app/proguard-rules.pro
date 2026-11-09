# Keep JNI methods — whisper.cpp bridge
-keep class com.voiceflow.app.whisper.WhisperEngine {
    native <methods>;
}

# Keep Hilt-generated components
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}

# Keep Room entities
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep data classes used for IPC Parcelable
-keep class com.voiceflow.app.ipc.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
