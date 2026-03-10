#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <atomic>
#include <cstdint>
#include <cstring>

#include "rlottie_capi.h"

namespace {
constexpr const char *kTag = "RlottieJNI";
std::atomic<bool> g_inited{false};

void ensureInitialized() {
    bool expected = false;
    if (g_inited.compare_exchange_strong(expected, true)) {
        lottie_init();
    }
}
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_createFromFile(
    JNIEnv *env,
    jclass,
    jstring path
) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    ensureInitialized();
    Lottie_Animation *anim = lottie_animation_from_file(cpath);
    env->ReleaseStringUTFChars(path, cpath);
    if (!anim) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "lottie_animation_from_file failed");
    }
    return reinterpret_cast<jlong>(anim);
}

JNIEXPORT jlong JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_createFromJson(
    JNIEnv *env,
    jclass,
    jstring json,
    jstring key
) {
    const char *cjson = env->GetStringUTFChars(json, nullptr);
    const char *ckey = env->GetStringUTFChars(key, nullptr);
    ensureInitialized();
    __android_log_print(ANDROID_LOG_DEBUG, kTag, "createFromJson len=%zu", strlen(cjson));
    Lottie_Animation *anim = lottie_animation_from_data(cjson, ckey, "");
    env->ReleaseStringUTFChars(json, cjson);
    env->ReleaseStringUTFChars(key, ckey);
    if (!anim) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "lottie_animation_from_data failed");
    }
    return reinterpret_cast<jlong>(anim);
}

JNIEXPORT jint JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_getTotalFrames(
    JNIEnv *env,
    jclass,
    jlong handle
) {
    auto *anim = reinterpret_cast<Lottie_Animation *>(handle);
    if (!anim) return 0;
    return static_cast<jint>(lottie_animation_get_totalframe(anim));
}

JNIEXPORT jfloat JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_getFrameRate(
    JNIEnv *env,
    jclass,
    jlong handle
) {
    auto *anim = reinterpret_cast<Lottie_Animation *>(handle);
    if (!anim) return 0.0f;
    return static_cast<jfloat>(lottie_animation_get_framerate(anim));
}

JNIEXPORT void JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_render(
    JNIEnv *env,
    jclass,
    jlong handle,
    jint frame,
    jobject bitmap
) {
    auto *anim = reinterpret_cast<Lottie_Animation *>(handle);
    if (!anim || !bitmap) return;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "AndroidBitmap_getInfo failed");
        return;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "Bitmap format not RGBA_8888");
        return;
    }

    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "AndroidBitmap_lockPixels failed");
        return;
    }

    lottie_animation_render(
        anim,
        static_cast<size_t>(frame),
        static_cast<uint32_t *>(pixels),
        static_cast<size_t>(info.width),
        static_cast<size_t>(info.height),
        static_cast<size_t>(info.stride)
    );

    AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT void JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_destroy(
    JNIEnv *env,
    jclass,
    jlong handle
) {
    auto *anim = reinterpret_cast<Lottie_Animation *>(handle);
    if (!anim) return;
    lottie_animation_destroy(anim);
}

JNIEXPORT void JNICALL
Java_com_example_samlottie_rlottie_NativeRlottie_configureModelCacheSize(
    JNIEnv *env,
    jclass,
    jint cacheSize
) {
    if (cacheSize < 0) cacheSize = 0;
    lottie_configure_model_cache_size(static_cast<size_t>(cacheSize));
}

}  // extern "C"
