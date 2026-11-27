package com.anypay.menuboard

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.Instrumentation
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.*
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import fi.iki.elonen.NanoHTTPD
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class MediaItemModel(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val type: String = "image",
    val durationSec: Int = 10,
    val waitAfterSec: Int = 0,
    val animation: String = "fade",
    val autoScroll: Boolean = false,
    val scrollUntilEnd: Boolean = false,
    val webScrollSpeed: String? = "medium",
    val showQr: Boolean = false,
    val qrCorner: String? = "bottom_right",
    val qrSizeDp: Int = 150,
    val qrBorderDp: Int = 8,
    val webZoomPercent: Int = 100,
    val webFontFamily: String? = "default",
    val webFontSizePercent: Int = 0,
    val webBoldHeaders: Boolean = false,
    val webImageScalePercent: Int = 100,
    val webFixedHeader: Boolean = false,
    val webFixedSelector: String? = "",
    val webFixedHeaderTextEnabled: Boolean = false,
    val webFixedHeaderText: String? = "",
    val igMediaFilter: String? = "all",
    val igSlideSec: Int = 8,
    val igAudioEnabled: Boolean = false,
    val isHidden: Boolean = false,
    var fileSizeBytes: Long = 0L,
    var fileDurationSec: Int = 0
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class PeerDeviceInfo(
    val id: String,
    val tag: String,
    val host: String,
    val lastSeenMs: Long,
    val isSelf: Boolean = false
)

data class ShareRequestPayload(
    val item: MediaItemModel? = null,
    val targets: List<String>? = emptyList()
)

class NoWifiIconView(context: Context) : View(context) {
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        alpha = 0.35f
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = minOf(w, h)
        if (s <= 0f) return

        val cx = w / 2f
        val cy = h * 0.78f
        strokePaint.strokeWidth = s * 0.085f
        slashPaint.strokeWidth = s * 0.09f

        val r1 = s * 0.20f
        val r2 = s * 0.36f
        val r3 = s * 0.52f

        canvas.drawArc(RectF(cx - r3, cy - r3, cx + r3, cy + r3), 225f, 90f, false, strokePaint)
        canvas.drawArc(RectF(cx - r2, cy - r2, cx + r2, cy + r2), 225f, 90f, false, strokePaint)
        canvas.drawArc(RectF(cx - r1, cy - r1, cx + r1, cy + r1), 225f, 90f, false, strokePaint)
        canvas.drawCircle(cx, cy, s * 0.065f, fillPaint)

        // Üzeri çapraz çizgi
        val pad = s * 0.14f
        canvas.drawLine(pad, pad, w - pad, h - pad, slashPaint)
    }
}

class AnimatedOverlayView(context: Context) : View(context) {
    private var mode: String = "none"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tick = 0f

    var cursorX = 540f
    var cursorY = 960f
    var showCursor = false
    private val cursorPath = Path()

    private data class Entity(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var size: Float, var phase: Float, var kind: Int,
        var angle: Float, var spinSpeed: Float, var color: Int
    )

    private val entities = mutableListOf<Entity>()

