#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "VoiceFlowJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifndef WHISPER_STUB
#include "include/whisper.h"
#endif

extern "C" {

/**
 * Loads a whisper GGML model from the given file path.
 * Returns a native context handle (cast to jlong) on success, or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_com_voiceflow_app_whisper_WhisperEngine_initModel(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPath) {

#ifdef WHISPER_STUB
    LOGI("STUB: initModel called");
    return 1L; // non-zero stub handle
#else
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) {
        LOGE("initModel: failed to get model path string");
        return 0L;
    }

    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false; // Android — CPU inference only

    struct whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("initModel: whisper_init_from_file_with_params returned null");
        return 0L;
    }

    LOGI("initModel: context initialised successfully");
    return reinterpret_cast<jlong>(ctx);
#endif
}

/**
 * Transcribes a float PCM buffer (16kHz mono) using the given context handle.
 * Returns the transcribed text as a Java String.
 */
JNIEXPORT jstring JNICALL
Java_com_voiceflow_app_whisper_WhisperEngine_transcribe(
        JNIEnv *env,
        jobject /* this */,
        jlong ctxHandle,
        jfloatArray pcmSamples) {

#ifdef WHISPER_STUB
    LOGI("STUB: transcribe called");
    return env->NewStringUTF("[stub transcription]");
#else
    if (ctxHandle == 0L) {
        LOGE("transcribe: null context handle");
        return env->NewStringUTF("");
    }

    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(ctxHandle);

    jsize sampleCount = env->GetArrayLength(pcmSamples);
    jfloat *samples = env->GetFloatArrayElements(pcmSamples, nullptr);
    if (samples == nullptr) {
        LOGE("transcribe: failed to get PCM array elements");
        return env->NewStringUTF("");
    }

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress   = false;
    wparams.print_special    = false;
    wparams.print_realtime   = false;
    wparams.print_timestamps = false;
    wparams.language         = "en";
    wparams.n_threads        = 4;
    wparams.single_segment   = true; // return single segment for overlay use

    int result = whisper_full(ctx, wparams, samples, static_cast<int>(sampleCount));
    env->ReleaseFloatArrayElements(pcmSamples, samples, JNI_ABORT);

    if (result != 0) {
        LOGE("transcribe: whisper_full returned error %d", result);
        return env->NewStringUTF("");
    }

    std::string text;
    int nSegments = whisper_full_n_segments(ctx);
    for (int i = 0; i < nSegments; i++) {
        const char *segText = whisper_full_get_segment_text(ctx, i);
        if (segText != nullptr) {
            text += segText;
        }
    }

    // Trim leading/trailing whitespace
    size_t start = text.find_first_not_of(" \t\n\r");
    size_t end   = text.find_last_not_of(" \t\n\r");
    if (start != std::string::npos) {
        text = text.substr(start, end - start + 1);
    } else {
        text.clear();
    }

    return env->NewStringUTF(text.c_str());
#endif
}

/**
 * Frees a whisper context created by initModel.
 */
JNIEXPORT void JNICALL
Java_com_voiceflow_app_whisper_WhisperEngine_freeModel(
        JNIEnv *env,
        jobject /* this */,
        jlong ctxHandle) {

#ifdef WHISPER_STUB
    LOGI("STUB: freeModel called");
    return;
#else
    if (ctxHandle == 0L) {
        return;
    }
    struct whisper_context *ctx = reinterpret_cast<struct whisper_context *>(ctxHandle);
    whisper_free(ctx);
    LOGI("freeModel: context freed");
#endif
}

} // extern "C"
