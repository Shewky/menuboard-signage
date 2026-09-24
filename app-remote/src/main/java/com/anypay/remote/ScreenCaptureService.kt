package com.anypay.remote

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val httpClient = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var targetHost = ""
    private var isRunning = false

    // Kalite Ayarları
    private var targetWidth = 540
    private var targetHeight = 960
    private var jpegQuality = 60
    private var frameIntervalMs = 70L // ~14 FPS

    // Drop-frame Kilidi: Ağ önceki kareyi bitirmeden yeni kare göndermez
    private val isFrameSending = AtomicBoolean(false)

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (!isFrameSending.get()) {
                captureAndSendFrame()
            }
            handler.postDelayed(this, frameIntervalMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopScreenCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        targetHost = intent?.getStringExtra("target_host") ?: ""
        val qualityLevel = intent?.getIntExtra("quality_level", 1) ?: 1
        applyQualityProfile(qualityLevel)

        val resultCode = intent?.getIntExtra("result_code", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = intent?.getParcelableExtra<Intent>("result_data")

        startForegroundNotification()

        if (resultData != null && resultCode == Activity.RESULT_OK) {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpManager.getMediaProjection(resultCode, resultData)
            setupVirtualDisplay()
            isRunning = true
            handler.post(captureRunnable)
        }

        return START_STICKY
    }

    private fun applyQualityProfile(level: Int) {
        when (level) {
            0 -> { // Düşük (Hızlı / Akıcı)
                targetWidth = 360
                targetHeight = 640
                jpegQuality = 45
                frameIntervalMs = 85L // ~12 FPS
            }
            2 -> { // Yüksek (Net)
                targetWidth = 720
                targetHeight = 1280
                jpegQuality = 75
                frameIntervalMs = 50L // ~20 FPS
            }
            else -> { // Dengeli (Önerilen)
                targetWidth = 540
                targetHeight = 960
                jpegQuality = 60
                frameIntervalMs = 65L // ~15 FPS
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "screen_capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ekran Paylaşımı", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Menuboard Ekran Paylaşımı")
            .setContentText("Görüntü aktarımı aktif (${targetWidth}x${targetHeight})")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()

        startForeground(101, notification)
    }

    private fun setupVirtualDisplay() {
        val dpi = resources.displayMetrics.densityDpi
        imageReader = ImageReader.newInstance(targetWidth, targetHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            targetWidth, targetHeight, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun captureAndSendFrame() {
        val image = imageReader?.acquireLatestImage() ?: return
        isFrameSending.set(true)

        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        val stream = ByteArrayOutputStream()
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, stream)
        val bytes = stream.toByteArray()

        if (targetHost.isNotEmpty()) {
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("http://$targetHost/api/live/frame")
                .post(body)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    isFrameSending.set(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                    isFrameSending.set(false)
                }
            })
        } else {
            isFrameSending.set(false)
        }
    }

    private fun stopScreenCapture() {
        isRunning = false
        handler.removeCallbacks(captureRunnable)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()

        if (targetHost.isNotEmpty()) {
            val request = Request.Builder().url("http://$targetHost/api/live/stop").post("".toRequestBody(null)).build()
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
        }
    }

    override fun onDestroy() {
        stopScreenCapture()
        super.onDestroy()
    }
}
