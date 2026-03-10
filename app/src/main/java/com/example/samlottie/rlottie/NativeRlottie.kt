package com.example.samlottie.rlottie

import android.graphics.Bitmap

object NativeRlottie {
    init {
        System.loadLibrary("rlottie_jni")
    }

    external fun createFromFile(path: String): Long
    external fun createFromJson(json: String, key: String): Long
    external fun getTotalFrames(handle: Long): Int
    external fun getFrameRate(handle: Long): Float
    external fun render(handle: Long, frame: Int, bitmap: Bitmap)
    external fun destroy(handle: Long)
}
