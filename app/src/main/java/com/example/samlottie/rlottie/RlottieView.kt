package com.example.samlottie.rlottie

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import kotlin.math.max
import kotlin.math.roundToLong
import android.view.View
import java.io.File

class RlottieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val renderThread = HandlerThread("rlottie-render").apply { start() }
    private val renderHandler = Handler(renderThread.looper)

    private var handle: Long = 0L
    private var totalFrames: Int = 0
    private var frameRate: Float = 60f
    private var currentFrame: Int = 0
    private var bitmap: Bitmap? = null
    private var running: Boolean = false
    private var assetName: String? = null
    private var loggedFailedHandle = false
    private var startTimeMs: Long = 0L

    private val targetFps = 60f
     private val renderRunnable = object : Runnable {
        override fun run() {
            if (!running || handle == 0L || bitmap == null || totalFrames <= 0) return
            val elapsedMs = SystemClock.uptimeMillis() - startTimeMs
            val frame = ((elapsedMs * frameRate) / 1000f).toInt() % totalFrames
            currentFrame = frame
            NativeRlottie.render(handle, currentFrame, bitmap!!)
            postInvalidateOnAnimation()
            val delayMs = max(1L, (1000f / targetFps).roundToLong())
            renderHandler.postDelayed(this, delayMs)
        }
        
    }

    fun setAnimationAsset(name: String) {
        assetName = name
        if (width > 0 && height > 0) {
            loadAnimation(width, height)
            if (isAttachedToWindow) {
                start()
            }
        }
    }

    fun start() {
        if (running || handle == 0L) return
        startTimeMs = SystemClock.uptimeMillis()
        
        running = true
        renderHandler.post(renderRunnable)
    }

    fun stop() {
        running = false
        renderHandler.removeCallbacks(renderRunnable)

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        release()
        renderThread.quitSafely()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && assetName != null) {
            loadAnimation(w, h)
        }
    }

    private fun loadAnimation(w: Int, h: Int) {
        release()
        val asset = assetName ?: return
        val file = File(context.cacheDir, asset)
        val jsonText = runCatching {
            val bytes = context.assets.open(asset).use { it.readBytes() }
            val trimmed = if (bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()
            ) {
                bytes.copyOfRange(3, bytes.size)
            } else {
                bytes
            }
            if (!file.exists() || file.length() != trimmed.size.toLong()) {
                file.outputStream().use { it.write(trimmed) }
            }
            String(trimmed, Charsets.UTF_8)
        }.getOrNull()

        handle = if (!jsonText.isNullOrBlank()) {
            Log.d("RlottieView", "Loaded JSON from assets, length=${jsonText.length}")
            NativeRlottie.createFromJson(jsonText, asset)
        } else {
            0L
        }
        if (handle == 0L) {
            Log.w("RlottieView", "Falling back to file path=${file.absolutePath} size=${file.length()}")
            handle = NativeRlottie.createFromFile(file.absolutePath)
        }
        if (handle == 0L) return
        totalFrames = NativeRlottie.getTotalFrames(handle).coerceAtLeast(1)
        frameRate = NativeRlottie.getFrameRate(handle).coerceAtLeast(1f)
        Log.d("RlottieView", "Loaded $asset frames=$totalFrames fps=$frameRate size=${w}x${h}")
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        currentFrame = 0
        if (isAttachedToWindow) {
            start()
        }
    }

    private fun release() {
        if (handle != 0L) {
            NativeRlottie.destroy(handle)
            handle = 0L
        }
        bitmap?.recycle()
        bitmap = null
    }



    private fun ensureHandle() {
        if (handle == 0L && !loggedFailedHandle) {
            Log.e("RlottieView", "RLottie handle is 0. Check asset or native load.")
            loggedFailedHandle = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        ensureHandle()
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }
}
