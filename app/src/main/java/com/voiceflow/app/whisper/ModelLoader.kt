package com.voiceflow.app.whisper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies the bundled `ggml-tiny.en.bin` from assets to [Context.filesDir] on first launch.
 * whisper.cpp requires a filesystem path, not an InputStream.
 *
 * Returns the absolute path to the extracted model file.
 */
object ModelLoader {

    private const val ASSET_NAME = "ggml-tiny.en.bin"

    suspend fun ensureModel(context: Context): String = withContext(Dispatchers.IO) {
        val dest = File(context.filesDir, ASSET_NAME)
        if (!dest.exists() || dest.length() == 0L) {
            // Copy to a temp file first so an interrupted copy never leaves a
            // truncated model behind that would be treated as valid next launch.
            val tmp = File(context.filesDir, "$ASSET_NAME.tmp")
            context.assets.open(ASSET_NAME).use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                error("Could not move model into place")
            }
        }
        dest.absolutePath
    }
}