    fun setAnimationMode(newMode: String) {
        mode = newMode
        entities.clear()
        when (mode) {
            "fish" -> {
                for (i in 0 until 12) {
                    val dir = if (i % 2 == 0) 1f else -1f
                    entities.add(Entity(Random.nextFloat() * 1080f, 160f + Random.nextFloat() * 1500f, (1.8f + Random.nextFloat() * 2.5f) * dir, 0f, 26f + Random.nextFloat() * 22f, Random.nextFloat() * 6.28f, i % 3, 0f, 0f, 0))
                }
                for (i in 0 until 2) entities.add(Entity(220f + i * 620f, 480f + i * 680f, 0.9f * (if (i == 0) 1f else -1f), 0.4f, 46f, i * 2.1f, 3, 0f, 0f, Color.argb(190, 171, 71, 188)))
                for (i in 0 until 2) entities.add(Entity(280f + i * 480f, 1840f, if (i == 0) 1.6f else -1.6f, 0f, 36f, i * 1.5f, 4, 0f, 0f, Color.argb(215, 211, 47, 47)))
                for (i in 0 until 25) entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, 0f, -(2.0f + Random.nextFloat() * 2.8f), 4f + Random.nextFloat() * 11f, Random.nextFloat() * 6.28f, 5, 0f, 0f, Color.WHITE))
            }
            "coffee" -> {
                for (i in 0 until 18) entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 1.1f, -(1.2f + Random.nextFloat() * 1.8f), 18f + Random.nextFloat() * 14f, Random.nextFloat() * 6.28f, 0, Random.nextFloat() * 360f, (Random.nextFloat() - 0.5f) * 4.5f, if (i % 2 == 0) Color.rgb(78, 52, 46) else Color.rgb(62, 39, 35)))
                for (i in 0 until 16) entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, 0f, -(1.6f + Random.nextFloat() * 2.2f), 75f + Random.nextFloat() * 95f, Random.nextFloat() * 6.28f, 1, 0f, 0f, Color.rgb(239, 235, 233)))
            }
            "doner" -> {
                for (i in 0 until 32) {
                    val k = when { i < 14 -> 0; i < 26 -> 1; else -> 2 }
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 1.4f, if (k == 2) (2.8f + Random.nextFloat() * 2f) else -(1.8f + Random.nextFloat() * 3.2f), if (k == 0) (65f + Random.nextFloat() * 85f) else (4f + Random.nextFloat() * 5f), Random.nextFloat() * 6.28f, k, 0f, 0f, Color.rgb(255, 110, 0)))
                }
            }
            "hookah" -> {
                for (i in 0 until 26) entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 0.9f, -(1.3f + Random.nextFloat() * 2.0f), if (i % 3 == 0) (28f + Random.nextFloat() * 35f) else (70f + Random.nextFloat() * 100f), Random.nextFloat() * 6.28f, if (i % 3 == 0) 1 else 0, 0f, 0f, Color.rgb(215, 238, 255)))
            }
            "gold", "snow" -> {
                for (i in 0 until 38) entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 0.8f, if (mode == "snow") (1.8f + Random.nextFloat() * 2.6f) else -(1.0f + Random.nextFloat() * 1.6f), 6f + Random.nextFloat() * 18f, Random.nextFloat() * 6.28f, 0, 0f, 1.5f, if (mode == "gold") Color.rgb(255, 215, 0) else Color.WHITE))
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat().coerceAtLeast(1080f)
        val h = height.toFloat().coerceAtLeast(1920f)
        tick += 0.045f

        when (mode) {
            "fish" -> drawMarineEcosystem(canvas, w, h)
            "coffee" -> drawCoffeeBeansAndSteam(canvas, w, h)
            "doner" -> drawRealisticDonerAndFire(canvas, w, h)
            "hookah" -> drawHookahCloudsAndRings(canvas, w, h)
            "gold", "snow" -> drawBokehOrSnow(canvas, w, h)
        }

        if (showCursor) {
            cursorPath.reset()
            cursorPath.moveTo(cursorX, cursorY)
            cursorPath.lineTo(cursorX, cursorY + 36f)
            cursorPath.lineTo(cursorX + 10f, cursorY + 28f)
            cursorPath.lineTo(cursorX + 18f, cursorY + 42f)
            cursorPath.lineTo(cursorX + 24f, cursorY + 38f)
            cursorPath.lineTo(cursorX + 16f, cursorY + 24f)
            cursorPath.lineTo(cursorX + 28f, cursorY + 24f)
            cursorPath.close()

            paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#FF1744")
            canvas.drawPath(cursorPath, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = Color.WHITE
            canvas.drawPath(cursorPath, paint)
        }

        if (mode != "none" || showCursor) postInvalidateDelayed(20L)
    }

    private fun drawCoffeeBeansAndSteam(canvas: Canvas, w: Float, h: Float) {
        for (e in entities) {
            e.y += e.vy
            val swayX = e.x + sin((tick * 1.1f + e.phase).toDouble()).toFloat() * 26f
            if (e.y < -120f) { e.y = h + 80f; e.x = Random.nextFloat() * w }
            if (e.kind == 1) {
                val progress = ((h - e.y) / h).coerceIn(0f, 1f)
                val rad = e.size * (0.85f + progress * 0.9f)
                val alpha = ((1f - progress * 0.6f) * 44f).toInt().coerceIn(8, 48)
                paint.style = Paint.Style.FILL
                paint.shader = RadialGradient(swayX, e.y, rad, intArrayOf(Color.argb(alpha, 245, 235, 225), Color.argb(alpha / 2, 230, 215, 200), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                canvas.drawCircle(swayX, e.y, rad, paint); paint.shader = null
            } else {
                e.angle = (e.angle + e.spinSpeed) % 360f
                canvas.save(); canvas.translate(swayX, e.y); canvas.rotate(e.angle)
                val bw = e.size * 1.35f; val bh = e.size * 0.92f
                paint.style = Paint.Style.FILL; paint.color = e.color
                canvas.drawOval(RectF(-bw, -bh, bw, bh), paint)
                paint.color = Color.argb(70, 161, 136, 127)
                canvas.drawOval(RectF(-bw * 0.7f, -bh * 0.75f, bw * 0.5f, -bh * 0.15f), paint)
                paint.style
