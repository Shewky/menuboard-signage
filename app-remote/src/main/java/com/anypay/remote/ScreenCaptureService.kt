package com.anypay.remote

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val httpClient = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var targetHost = ""
    private var isRunning = false
    private val isSending = AtomicBoolean(false)

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (!isSending.get()) {
                captureAndSend()
            }
            handler.postDelayed(this, 70L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        targetHost = intent?.getStringExtra("target_host") ?: ""
        val resultCode = intent?.getIntExtra("result_code", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("result_data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("result_data")
        }

        if (resultData != null && resultCode == Activity.RESULT_OK) {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

            // Android 14 Zorunluluğu: createVirtualDisplay öncesi Callback kaydı
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                }
            }, handler)

            setupVirtualDisplay()
            isRunning = true
            handler.post(captureRunnable)
        }

        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "screen_stream_ch"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Ekran Paylaşımı", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Menuboard Ekran Paylaşımı")
            .setContentText("Ekranınız canlı olarak aktarılıyor...")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun setupVirtualDisplay() {
        val dpi = resources.displayMetrics.densityDpi
        val width = 540
        val height = 960

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenStream",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )
    }

    private fun captureAndSend() {
        val image = imageReader?.acquireLatestImage() ?: return
        isSending.set(true)

        try {
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
                val req = Request.Builder().url("http://$targetHost/api/screen/frame").post(body).build()
                httpClient.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { isSending.set(false) }
                    override fun onResponse(call: Call, response: Response) {
                        response.close()
                        isSending.set(false)
                    }
                })
            } else {
                isSending.set(false)
            }
        } catch (_: Exception) {
            image.close()
            isSending.set(false)
        }
    }

    private fun stopCapture() {
        isRunning = false
        handler.removeCallbacks(captureRunnable)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null

        if (targetHost.isNotEmpty()) {
            val req = Request.Builder().url("http://$targetHost/api/screen/stop").post("".toRequestBody(null)).build()
            httpClient.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
        }
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
