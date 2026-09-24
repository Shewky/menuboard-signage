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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class ScreenCaptureService :荆Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val httpClient = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var targetHost = ""
    private var isRunning = false

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            captureAndSendFrame()
            handler.postDelayed(this, 100L) // Saniyede ~10 FPS ile akıcı ekran aktarımı
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

    private fun startForegroundNotification() {
        val channelId = "screen_capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ekran Paylaşımı", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Menuboard Ekran Paylaşımı")
            .setContentText("Telefon ekranınız menuboarda aktarılıyor...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()

        startForeground(101, notification)
    }

    private fun setupVirtualDisplay() {
        val metrics = resources.displayMetrics
        val width = 540 // Ağ trafiğini hafif tutmak için 540x960 çözünürlüğe ölçeklenir
        val height = 960
        val dpi = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun captureAndSendFrame() {
        val image = imageReader?.acquireLatestImage() ?: return
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
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        val bytes = stream.toByteArray()

        if (targetHost.isNotEmpty()) {
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("http://$targetHost/api/live/frame")
                .post(body)
                .build()
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
            })
        }
    }

    private fun stopScreenCapture() {
        isRunning = false
        handler.removeCallbacks(captureRunnable)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()

        // Menuboard'a yayın bitti sinyali gönder
        if (targetHost.isNotEmpty()) {
            val request = Request.Builder().url("http://$targetHost/api/live/stop").post("".toRequestBody(null)).build()
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
            })
        }
    }

    override fun onDestroy() {
        stopScreenCapture()
        super.onDestroy()
    }
}
