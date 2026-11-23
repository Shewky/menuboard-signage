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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.Executors
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
    val isHidden: Boolean = false
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

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
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.5f; paint.color = Color.argb(220, 27, 15, 12)
                canvas.drawPath(Path().apply { moveTo(-bw * 0.85f, 0f); cubicTo(-bw * 0.3f, -bh * 0.45f, bw * 0.3f, bh * 0.45f, bw * 0.85f, 0f) }, paint)
                canvas.restore()
            }
        }
    }

    private fun drawMarineEcosystem(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 8f; paint.strokeCap = Paint.Cap.ROUND
        for (k in 1..9) {
            val baseX = (w / 10f) * k
            paint.color = if (k % 2 == 0) Color.argb(110, 46, 125, 50) else Color.argb(100, 0, 150, 136)
            val sway1 = sin((tick * 1.6f + k).toDouble()).toFloat() * 28f
            val sway2 = cos((tick * 1.6f + k).toDouble()).toFloat() * 38f
            canvas.drawPath(Path().apply { moveTo(baseX, h); cubicTo(baseX + sway1, h - 75f, baseX - sway1, h - 145f, baseX + sway2, h - 220f) }, paint)
        }
        for (e in entities) {
            when (e.kind) {
                0, 1, 2 -> {
                    e.x += e.vx
                    val dir = if (e.vx >= 0) 1f else -1f
                    val waveY = e.y + sin((tick * 2.2f + e.phase).toDouble()).toFloat() * 20f
                    if (dir > 0 && e.x > w + 130f) e.x = -110f
                    if (dir < 0 && e.x < -130f) e.x = w + 110f
                    val primaryColor = when (e.kind) { 0 -> Color.argb(200, 255, 109, 0); 1 -> Color.argb(200, 2, 136, 209); else -> Color.argb(200, 255, 196, 0) }
                    val bodyH = if (e.kind == 2) e.size * 0.88f else e.size * 0.54f
                    val wag = sin((tick * 4.8f + e.phase).toDouble()).toFloat() * 13f
                    paint.style = Paint.Style.FILL; paint.color = primaryColor
                    val tail = Path().apply { moveTo(e.x - dir * e.size * 0.75f, waveY); lineTo(e.x - dir * e.size * 1.7f, waveY - e.size * 0.72f + wag); lineTo(e.x - dir * e.size * 1.3f, waveY + wag * 0.3f); lineTo(e.x - dir * e.size * 1.7f, waveY + e.size * 0.72f + wag); close() }
                    canvas.drawPath(tail, paint)
                    canvas.drawPath(Path().apply { moveTo(e.x + dir * e.size * 0.1f, waveY - bodyH * 0.85f); quadTo(e.x - dir * e.size * 0.35f, waveY - bodyH * 1.55f, e.x - dir * e.size * 0.65f, waveY - bodyH * 0.75f); close() }, paint)
                    canvas.drawOval(RectF(e.x - e.size, waveY - bodyH, e.x + e.size, waveY + bodyH), paint)
                    if (e.kind == 0) { paint.color = Color.argb(195, 255, 255, 255); canvas.drawRoundRect(RectF(e.x - e.size * 0.15f, waveY - bodyH * 0.92f, e.x + e.size * 0.12f, waveY + bodyH * 0.92f), 6f, 6f, paint) }
                    else if (e.kind == 1) { paint.color = Color.argb(180, 255, 235, 59); canvas.drawPath(tail, paint) }
                    paint.color = Color.WHITE; canvas.drawCircle(e.x + dir * e.size * 0.58f, waveY - bodyH * 0.22f, e.size * 0.16f, paint)
                    paint.color = Color.BLACK; canvas.drawCircle(e.x + dir * e.size * 0.63f, waveY - bodyH * 0.22f, e.size * 0.08f, paint)
                }
                3 -> {
                    e.x += e.vx
                    val bobY = e.y + sin((tick * 1.5f + e.phase).toDouble()).toFloat() * 38f
                    if (e.x > w - 85f || e.x < 85f) e.vx = -e.vx
                    paint.style = Paint.Style.FILL; paint.color = e.color
                    canvas.drawOval(RectF(e.x - e.size, bobY - e.size * 1.15f, e.x + e.size, bobY + e.size * 0.32f), paint)
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 6.5f
                    for (t in -3..4) {
                        val startX = e.x + t * (e.size * 0.22f)
                        val sway = sin((tick * 2.6f + t * 0.7f + e.phase).toDouble()).toFloat() * 24f
                        canvas.drawPath(Path().apply { moveTo(startX, bobY + e.size * 0.2f); quadTo(startX + sway, bobY + e.size * 1.1f, startX - sway * 0.6f, bobY + e.size * 1.95f) }, paint)
                    }
                    paint.style = Paint.Style.FILL; paint.color = Color.WHITE
                    canvas.drawCircle(e.x - e.size * 0.35f, bobY - e.size * 0.2f, 7.5f, paint)
                    canvas.drawCircle(e.x + e.size * 0.35f, bobY - e.size * 0.2f, 7.5f, paint)
                    paint.color = Color.BLACK
                    canvas.drawCircle(e.x - e.size * 0.35f, bobY - e.size * 0.2f, 3.8f, paint)
                    canvas.drawCircle(e.x + e.size * 0.35f, bobY - e.size * 0.2f, 3.8f, paint)
                }
                4 -> {
                    e.x += e.vx
                    if (e.x > w - 90f || e.x < 90f) e.vx = -e.vx
                    val stepY = (h - 62f) + kotlin.math.abs(sin((tick * 5.5f + e.phase).toDouble()).toFloat()) * 8f
                    paint.style = Paint.Style.FILL; paint.color = e.color
                    canvas.drawOval(RectF(e.x - e.size, stepY - e.size * 0.55f, e.x + e.size, stepY + e.size * 0.55f), paint)
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 4.5f
                    for (leg in -1..1) {
                        val legOffset = sin((tick * 6f + leg).toDouble()).toFloat() * 6f
                        canvas.drawLine(e.x - e.size * 0.8f, stepY + leg * 6f, e.x - e.size * 1.45f, stepY + 18f + legOffset, paint)
                        canvas.drawLine(e.x + e.size * 0.8f, stepY + leg * 6f, e.x + e.size * 1.45f, stepY + 18f - legOffset, paint)
                    }
                    paint.style = Paint.Style.FILL
                    canvas.drawArc(RectF(e.x - e.size * 1.6f, stepY - e.size * 1.25f, e.x - e.size * 0.9f, stepY - e.size * 0.55f), 30f, 290f, true, paint)
                    canvas.drawArc(RectF(e.x + e.size * 0.9f, stepY - e.size * 1.25f, e.x + e.size * 1.6f, stepY - e.size * 0.55f), 210f, 290f, true, paint)
                    paint.color = Color.WHITE; canvas.drawCircle(e.x - 10f, stepY - e.size * 0.7f, 5.5f, paint); canvas.drawCircle(e.x + 10f, stepY - e.size * 0.7f, 5.5f, paint)
                }
                5 -> {
                    e.y += e.vy
                    val bx = e.x + sin((tick * 2f + e.phase).toDouble()).toFloat() * 14f
                    if (e.y < -30f) { e.y = h + 20f; e.x = Random.nextFloat() * w }
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = Color.argb(130, 200, 245, 255)
                    canvas.drawCircle(bx, e.y, e.size, paint)
                    paint.style = Paint.Style.FILL; paint.color = Color.argb(160, 255, 255, 255)
                    canvas.drawCircle(bx - e.size * 0.3f, e.y - e.size * 0.3f, e.size * 0.22f, paint)
                }
            }
        }
    }

    private fun drawRealisticDonerAndFire(canvas: Canvas, w: Float, h: Float) {
        paint.shader = RadialGradient(w - 150f, h - 80f, 380f, intArrayOf(Color.argb(135, 255, 87, 34), Color.argb(60, 211, 47, 47), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL; canvas.drawCircle(w - 150f, h - 80f, 380f, paint); paint.shader = null
        val cx = w - 145f; val cy = h - 250f
        paint.color = Color.argb(190, 190, 195, 200); canvas.drawRect(cx - 5f, cy - 155f, cx + 5f, cy + 155f, paint)
        for (layer in -5..5) {
            val yPos = cy + layer * 20f; val coneRadius = (90f - (layer + 5) * 3.8f)
            val shimmer = sin((tick * 2.5f + layer * 0.8f).toDouble()).toFloat() * 6f
            paint.color = if ((layer + (tick * 2).toInt()) % 2 == 0) Color.argb(185, 145, 52, 18) else Color.argb(185, 198, 84, 30)
            canvas.drawOval(RectF(cx - coneRadius + shimmer, yPos - 13f, cx + coneRadius + shimmer, yPos + 13f), paint)
        }
        for (e in entities) {
            e.y += e.vy
            val driftX = e.x + sin((tick * 1.2f + e.phase).toDouble()).toFloat() * 28f
            if (e.kind == 2 && e.y > h) { e.y = cy - 80f; e.x = cx + (Random.nextFloat() - 0.5f) * 90f }
            else if (e.kind != 2 && e.y < -120f) { e.y = h + 60f; e.x = Random.nextFloat() * w }
            when (e.kind) {
                0 -> {
                    val radius = e.size * (1f + ((h - e.y) / h) * 0.65f)
                    paint.shader = RadialGradient(driftX, e.y, radius, intArrayOf(Color.argb(46, 255, 245, 238), Color.argb(20, 255, 235, 220), Color.TRANSPARENT), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
                    canvas.drawCircle(driftX, e.y, radius, paint); paint.shader = null
                }
                1 -> { paint.color = Color.argb((130 + sin((tick * 8f + e.phase).toDouble()) * 85).toInt().coerceIn(45, 230), 255, 152, 0); canvas.drawCircle(driftX, e.y, e.size, paint) }
                2 -> { paint.color = Color.argb(210, 255, 193, 7); canvas.drawOval(RectF(e.x - 3.5f, e.y - 7f, e.x + 3.5f, e.y + 7f), paint) }
            }
        }
    }

    private fun drawHookahCloudsAndRings(canvas: Canvas, w: Float, h: Float) {
        for (e in entities) {
            e.y += e.vy
            val waveX = e.x + cos((tick * 0.9f + e.phase).toDouble()).toFloat() * 48f
            if (e.y < -160f) { e.y = h + 120f; e.x = Random.nextFloat() * w }
            val progress = ((h - e.y) / h).coerceIn(0f, 1f)
            if (e.kind == 1) {
                val ringRad = e.size * (0.9f + progress * 1.4f)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 9f + progress * 8f
                paint.color = Color.argb(((1f - progress * 0.7f) * 85f).toInt().coerceIn(12, 90), 220, 240, 255)
                canvas.drawOval(RectF(waveX - ringRad, e.y - ringRad * 0.62f, waveX + ringRad, e.y + ringRad * 0.62f), paint)
            } else {
                paint.style = Paint.Style.FILL
                val rad = e.size * (0.8f + progress * 1.15f)
                val peakAlpha = ((1f - progress * 0.65f) * 54f).toInt().coerceIn(8, 60)
                paint.shader = RadialGradient(waveX, e.y, rad, intArrayOf(Color.argb(peakAlpha, 215, 238, 255), Color.argb(peakAlpha / 2, 200, 230, 255), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                canvas.drawCircle(waveX, e.y, rad, paint); paint.shader = null
            }
        }
    }

    private fun drawBokehOrSnow(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        for (e in entities) {
            e.y += e.vy
            val sx = e.x + sin((tick + e.phase).toDouble()).toFloat() * 20f
            if (mode == "snow" && e.y > h + 30f) { e.y = -20f; e.x = Random.nextFloat() * w }
            else if (mode == "gold" && e.y < -30f) { e.y = h + 20f; e.x = Random.nextFloat() * w }
            val pulseAlpha = (85 + sin((tick * 3f + e.phase).toDouble()) * 65).toInt().coerceIn(30, 185)
            paint.color = if (mode == "gold") Color.argb(pulseAlpha, 255, 215, 0) else Color.argb(pulseAlpha, 255, 255, 255)
            canvas.drawCircle(sx, e.y, e.size, paint)
        }
    }
}

class MainActivity : AppCompatActivity() {

    companion object {
        var currentTaskId: Int = -1
    }

    private val GITHUB_REPO = "KULLANICI_ADINIZ/menuboard-signage"

    private lateinit var mediaContainer: FrameLayout
    private lateinit var bgBlurImageView: ImageView
    private lateinit var videoTextureView: TextureView
    private lateinit var playerView: PlayerView
    private lateinit var imageView: ImageView
    private lateinit var imgLiveStream: ImageView
    private lateinit var imgCornerLogo: ImageView
    private lateinit var qrOverlay: View
    private lateinit var imgQrCode: ImageView
    private lateinit var txtIpAddress: TextView
    private lateinit var txtStatus: TextView
    private lateinit var btnSettings: ImageView
    private lateinit var btnCloseQr: Button
    private lateinit var findMeOverlay: View
    private lateinit var txtFindMeTag: TextView
    private lateinit var animatedOverlayView: AnimatedOverlayView
    private var btnWifiSettings: Button? = null
    private var isWifiPopupOpen = false

    private lateinit var webView: WebView
    private lateinit var webQrContainer: FrameLayout
    private lateinit var imgWebQr: ImageView
    private var activeWebItem: MediaItemModel? = null
    private var webScrollActive = false
    private var webScrollBottomStrikes = 0

    private var igPostUrls = mutableListOf<String>()
    private var igPostIndex = 0
    private val igSafetyRunnable = Runnable { playNextInstagramPost() }

    private var exoPlayer: ExoPlayer? = null
    private var httpServer: SignageServer? = null
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val updateHttpClient = OkHttpClient()
    private val blurExecutor = Executors.newSingleThreadExecutor()
    private val isBlurProcessing = AtomicBoolean(false)

    private var playlist = mutableListOf<MediaItemModel>()
    private var currentIndex = 0
    private var deviceId = ""
    private var deviceTag = "Menuboard"
    private var isScreenSharing = false
    private var isBroadcasting = true
    private var currentKnownIp = ""
    private var activeBgObject = "none"
    private var screenOrientationMode = "portrait"
    private var blurBackgroundEnabled = false
    private var blurDarknessPercent = 30
    private var blurIntensityLevel = 1
    private var blurQualityLevel = 1
    private var blurApplyInstagram = false
    private var immersiveModeEnabled = true
    private var autoStartOnBoot = false

    private var currentVideoWidth = 0
    private var currentVideoHeight = 0
    private var awaitingSequentialPermissionChain = false

    private val mediaEndRunnable = Runnable { scheduleNextMedia() }
    private val hideCursorRunnable = Runnable {
        animatedOverlayView.showCursor = false
        animatedOverlayView.invalidate()
    }

    private val videoBlurRunnable = object : Runnable {
        override fun run() {
            if (!blurBackgroundEnabled || isScreenSharing || videoTextureView.visibility != View.VISIBLE) return
            val player = exoPlayer
            val delayMs = when (blurQualityLevel) { 0 -> 70L; 2 -> 22L; else -> 38L }
            if (player != null && player.isPlaying && videoTextureView.isAvailable) {
                try {
                    val vw = videoTextureView.width.coerceAtLeast(1)
                    val vh = videoTextureView.height.coerceAtLeast(1)
                    val ratio = vw.toFloat() / vh.toFloat()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val longDim = when (blurQualityLevel) { 0 -> 180; 2 -> 360; else -> 260 }
                        val bw = if (ratio >= 1f) longDim else (longDim * ratio).toInt().coerceAtLeast(48)
                        val bh = if (ratio >= 1f) (longDim / ratio).toInt().coerceAtLeast(48) else longDim
                        videoTextureView.getBitmap(bw, bh)?.let { frame ->
                            bgBlurImageView.setImageBitmap(frame)
                            if (bgBlurImageView.visibility != View.VISIBLE) bgBlurImageView.visibility = View.VISIBLE
                        }
                    } else if (isBlurProcessing.compareAndSet(false, true)) {
                        val longDim = when (blurQualityLevel) { 0 -> 110; 2 -> 200; else -> 150 }
                        val bw = if (ratio >= 1f) longDim else (longDim * ratio).toInt().coerceAtLeast(40)
                        val bh = if (ratio >= 1f) (longDim / ratio).toInt().coerceAtLeast(40) else longDim
                        val rawFrame = videoTextureView.getBitmap(bw, bh)
                        val blurRadius = when (blurIntensityLevel) { 0 -> 5; 2 -> 18; else -> 10 }
                        if (rawFrame != null) {
                            blurExecutor.execute {
                                try {
                                    val smooth = fastBlur(rawFrame, blurRadius)
                                    rawFrame.recycle()
                                    runOnUiThread {
                                        if (blurBackgroundEnabled && !isScreenSharing && videoTextureView.visibility == View.VISIBLE) {
                                            bgBlurImageView.setImageBitmap(smooth)
                                            if (bgBlurImageView.visibility != View.VISIBLE) bgBlurImageView.visibility = View.VISIBLE
                                        }
                                    }
                                } catch (_: Throwable) {
                                } finally { isBlurProcessing.set(false) }
                            }
                        } else isBlurProcessing.set(false)
                    }
                } catch (_: Throwable) { isBlurProcessing.set(false) }
            }
            handler.postDelayed(this, delayMs)
        }
    }

    private val webScrollRunnable = object : Runnable {
        override fun run() {
            if (!webScrollActive || webView.visibility != View.VISIBLE) return
            val item = activeWebItem ?: return
            val (stepPx, delayMs) = when (item.webScrollSpeed) {
                "slow" -> Pair(1, 34L)
                "slow_medium" -> Pair(2, 30L)
                "medium_fast" -> Pair(5, 25L)
                "fast" -> Pair(7, 20L)
                else -> Pair(3, 28L)
            }
            if (webView.canScrollVertically(1)) {
                webScrollBottomStrikes = 0
                webView.scrollBy(0, stepPx)
                handler.postDelayed(this, delayMs)
            } else if (item.scrollUntilEnd) {
                webScrollBottomStrikes++
                if (webScrollBottomStrikes < 3) handler.postDelayed(this, 500L)
                else {
                    webScrollActive = false; webScrollBottomStrikes = 0
                    handler.removeCallbacks(mediaEndRunnable)
                    handler.postDelayed(mediaEndRunnable, 2500L)
                }
            }
        }
    }

    private val ipCheckRunnable = object : Runnable {
        override fun run() {
            val freshIp = getActiveLocalIpAddress()
            val hasNetwork = freshIp != "127.0.0.1"
            if (hasNetwork) {
                if (freshIp != currentKnownIp) {
                    currentKnownIp = freshIp
                    txtIpAddress.text = "IP: $freshIp:8080"
                    generateQr(freshIp, deviceId)
                }
                if (isWifiPopupOpen) {
                    isWifiPopupOpen = false
                    try {
                        startActivity(Intent(this@MainActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        })
                    } catch (_: Exception) {}
                    Toast.makeText(this@MainActivity, "✅ Ağ bağlantısı sağlandı!", Toast.LENGTH_SHORT).show()
                }
            } else {
                currentKnownIp = "127.0.0.1"
                txtIpAddress.text = "Ağ Erişimi Yok"
            }
            updateNoNetworkButtonVisibility()
            handler.postDelayed(this, 2500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
        screenOrientationMode = prefs.getString("screen_orientation", "portrait") ?: "portrait"
        blurBackgroundEnabled = prefs.getBoolean("blur_bg_enabled", false)
        blurDarknessPercent = prefs.getInt("blur_darkness", 30).coerceIn(0, 85)
        blurIntensityLevel = prefs.getInt("blur_intensity", 1).coerceIn(0, 2)
        blurQualityLevel = prefs.getInt("blur_quality", 1).coerceIn(0, 2)
        blurApplyInstagram = prefs.getBoolean("blur_apply_ig", false)
        immersiveModeEnabled = prefs.getBoolean("immersive_mode", true)
        autoStartOnBoot = prefs.getBoolean("auto_start_on_boot", false)
        applyScreenOrientation(screenOrientationMode)

        setContentView(R.layout.activity_main)
        applyImmersiveMode(immersiveModeEnabled)
        currentTaskId = taskId

        mediaContainer = findViewById(R.id.mediaContainer)
        playerView = findViewById(R.id.playerView)
        imageView = findViewById(R.id.imageView)
        imgLiveStream = findViewById(R.id.imgLiveStream)
        imgCornerLogo = findViewById(R.id.imgCornerLogo)
        qrOverlay = findViewById(R.id.qrOverlay)
        imgQrCode = findViewById(R.id.imgQrCode)
        txtIpAddress = findViewById(R.id.txtIpAddress)
        txtStatus = findViewById(R.id.txtStatus)
        btnSettings = findViewById(R.id.btnSettings)
        btnCloseQr = findViewById(R.id.btnCloseQr)
        findMeOverlay = findViewById(R.id.findMeOverlay)
        txtFindMeTag = findViewById(R.id.txtFindMeTag)

        setupNoNetworkWifiButton()
        updateWelcomeScreenLayout()
        setupBlurBackgroundAndVideoSurface()
        setupWebPlayerAndQrOverlay()

        val rootFrame = mediaContainer.parent as? FrameLayout ?: mediaContainer
        animatedOverlayView = AnimatedOverlayView(this)
        rootFrame.addView(animatedOverlayView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        try {
            val vName = packageManager.getPackageInfo(packageName, 0).versionName
            txtStatus.text = "Sürüm: v$vName | Kumandadan QR Okutun veya Ağı Tarayın"
        } catch (_: Exception) {}

        initPlayer()
        loadSavedState()

        btnSettings.setOnClickListener { showQrOverlay() }
        btnCloseQr.setOnClickListener {
            if (getActivePlaylist().isNotEmpty()) { hideQrOverlay(); startPlayback() }
        }

        startLocalServer()
        startUdpBeacon()
        handler.post(ipCheckRunnable)
        checkForUpdate(manualTrigger = false)
    }

    // Android TV'de yüksek çözünürlüklü fotoğrafların RAM/GPU taşmasına yol açmadan güvenli yüklenmesi
    private fun loadScaledImageForDisplay(file: File, expectedFileName: String) {
        blurExecutor.execute {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val (realW, realH) = getRealScreenSize()
                val maxW = realW.coerceIn(720, 1920)
                val maxH = realH.coerceIn(720, 1920)

                var sampleSize = 1
                while ((bounds.outWidth / sampleSize) > maxW * 1.5 || (bounds.outHeight / sampleSize) > maxH * 1.5) {
                    sampleSize *= 2
                }

                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(file.absolutePath, opts)
                if (decoded != null) {
                    val scale = minOf(
                        maxW.toFloat() / decoded.width.coerceAtLeast(1),
                        maxH.toFloat() / decoded.height.coerceAtLeast(1),
                        1f
                    )
                    val finalBmp = if (scale < 0.99f) {
                        val sw = (decoded.width * scale).toInt().coerceAtLeast(1)
                        val sh = (decoded.height * scale).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(decoded, sw, sh, true)
                        if (scaled != decoded) decoded.recycle()
                        scaled
                    } else decoded

                    runOnUiThread {
                        val activeList = getActivePlaylist()
                        val cur = if (activeList.isNotEmpty()) activeList[currentIndex % activeList.size] else null
                        if (!isScreenSharing && cur?.fileName == expectedFileName && imageView.visibility == View.VISIBLE) {
                            imageView.setImageBitmap(finalBmp)
                        }
                    }
                }
            } catch (_: Throwable) {
                runOnUiThread { Toast.makeText(this, "Fotoğraf yüklenirken bellek hatası oluştu.", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun setupNoNetworkWifiButton() {
        val parentGroup = btnCloseQr.parent as? ViewGroup ?: return
        val density = resources.displayMetrics.density
        val wifiBtn = Button(this).apply {
            text = "📡 Ağ Erişimi Yok. Wifi ayarlarını aç"
            isAllCaps = false; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#D32F2F"))
            setPadding((20 * density).toInt(), (10 * density).toInt(), (20 * density).toInt(), (10 * density).toInt())
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = (8 * density).toInt(); bottomMargin = (8 * density).toInt()
            }
            setOnClickListener { openWifiSettingsPopup() }
        }
        parentGroup.addView(wifiBtn, parentGroup.indexOfChild(btnCloseQr).coerceAtLeast(0))
        btnWifiSettings = wifiBtn
    }

    private fun openWifiSettingsPopup() {
        isWifiPopupOpen = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startActivity(Intent(Settings.Panel.ACTION_WIFI))
            else startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            catch (_: Exception) { isWifiPopupOpen = false; Toast.makeText(this, "Wi-Fi ayarları açılamadı!", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateNoNetworkButtonVisibility() {
        val hasNetwork = getActiveLocalIpAddress() != "127.0.0.1"
        val noMedia = getActivePlaylist().isEmpty()
        btnWifiSettings?.visibility = if (!hasNetwork && noMedia && qrOverlay.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

    private fun getRealScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION") windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun restartMenuboardApp() {
        runOnUiThread {
            Toast.makeText(this, "🔄 Menuboard yeniden başlatılıyor...", Toast.LENGTH_SHORT).show()
            handler.postDelayed({
                try {
                    isBroadcasting = false; httpServer?.stop(); exoPlayer?.release()
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    startActivity(Intent.makeRestartActivityTask(launchIntent?.component))
                    Runtime.getRuntime().exit(0)
                } catch (_: Exception) {}
            }, 350L)
        }
    }

    private fun updateWelcomeScreenLayout() {
        if (!::qrOverlay.isInitialized) return
        val isLandscape = screenOrientationMode == "landscape"
        val density = resources.displayMetrics.density

        qrOverlay.setPadding(((if (isLandscape) 16 else 24) * density).toInt(), ((if (isLandscape) 8 else 24) * density).toInt(), ((if (isLandscape) 16 else 24) * density).toInt(), ((if (isLandscape) 8 else 24) * density).toInt())
        val qrSizePx = ((if (isLandscape) 130 else 230) * density).toInt()
        imgQrCode.layoutParams = (imgQrCode.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            width = qrSizePx; height = qrSizePx
            topMargin = ((if (isLandscape) 4 else 16) * density).toInt()
            bottomMargin = ((if (isLandscape) 4 else 16) * density).toInt()
        } ?: imgQrCode.layoutParams.apply { width = qrSizePx; height = qrSizePx }

        btnCloseQr.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isLandscape) 13f else 15f)
        btnCloseQr.setPadding(((if (isLandscape) 20 else 28) * density).toInt(), ((if (isLandscape) 6 else 12) * density).toInt(), ((if (isLandscape) 20 else 28) * density).toInt(), ((if (isLandscape) 6 else 12) * density).toInt())
        (btnCloseQr.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            topMargin = ((if (isLandscape) 4 else 14) * density).toInt()
            bottomMargin = ((if (isLandscape) 4 else 10) * density).toInt()
        }

        fun styleWelcomeViews(v: View) {
            if (v is ImageView && v.id != R.id.imgQrCode) {
                val logoH = ((if (isLandscape) 34 else 56) * density).toInt()
                v.layoutParams = (v.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                    height = logoH; topMargin = ((if (isLandscape) 4 else 12) * density).toInt(); bottomMargin = ((if (isLandscape) 2 else 8) * density).toInt()
                } ?: v.layoutParams.apply { height = logoH }
            } else if (v is TextView && v !is Button) {
                v.gravity = Gravity.CENTER; v.textAlignment = View.TEXT_ALIGNMENT_CENTER
                val curTxt = v.text?.toString() ?: ""
                if (curTxt.contains("ANYPAY", ignoreCase = true) || curTxt.contains("MENUBOARD YAYIN", ignoreCase = true)) {
                    v.text = "MENUBOARD YAYIN SİSTEMİ"
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isLandscape) 19f else 26f)
                    (v.layoutParams as? LinearLayout.LayoutParams)?.apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = ((if (isLandscape) 2 else 8) * density).toInt(); bottomMargin = ((if (isLandscape) 4 else 12) * density).toInt() }
                } else if (v.id == R.id.txtIpAddress) {
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isLandscape) 15f else 20f)
                } else if (v.id == R.id.txtStatus) {
                    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isLandscape) 11f else 14f)
                }
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) styleWelcomeViews(v.getChildAt(i))
        }
        styleWelcomeViews(qrOverlay)
        qrOverlay.requestLayout()
    }

    private fun getActivePlaylist(): List<MediaItemModel> = playlist.filter { !it.isHidden }

    private fun applyImmersiveMode(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(!enabled)
            val controller = window.insetsController
            if (enabled) {
                controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (enabled) {
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            } else View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode(if (isScreenSharing) true else immersiveModeEnabled)
    }

    private fun updateBlurVisualEffects() {
        val alpha = (blurDarknessPercent.coerceIn(0, 85) * 255) / 100
        if (alpha > 0) bgBlurImageView.setColorFilter(Color.argb(alpha, 0, 0, 0), PorterDuff.Mode.SRC_OVER)
        else bgBlurImageView.clearColorFilter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (blurBackgroundEnabled) {
                val radius = when (blurIntensityLevel) { 0 -> 28f; 2 -> 95f; else -> 55f }
                bgBlurImageView.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
            } else bgBlurImageView.setRenderEffect(null)
        }
    }

    private fun updateVideoTextureSize() {
        if (currentVideoWidth <= 0 || currentVideoHeight <= 0) return
        val containerW = mediaContainer.width; val containerH = mediaContainer.height
        if (containerW <= 0 || containerH <= 0) return
        val videoRatio = currentVideoWidth.toFloat() / currentVideoHeight.toFloat()
        val containerRatio = containerW.toFloat() / containerH.toFloat()
        val targetW: Int; val targetH: Int
        if (videoRatio > containerRatio) { targetW = containerW; targetH = (containerW / videoRatio).toInt() }
        else { targetH = containerH; targetW = (containerH * videoRatio).toInt() }
        videoTextureView.layoutParams = FrameLayout.LayoutParams(targetW, targetH, Gravity.CENTER)
    }

    private fun setupBlurBackgroundAndVideoSurface() {
        bgBlurImageView = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; visibility = View.GONE }
        updateBlurVisualEffects()
        mediaContainer.addView(bgBlurImageView, 0, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        videoTextureView = TextureView(this).apply { visibility = View.GONE }
        mediaContainer.addView(videoTextureView, 1, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        mediaContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> if (videoTextureView.visibility == View.VISIBLE) updateVideoTextureSize() }
        imageView.setBackgroundColor(Color.TRANSPARENT)
        playerView.visibility = View.GONE
    }

    private fun applyBlurBackgroundForImageFile(file: File) {
        if (!blurBackgroundEnabled) { bgBlurImageView.visibility = View.GONE; bgBlurImageView.setImageDrawable(null); return }
        updateBlurVisualEffects()
        val expectedFileName = file.name
        val intensity = blurIntensityLevel
        val quality = blurQualityLevel
        blurExecutor.execute {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sample = if (quality == 0) 4 else 2
                while ((bounds.outWidth / sample) > 800 || (bounds.outHeight / sample) > 800) sample *= 2
                val rawBmp = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                if (rawBmp != null) {
                    val resultBmp: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val maxDim = when (quality) { 0 -> 280; 2 -> 540; else -> 400 }
                        val scale = (maxDim.toFloat() / maxOf(rawBmp.width, rawBmp.height).coerceAtLeast(1)).coerceAtMost(1f)
                        val scaled = Bitmap.createScaledBitmap(rawBmp, (rawBmp.width * scale).toInt().coerceAtLeast(32), (rawBmp.height * scale).toInt().coerceAtLeast(32), true)
                        if (scaled != rawBmp) rawBmp.recycle()
                        scaled
                    } else {
                        val maxDim = when (quality) { 0 -> 140; 2 -> 280; else -> 200 }
                        val radius = when (intensity) { 0 -> 6; 2 -> 22; else -> 12 }
                        val scale = (maxDim.toFloat() / maxOf(rawBmp.width, rawBmp.height).coerceAtLeast(1)).coerceAtMost(1f)
                        val small = Bitmap.createScaledBitmap(rawBmp, (rawBmp.width * scale).toInt().coerceAtLeast(32), (rawBmp.height * scale).toInt().coerceAtLeast(32), true)
                        val blurred = fastBlur(small, radius)
                        if (small != rawBmp) small.recycle(); rawBmp.recycle()
                        blurred
                    }
                    runOnUiThread {
                        val activeList = getActivePlaylist()
                        val cur = if (activeList.isNotEmpty()) activeList[currentIndex % activeList.size] else null
                        if (blurBackgroundEnabled && !isScreenSharing && cur?.fileName == expectedFileName) {
                            updateBlurVisualEffects(); bgBlurImageView.setImageBitmap(resultBmp); bgBlurImageView.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun fastBlur(source: Bitmap, radius: Int): Bitmap {
        val w = source.width.coerceAtLeast(1); val h = source.height.coerceAtLeast(1)
        val bmp = source.copy(Bitmap.Config.ARGB_8888, true)
        val pix = IntArray(w * h); bmp.getPixels(pix, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h); val r = radius.coerceIn(1, 25)
        repeat(3) {
            for (y in 0 until h) {
                val rowOff = y * w; var rSum = 0; var gSum = 0; var bSum = 0; val wm = w - 1
                for (i in -r..r) { val c = pix[rowOff + i.coerceIn(0, wm)]; rSum += (c shr 16) and 0xFF; gSum += (c shr 8) and 0xFF; bSum += c and 0xFF }
                val div = r * 2 + 1
                for (x in 0 until w) {
                    tmp[rowOff + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                    val cOut = pix[rowOff + (x - r).coerceIn(0, wm)]; val cIn = pix[rowOff + (x + r + 1).coerceIn(0, wm)]
                    rSum += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF); gSum += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF); bSum += (cIn and 0xFF) - (cOut and 0xFF)
                }
            }
            for (x in 0 until w) {
                var rSum = 0; var gSum = 0; var bSum = 0; val hm = h - 1
                for (i in -r..r) { val c = tmp[i.coerceIn(0, hm) * w + x]; rSum += (c shr 16) and 0xFF; gSum += (c shr 8) and 0xFF; bSum += c and 0xFF }
                val div = r * 2 + 1
                for (y in 0 until h) {
                    pix[y * w + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                    val cOut = tmp[(y - r).coerceIn(0, hm) * w + x]; val cIn = tmp[(y + r + 1).coerceIn(0, hm) * w + x]
                    rSum += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF); gSum += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF); bSum += (cIn and 0xFF) - (cOut and 0xFF)
                }
            }
        }
        bmp.setPixels(pix, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun applyScreenOrientation(mode: String) {
        screenOrientationMode = if (mode == "landscape") "landscape" else "portrait"
        requestedOrientation = if (screenOrientationMode == "landscape") ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        updateWelcomeScreenLayout()
    }

    private fun setupWebPlayerAndQrOverlay() {
        webView = WebView(this).apply {
            visibility = View.GONE; setBackgroundColor(Color.BLACK); setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.useWideViewPort = true
            settings.loadWithOverviewMode = true; settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onPostLinks(jsonArrayStr: String) {
                    runOnUiThread {
                        if (activeWebItem?.type != "instagram") return@runOnUiThread
                        try {
                            val links: List<String> = gson.fromJson(jsonArrayStr, object : TypeToken<List<String>>() {}.type)
                            igPostUrls.clear(); igPostUrls.addAll(links.distinct()); igPostIndex = 0; playNextInstagramPost()
                        } catch (_: Exception) {}
                    }
                }
                @JavascriptInterface
                fun nextPost() { runOnUiThread { if (activeWebItem?.type == "instagram") playNextInstagramPost() } }
            }, "AnypayIg")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val item = activeWebItem ?: return
                    if (view?.visibility == View.VISIBLE) {
                        if (item.type == "instagram") {
                            val u = url ?: ""
                            if (u.contains("/p/") || u.contains("/reel/")) injectInstagramPostPlayerJs(view, item)
                            else injectInstagramProfileCollectorJs(view)
                        } else {
                            injectWebCustomStyles(view, item)
                            if (item.autoScroll || item.scrollUntilEnd) {
                                handler.removeCallbacks(webScrollRunnable); webScrollBottomStrikes = 0; webScrollActive = true
                                handler.postDelayed(webScrollRunnable, 2000L)
                            }
                        }
                    }
                }
            }
        }
        mediaContainer.addView(webView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        webQrContainer = FrameLayout(this).apply { visibility = View.GONE; setBackgroundColor(Color.WHITE); elevation = 16f }
        imgWebQr = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        webQrContainer.addView(imgWebQr, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        mediaContainer.addView(webQrContainer)
    }

    private fun playNextInstagramPost() {
        handler.removeCallbacks(igSafetyRunnable)
        val item = activeWebItem ?: return
        if (item.type != "instagram") return
        if (igPostUrls.isEmpty()) { if (item.scrollUntilEnd) { handler.removeCallbacks(mediaEndRunnable); scheduleNextMedia() }; return }
        if (igPostIndex >= igPostUrls.size) {
            if (item.scrollUntilEnd) { handler.removeCallbacks(mediaEndRunnable); scheduleNextMedia(); return }
            else igPostIndex = 0
        }
        webView.loadUrl(igPostUrls[igPostIndex++])
        handler.postDelayed(igSafetyRunnable, 65000L)
    }

    private fun injectInstagramProfileCollectorJs(view: WebView) {
        val js = "(function(){ if(window._anypayIgCollected) return; window._anypayIgCollected=true; setTimeout(function(){ var anchors=document.querySelectorAll('a[href*=\"/p/\"], a[href*=\"/reel/\"]'); var urls=[]; anchors.forEach(function(a){ if(a.href && urls.indexOf(a.href)===-1) urls.push(a.href); }); if(window.AnypayIg) window.AnypayIg.onPostLinks(JSON.stringify(urls.slice(0,20))); }, 2500); })();"
        view.evaluateJavascript(js, null)
    }

    private fun injectInstagramPostPlayerJs(view: WebView, item: MediaItemModel) {
        val filter = item.igMediaFilter ?: "all"
        val slideMs = (if (item.igSlideSec > 0) item.igSlideSec else 8) * 1000
        val audioEnabled = item.igAudioEnabled
        val blurBg = blurBackgroundEnabled && blurApplyInstagram
        val darkAlpha = String.format(java.util.Locale.US, "%.2f", blurDarknessPercent.coerceIn(0, 85) / 100.0)
        val videoLongDim = when (blurQualityLevel) { 0 -> 140; 2 -> 280; else -> 200 }
        val videoBlurPx = when (blurIntensityLevel) { 0 -> 6; 2 -> 20; else -> 12 }
        val photoBlurPx = when (blurIntensityLevel) { 0 -> 12; 2 -> 40; else -> 24 }
        val js = """
            (function() {
                if (window._anypayPostPlaying) return;
                window._anypayPostPlaying = true;
                var filter = '$filter', slideMs = $slideMs, audioEnabled = $audioEnabled, blurBg = $blurBg, darkAlpha = $darkAlpha, vLong = $videoLongDim, vBlur = $videoBlurPx, pBlur = $photoBlurPx;
                setTimeout(function() {
                    var vid = document.querySelector('video');
                    if (vid) {
                        if (filter === 'photo') { if (window.AnypayIg) window.AnypayIg.nextPost(); return; }
                        var ov = document.createElement('div');
                        ov.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:#000;z-index:2147483647;display:flex;align-items:center;justify-content:center;overflow:hidden;';
                        document.body.appendChild(ov);
                        var bgStopped = false, rafId = null;
                        if (blurBg) {
                            var bgCanvas = document.createElement('canvas'); bgCanvas.width = vLong; bgCanvas.height = vLong;
                            bgCanvas.style.cssText = 'position:absolute;top:0;left:0;width:100vw;height:100vh;object-fit:cover;z-index:1;pointer-events:none;image-rendering:auto;transform:translateZ(0);';
                            ov.appendChild(bgCanvas);
                            if (darkAlpha > 0) { var dim = document.createElement('div'); dim.style.cssText = 'position:absolute;top:0;left:0;width:100vw;height:100vh;background:rgba(0,0,0,' + darkAlpha + ');z-index:2;pointer-events:none;'; ov.appendChild(dim); }
                            var ctx = bgCanvas.getContext('2d', { alpha: false }), sized = false;
                            if (ctx) {
                                ctx.imageSmoothingEnabled = true;
                                function renderFrame() {
                                    if (bgStopped) return;
                                    try {
                                        if (!vid.paused && !vid.ended && vid.videoWidth > 0 && vid.videoHeight > 0) {
                                            if (!sized) {
                                                var ratio = vid.videoWidth / vid.videoHeight;
                                                if (ratio >= 1) { bgCanvas.width = vLong; bgCanvas.height = Math.max(48, Math.round(vLong / ratio)); }
                                                else { bgCanvas.height = vLong; bgCanvas.width = Math.max(48, Math.round(vLong * ratio)); }
                                                sized = true;
                                            }
                                            var pad = vBlur * 2; ctx.filter = 'blur(' + vBlur + 'px)';
                                            ctx.drawImage(vid, -pad, -pad, bgCanvas.width + pad * 2, bgCanvas.height + pad * 2);
                                        }
                                    } catch (e) { bgStopped = true; return; }
                                    if (typeof vid.requestVideoFrameCallback === 'function') vid.requestVideoFrameCallback(renderFrame); else rafId = requestAnimationFrame(renderFrame);
                                }
                                if (typeof vid.requestVideoFrameCallback === 'function') vid.requestVideoFrameCallback(renderFrame); else rafId = requestAnimationFrame(renderFrame);
                            }
                        }
                        vid.style.cssText = 'width:100vw !important;height:100vh !important;object-fit:contain !important;background:transparent !important;position:relative !important;z-index:3 !important;display:block !important;';
                        vid.setAttribute('playsinline', ''); vid.setAttribute('webkit-playsinline', ''); vid.loop = false; vid.muted = !audioEnabled; vid.volume = 1.0;
                        ov.appendChild(vid);
                        function finishVideo() { bgStopped = true; if (rafId !== null) cancelAnimationFrame(rafId); if (window.AnypayIg) window.AnypayIg.nextPost(); }
                        vid.onended = finishVideo; vid.onerror = finishVideo;
                        vid.play().catch(function() { vid.muted = true; vid.play().catch(finishVideo); });
                        return;
                    }
                    if (filter === 'video') { if (window.AnypayIg) window.AnypayIg.nextPost(); return; }
                    var imgs = document.querySelectorAll('article img, main img'), bestImg = null, maxArea = 0;
                    imgs.forEach(function(im) { var a = (im.naturalWidth || 0) * (im.naturalHeight || 0); if (im.naturalWidth > 200 && a > maxArea) { maxArea = a; bestImg = im; } });
                    if (bestImg && bestImg.src) {
                        var ovImg = document.createElement('div');
                        ovImg.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:#000;z-index:2147483647;display:flex;align-items:center;justify-content:center;overflow:hidden;';
                        if (blurBg) {
                            var bgIm = document.createElement('img'); bgIm.src = bestImg.src;
                            bgIm.style.cssText = 'position:absolute;top:-20px;left:-20px;width:calc(100vw + 40px);height:calc(100vh + 40px);object-fit:cover;filter:blur(' + pBlur + 'px);z-index:1;';
                            ovImg.appendChild(bgIm);
                            if (darkAlpha > 0) { var dimImg = document.createElement('div'); dimImg.style.cssText = 'position:absolute;top:0;left:0;width:100vw;height:100vh;background:rgba(0,0,0,' + darkAlpha + ');z-index:2;'; ovImg.appendChild(dimImg); }
                        }
                        var el = document.createElement('img'); el.src = bestImg.src; el.style.cssText = 'position:relative;z-index:3;width:100vw;height:100vh;object-fit:contain;';
                        ovImg.appendChild(el); document.body.appendChild(ovImg);
                        setTimeout(function() { if (window.AnypayIg) window.AnypayIg.nextPost(); }, slideMs);
                    } else if (window.AnypayIg) window.AnypayIg.nextPost();
                }, 1800);
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    private fun injectWebCustomStyles(view: WebView, item: MediaItemModel) {
        val cssRules = StringBuilder()
        val fontFamily = item.webFontFamily ?: "default"
        val googleFonts = setOf("Roboto", "Open Sans", "Montserrat", "Poppins", "Lato", "Oswald", "Playfair Display", "Merriweather")
        val googleFontLinkJs = if (fontFamily in googleFonts) {
            "if(!document.getElementById('anypay-gfont')){ var l=document.createElement('link'); l.id='anypay-gfont'; l.rel='stylesheet'; l.href='https://fonts.googleapis.com/css2?family=${fontFamily.replace(" ", "+")}:wght@400;700;900&subset=latin-ext&display=swap'; document.head.appendChild(l); }"
        } else ""
        if (fontFamily != "default" && fontFamily.isNotEmpty()) {
            val cssFont = if (fontFamily in googleFonts) "'$fontFamily', sans-serif" else fontFamily
            cssRules.append("body, p, div, span, li, td, th, a, h1, h2, h3, h4, h5, h6 { font-family: $cssFont !important; } ")
        }
        if (item.webBoldHeaders) cssRules.append("h1, h2, h3, h4, h5, h6, strong, b, .title, .header, [class*='title'], [class*='head'] { font-weight: 900 !important; } ")
        if (item.webImageScalePercent in 25..250 && item.webImageScalePercent != 100) cssRules.append("img { zoom: ${item.webImageScalePercent}% !important; } ")

        val customSelector = (item.webFixedSelector ?: "").trim().replace("\\", "\\\\").replace("'", "\\'")
        val fixedHeaderText = if (item.webFixedHeaderTextEnabled) (item.webFixedHeaderText ?: "").trim().take(60).replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ") else ""
        val fixedHeaderJs = if (item.webFixedHeader) {
            """
            function anypayPinTopBanner() {
                if (window._anypayHeaderPinned) return;
                var sel = '$customSelector', fixedTxt = '$fixedHeaderText', el = null;
                if (sel) { try { el = document.querySelector(sel); } catch(e) {} }
                if (!el) {
                    var std = document.querySelectorAll('header, nav, .navbar, .site-header, .topbar, [role="banner"]');
                    for (var i = 0; i < std.length; i++) { var r = std[i].getBoundingClientRect(); if (r.top <= 20 && r.height >= 40 && r.height <= 300 && r.width >= window.innerWidth * 0.8) { el = std[i]; break; } }
                }
                if (!el) {
                    var cands = document.querySelectorAll('div, section');
                    for (var j = 0; j < cands.length; j++) { var rect = cands[j].getBoundingClientRect(); if (rect.top >= -2 && rect.top <= 18 && rect.height >= 55 && rect.height <= 300 && rect.width >= window.innerWidth * 0.85) { el = cands[j]; break; } }
                }
                if (el) {
                    var h = Math.ceil(el.getBoundingClientRect().height);
                    el.style.setProperty('position', 'fixed', 'important'); el.style.setProperty('top', '0px', 'important'); el.style.setProperty('left', '0px', 'important');
                    el.style.setProperty('right', '0px', 'important'); el.style.setProperty('width', '100%', 'important'); el.style.setProperty('z-index', '999999', 'important');
                    el.style.setProperty('margin-top', '0px', 'important'); document.body.style.setProperty('padding-top', h + 'px', 'important');
                    if (fixedTxt && !document.getElementById('anypay-fixed-banner-text')) {
                        var txtDiv = document.createElement('div'); txtDiv.id = 'anypay-fixed-banner-text'; txtDiv.textContent = fixedTxt;
                        txtDiv.style.cssText = 'position:absolute !important;left:8px !important;right:8px !important;bottom:2px !important;text-align:center !important;font-size:11px !important;font-weight:700 !important;letter-spacing:0.6px !important;line-height:1.1 !important;color:#333 !important;white-space:nowrap !important;overflow:hidden !important;text-overflow:ellipsis !important;pointer-events:none !important;z-index:1000000 !important;';
                        el.appendChild(txtDiv);
                    }
                    window._anypayHeaderPinned = true;
                }
            }
            anypayPinTopBanner(); setTimeout(anypayPinTopBanner, 600); setTimeout(anypayPinTopBanner, 1500);
            """.trimIndent()
        } else ""

        if (cssRules.isNotEmpty() || googleFontLinkJs.isNotEmpty() || fixedHeaderJs.isNotEmpty()) {
            val safeCss = cssRules.toString().replace("'", "\\'")
            view.evaluateJavascript("(function(){ $googleFontLinkJs var s = document.getElementById('anypay-custom-style'); if(!s){ s = document.createElement('style'); s.id='anypay-custom-style'; document.head.appendChild(s); } s.innerHTML = '$safeCss'; $fixedHeaderJs })();", null)
        }
    }

    override fun onResume() {
        super.onResume()
        currentTaskId = taskId; isWifiPopupOpen = false; updateNoNetworkButtonVisibility()
        applyImmersiveMode(if (isScreenSharing) true else immersiveModeEnabled)
        if (awaitingSequentialPermissionChain) {
            val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
            val hasAcc = isAccessibilityServiceEnabled()
            if (hasOverlay && !hasAcc) {
                awaitingSequentialPermissionChain = false
                Toast.makeText(this, "Şimdi 2. Adım: 'Anypay Uzaktan Mouse ve Kontrol' erişilebilirlik iznini aktif edin.", Toast.LENGTH_LONG).show()
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
            } else if (hasOverlay && hasAcc) {
                awaitingSequentialPermissionChain = false
                Toast.makeText(this, "Tüm kontrol izinleri tamamlandı! Mouse & Klavye hazır.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (MenuboardControlService.instance != null) return true
        val expected = "$packageName/${MenuboardControlService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(expected, ignoreCase = true) || enabled.contains("$packageName/.MenuboardControlService", ignoreCase = true)
    }

    private fun promptMissingPermissionsSequentially() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        val hasAcc = isAccessibilityServiceEnabled()
        runOnUiThread {
            if (!hasOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                awaitingSequentialPermissionChain = true
                Toast.makeText(this, "1. Adım: Lütfen 'Diğer uygulamaların üzerinde göster' iznini açıp geri dönün.", Toast.LENGTH_LONG).show()
                try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
            } else if (!hasAcc) {
                awaitingSequentialPermissionChain = false
                Toast.makeText(this, "Lütfen listeden 'Anypay Uzaktan Mouse ve Kontrol' seçeneğini aktif edin.", Toast.LENGTH_LONG).show()
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
            }
        }
    }

    private fun bringMenuboardToFront() {
        MenuboardControlService.instance?.bringPackageToFront(packageName)
        runOnUiThread {
            try {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) am?.appTasks?.forEach { it.moveToFront() }
                val frontIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN; addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                val pi = PendingIntent.getActivity(applicationContext, 999, frontIntent, flags)
                try { pi.send() } catch (_: Exception) {}
                startActivity(frontIntent)
                try {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val chId = "menuboard_front_ch"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) nm.createNotificationChannel(NotificationChannel(chId, "Menuboard Öne Getirme", NotificationManager.IMPORTANCE_HIGH))
                    val notif = NotificationCompat.Builder(applicationContext, chId)
                        .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Menuboard Yayın Sistemi")
                        .setContentText("Menuboard ekranı öne getiriliyor...").setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL).setFullScreenIntent(pi, true).setAutoCancel(true).build()
                    nm.notify(777, notif); handler.postDelayed({ nm.cancel(777) }, 1200L)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    private fun checkForUpdate(manualTrigger: Boolean = false) {
        if (GITHUB_REPO.startsWith("KULLANICI_ADINIZ")) {
            if (manualTrigger) runOnUiThread { Toast.makeText(this, "GitHub depo adresi tanımlı değil!", Toast.LENGTH_LONG).show() }
            return
        }
        val req = Request.Builder().url("https://api.github.com/repos/$GITHUB_REPO/releases/latest").header("User-Agent", "Anypay-Menuboard-App").header("Accept", "application/vnd.github+json").build()
        updateHttpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (manualTrigger) runOnUiThread { Toast.makeText(this@MainActivity, "Güncelleme sunucusuna ulaşılamadı!", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (!response.isSuccessful) return
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val rawTag = json.optString("tag_name", "v1.00")
                    val remoteCode = rawTag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 100
                    val curCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt() else @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0).versionCode
                    if (remoteCode > curCode) {
                        val assets = json.optJSONArray("assets") ?: return
                        var dlUrl = ""
                        for (i in 0 until assets.length()) { val a = assets.getJSONObject(i); val n = a.getString("name").lowercase(); if (n.contains("menuboard") && n.endsWith(".apk")) { dlUrl = a.getString("browser_download_url"); break } }
                        if (dlUrl.isNotEmpty()) runOnUiThread { if (manualTrigger) downloadAndInstallApk(dlUrl) else showUpdateDialog(rawTag, dlUrl) }
                    } else if (manualTrigger) runOnUiThread { Toast.makeText(this@MainActivity, "Menuboard zaten en güncel sürümde!", Toast.LENGTH_SHORT).show() }
                } catch (_: Exception) {}
            }
        })
    }

    private fun showUpdateDialog(versionTitle: String, downloadUrl: String) {
        AlertDialog.Builder(this).setTitle("Yeni Güncelleme Bulundu").setMessage("Menuboard için yeni sürüm mevcut ($versionTitle).\nŞimdi yüklemek ister misiniz?")
            .setPositiveButton("Güncelle") { _, _ -> downloadAndInstallApk(downloadUrl) }.setNegativeButton("Daha Sonra", null).show()
    }

    private fun downloadAndInstallApk(url: String) {
        Toast.makeText(this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "menuboard_update_${System.currentTimeMillis()}.apk")
        if (destination.exists()) destination.delete()
        val req = DownloadManager.Request(Uri.parse(url)).setTitle("Menuboard Güncellemesi").setDescription("İndiriliyor...").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationUri(Uri.fromFile(destination))
        val dlId = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == dlId) { unregisterReceiver(this); installLocalApkFile(destination) }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        else registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installLocalApkFile(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", apkFile)
            startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(apkUri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) { runOnUiThread { Toast.makeText(this, "APK Kurulum Hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show() } }
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setVideoTextureView(videoTextureView)
            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        currentVideoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt()
                        currentVideoHeight = videoSize.height; updateVideoTextureSize()
                    }
                }
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) scheduleNextMedia() }
            })
        }
    }

    private fun loadSavedState() {
        val prefs = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
        deviceId = prefs.getString("device_id", "") ?: ""
        if (deviceId.isEmpty()) { deviceId = UUID.randomUUID().toString().take(8); prefs.edit().putString("device_id", deviceId).apply() }
        deviceTag = prefs.getString("device_tag", "Menuboard") ?: "Menuboard"
        activeBgObject = prefs.getString("bg_object", "none") ?: "none"
        animatedOverlayView.setAnimationMode(activeBgObject)
        val json = prefs.getString("playlist_json", null)
        if (!json.isNullOrEmpty()) playlist = gson.fromJson(json, object : TypeToken<MutableList<MediaItemModel>>() {}.type)
        if (getActivePlaylist().isNotEmpty()) { hideQrOverlay(); startPlayback() } else showQrOverlay()
    }

    private fun startLocalServer() {
        currentKnownIp = getActiveLocalIpAddress()
        txtIpAddress.text = if (currentKnownIp != "127.0.0.1") "IP: $currentKnownIp:8080" else "Ağ Erişimi Yok"
        if (currentKnownIp != "127.0.0.1") generateQr(currentKnownIp, deviceId)
        updateNoNetworkButtonVisibility()
        if (httpServer == null) { httpServer = SignageServer(8080); httpServer?.start(600000, false) }
    }

    private fun startUdpBeacon() {
        Thread {
            val socket = DatagramSocket(); socket.broadcast = true
            while (isBroadcasting) {
                try {
                    val ip = getActiveLocalIpAddress()
                    if (ip != "127.0.0.1") {
                        val bytes = "ANYPAY_DISCOVERY:$deviceId:$deviceTag:$ip:8080".toByteArray()
                        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), 8888))
                    }
                } catch (_: Exception) {}
                Thread.sleep(3000L)
            }
        }.start()
    }

    private fun getActiveLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) return addr.hostAddress ?: "127.0.0.1"
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun generateQr(ip: String, id: String) {
        try { imgQrCode.setImageBitmap(BarcodeEncoder().encodeBitmap("{\"id\":\"$id\",\"tag\":\"$deviceTag\",\"ip\":\"$ip\",\"port\":8080}", BarcodeFormat.QR_CODE, 500, 500)) } catch (_: Exception) {}
    }

    private fun showQrOverlay() { stopAllPlayback(); imgCornerLogo.visibility = View.GONE; qrOverlay.visibility = View.VISIBLE; updateWelcomeScreenLayout(); updateNoNetworkButtonVisibility() }
    private fun hideQrOverlay() { qrOverlay.visibility = View.GONE; imgCornerLogo.visibility = View.VISIBLE; btnWifiSettings?.visibility = View.GONE }

    private fun stopAllPlayback() {
        handler.removeCallbacks(mediaEndRunnable); handler.removeCallbacks(webScrollRunnable); handler.removeCallbacks(igSafetyRunnable); handler.removeCallbacks(videoBlurRunnable)
        igPostUrls.clear(); webScrollActive = false; webScrollBottomStrikes = 0; activeWebItem = null
        exoPlayer?.stop(); exoPlayer?.clearMediaItems()
        bgBlurImageView.visibility = View.GONE; bgBlurImageView.setImageDrawable(null)
        videoTextureView.visibility = View.GONE; playerView.visibility = View.GONE
        imageView.visibility = View.GONE; imageView.setImageDrawable(null)
        imgLiveStream.visibility = View.GONE; webView.stopLoading(); webView.visibility = View.GONE; webQrContainer.visibility = View.GONE
    }

    private fun startPlayback() {
        if (isScreenSharing) return
        val activeList = getActivePlaylist()
        if (activeList.isEmpty()) { showQrOverlay(); return }
        playItem(currentIndex % activeList.size)
    }

    private fun applyTransitionAnimation(animationType: String) {
        val anim: Animation? = when (animationType) {
            "slide_left" -> TranslateAnimation(Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f).apply { duration = 500 }
            "slide_right" -> TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f).apply { duration = 500 }
            "slide_up" -> TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f).apply { duration = 500 }
            "slide_down" -> TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f).apply { duration = 500 }
            "zoom_in" -> ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply { duration = 400 }
            "zoom_out" -> ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply { duration = 400 }
            "fade" -> AlphaAnimation(0.0f, 1.0f).apply { duration = 400 }
            else -> null
        }
        anim?.let { mediaContainer.startAnimation(it) }
    }

    private fun playItem(index: Int) {
        if (isScreenSharing) return
        handler.removeCallbacks(mediaEndRunnable); handler.removeCallbacks(webScrollRunnable); handler.removeCallbacks(igSafetyRunnable); handler.removeCallbacks(videoBlurRunnable)
        igPostUrls.clear(); webScrollActive = false; webScrollBottomStrikes = 0; activeWebItem = null

        val activeList = getActivePlaylist()
        if (activeList.isEmpty()) { showQrOverlay(); return }
        currentIndex = index % activeList.size
        val item = activeList[currentIndex]

        if (item.type == "instagram") {
            exoPlayer?.stop(); bgBlurImageView.visibility = View.GONE; videoTextureView.visibility = View.GONE; playerView.visibility = View.GONE; imageView.visibility = View.GONE; webQrContainer.visibility = View.GONE; webView.visibility = View.VISIBLE
            activeWebItem = item
            val prefs = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
            val savedCookie = prefs.getString("ig_cookie", "") ?: ""; val savedUa = prefs.getString("ig_ua", "") ?: ""
            if (savedUa.isNotEmpty()) webView.settings.userAgentString = savedUa
            if (savedCookie.isNotEmpty()) {
                val cm = CookieManager.getInstance(); cm.setAcceptCookie(true); cm.setAcceptThirdPartyCookies(webView, true)
                savedCookie.split(";").forEach { part -> val t = part.trim(); if (t.isNotEmpty()) cm.setCookie("https://www.instagram.com", t) }
                cm.flush()
            }
            val cleanUser = item.fileName.trim().removePrefix("https://www.instagram.com/").removePrefix("https://instagram.com/").removePrefix("http://www.instagram.com/").removePrefix("@").trim('/')
            webView.setInitialScale(100); webView.settings.textZoom = 100; webView.scrollTo(0, 0)
            webView.loadUrl("https://www.instagram.com/$cleanUser/")
            applyTransitionAnimation(item.animation)
            handler.postDelayed(mediaEndRunnable, if (item.scrollUntilEnd) 1800000L else (if (item.durationSec > 0) item.durationSec else 30) * 1000L)
            animatedOverlayView.bringToFront(); return
        }

        if (item.type == "web") {
            exoPlayer?.stop(); bgBlurImageView.visibility = View.GONE; videoTextureView.visibility = View.GONE; playerView.visibility = View.GONE; imageView.visibility = View.GONE; webView.visibility = View.VISIBLE
            activeWebItem = item
            val targetUrl = if (item.fileName.startsWith("http://") || item.fileName.startsWith("https://")) item.fileName else "https://${item.fileName}"
            webView.setInitialScale(if (item.webZoomPercent in 25..300) item.webZoomPercent else 100)
            webView.settings.textZoom = if (item.webFontSizePercent in 50..250) item.webFontSizePercent else 100
            webView.scrollTo(0, 0); webView.loadUrl(targetUrl); applyTransitionAnimation(item.animation)
            if (item.showQr) {
                try {
                    val d = resources.displayMetrics.density
                    val qrSizePx = ((if (item.qrSizeDp in 50..400) item.qrSizeDp else 150) * d).toInt()
                    val borderPx = ((if (item.qrBorderDp in 0..40) item.qrBorderDp else 8) * d).toInt()
                    val marginPx = (20 * d).toInt()
                    imgWebQr.setImageBitmap(BarcodeEncoder().encodeBitmap(targetUrl, BarcodeFormat.QR_CODE, qrSizePx, qrSizePx))
                    val grav = when (item.qrCorner) { "top_left" -> Gravity.TOP or Gravity.START; "top_right" -> Gravity.TOP or Gravity.END; "bottom_left" -> Gravity.BOTTOM or Gravity.START; else -> Gravity.BOTTOM or Gravity.END }
                    val box = qrSizePx + (borderPx * 2)
                    webQrContainer.setPadding(borderPx, borderPx, borderPx, borderPx)
                    webQrContainer.setBackgroundColor(if (borderPx > 0) Color.WHITE else Color.TRANSPARENT)
                    webQrContainer.layoutParams = FrameLayout.LayoutParams(box, box).apply { gravity = grav; setMargins(marginPx, marginPx, marginPx, marginPx) }
                    webQrContainer.visibility = View.VISIBLE; webQrContainer.bringToFront()
                } catch (_: Exception) { webQrContainer.visibility = View.GONE }
            } else webQrContainer.visibility = View.GONE
            handler.postDelayed(mediaEndRunnable, if (item.scrollUntilEnd) 1800000L else (if (item.durationSec > 0) item.durationSec else 20) * 1000L)
            animatedOverlayView.bringToFront(); return
        }

        webView.stopLoading(); webView.visibility = View.GONE; webQrContainer.visibility = View.GONE
        val file = File(filesDir, item.fileName)
        if (!file.exists() || file.length() == 0L) { scheduleNextMedia(); return }
        applyTransitionAnimation(item.animation)

        if (item.type == "video") {
            imageView.visibility = View.GONE; playerView.visibility = View.GONE; videoTextureView.visibility = View.VISIBLE
            if (blurBackgroundEnabled) { updateBlurVisualEffects(); handler.post(videoBlurRunnable) }
            else { bgBlurImageView.visibility = View.GONE; bgBlurImageView.setImageDrawable(null) }
            exoPlayer?.stop(); exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file))); exoPlayer?.prepare(); exoPlayer?.play()
            if (item.durationSec > 0) handler.postDelayed(mediaEndRunnable, item.durationSec * 1000L)
        } else {
            exoPlayer?.stop(); videoTextureView.visibility = View.GONE; playerView.visibility = View.GONE
            imageView.visibility = View.VISIBLE; imageView.scaleType = ImageView.ScaleType.FIT_CENTER; imageView.setBackgroundColor(Color.TRANSPARENT)
            imageView.setImageDrawable(null)
            applyBlurBackgroundForImageFile(file)
            loadScaledImageForDisplay(file, item.fileName)
            handler.postDelayed(mediaEndRunnable, (if (item.durationSec > 0) item.durationSec else 10) * 1000L)
        }
        animatedOverlayView.bringToFront()
    }

    private fun scheduleNextMedia() {
        if (isScreenSharing) return
        stopAllPlayback()
        val activeList = getActivePlaylist()
        if (activeList.isEmpty()) { showQrOverlay(); return }
        val waitTime = activeList[currentIndex % activeList.size].waitAfterSec * 1000L
        handler.postDelayed({
            val freshActive = getActivePlaylist()
            if (freshActive.isEmpty()) { showQrOverlay(); return@postDelayed }
            currentIndex = (currentIndex + 1) % freshActive.size; playItem(currentIndex)
        }, waitTime)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (qrOverlay.visibility == View.VISIBLE && getActivePlaylist().isNotEmpty()) { hideQrOverlay(); startPlayback() }
        else super.onBackPressed()
    }

    inner class SignageServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri; val method = session.method

            if (uri == "/api/autostart" && method == Method.POST) {
                val enabled = session.parameters["enabled"]?.firstOrNull() == "true"
                autoStartOnBoot = enabled
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putBoolean("auto_start_on_boot", enabled).apply()
                runOnUiThread { Toast.makeText(this@MainActivity, if (enabled) "🚀 Açılışta Otomatik Başlatma: AÇIK" else "⏹️ Açılışta Otomatik Başlatma: KAPALI", Toast.LENGTH_SHORT).show() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", enabled.toString())
            }

            if (uri == "/api/restart" && method == Method.POST) {
                restartMenuboardApp(); return newFixedLengthResponse(Response.Status.OK, "text/plain", "RESTARTING")
            }

            if (uri == "/api/immersive" && method == Method.POST) {
                val enabled = session.parameters["enabled"]?.firstOrNull() != "false"
                immersiveModeEnabled = enabled
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putBoolean("immersive_mode", enabled).apply()
                runOnUiThread { applyImmersiveMode(enabled); Toast.makeText(this@MainActivity, if (enabled) "🖥️ Tam Ekran Modu (Çubuklar Gizli)" else "📱 Sistem Çubukları Gösteriliyor", Toast.LENGTH_SHORT).show() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", enabled.toString())
            }

            if (uri == "/api/blur_bg" && method == Method.POST) {
                val enabled = session.parameters["enabled"]?.firstOrNull() == "true"
                val darkness = session.parameters["darkness"]?.firstOrNull()?.toIntOrNull()?.coerceIn(0, 85) ?: blurDarknessPercent
                val intensity = session.parameters["intensity"]?.firstOrNull()?.toIntOrNull()?.coerceIn(0, 2) ?: blurIntensityLevel
                val quality = session.parameters["quality"]?.firstOrNull()?.toIntOrNull()?.coerceIn(0, 2) ?: blurQualityLevel
                val applyIg = session.parameters["ig"]?.firstOrNull()?.let { it == "true" } ?: blurApplyInstagram
                blurBackgroundEnabled = enabled; blurDarknessPercent = darkness; blurIntensityLevel = intensity; blurQualityLevel = quality; blurApplyInstagram = applyIg
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit()
                    .putBoolean("blur_bg_enabled", enabled).putInt("blur_darkness", darkness).putInt("blur_intensity", intensity).putInt("blur_quality", quality).putBoolean("blur_apply_ig", applyIg).apply()
                runOnUiThread {
                    updateBlurVisualEffects()
                    val activeList = getActivePlaylist()
                    if (!isScreenSharing && qrOverlay.visibility != View.VISIBLE && activeList.isNotEmpty()) playItem(currentIndex % activeList.size)
                    else if (!enabled) { handler.removeCallbacks(videoBlurRunnable); bgBlurImageView.visibility = View.GONE; bgBlurImageView.setImageDrawable(null) }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", enabled.toString())
            }

            if (uri == "/api/orientation" && method == Method.POST) {
                val mode = if (session.parameters["mode"]?.firstOrNull() == "landscape") "landscape" else "portrait"
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("screen_orientation", mode).apply()
                runOnUiThread { applyScreenOrientation(mode); Toast.makeText(this@MainActivity, if (mode == "landscape") "📺 TV Modu (16:9 Yatay) Aktif" else "📱 Dikey Mod (9:16) Aktif", Toast.LENGTH_SHORT).show() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", mode)
            }

            if (uri == "/api/update/check" && method == Method.POST) {
                checkForUpdate(manualTrigger = true); return newFixedLengthResponse(Response.Status.OK, "text/plain", "CHECKING")
            }

            if (uri == "/api/instagram/status" && method == Method.GET) {
                val cookie = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).getString("ig_cookie", "") ?: ""
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(mapOf("active" to cookie.contains("sessionid="))))
            }

            if (uri == "/api/instagram/cookie" && method == Method.POST) {
                val files = HashMap<String, String>(); session.parseBody(files)
                return try {
                    val json = JSONObject(files["postData"] ?: "{}")
                    val cookie = json.optString("cookie", ""); val ua = json.optString("userAgent", "")
                    getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("ig_cookie", cookie).putString("ig_ua", ua).apply()
                    runOnUiThread {
                        val cm = CookieManager.getInstance(); cm.setAcceptCookie(true)
                        cookie.split(";").forEach { part -> val t = part.trim(); if (t.isNotEmpty()) cm.setCookie("https://www.instagram.com", t) }
                        cm.flush()
                    }
                    newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
                } catch (_: Exception) { newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "ERR") }
            }

            if (uri == "/api/permissions/check" && method == Method.POST) {
                val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this@MainActivity) else true
                val hasAcc = isAccessibilityServiceEnabled()
                val allReady = hasOverlay && hasAcc
                if (!allReady) promptMissingPermissionsSequentially()
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(mapOf("allGranted" to allReady, "overlayGranted" to hasOverlay, "accessibilityGranted" to hasAcc)))
            }

            if (uri == "/api/input" && method == Method.POST) {
                val type = session.parameters["type"]?.firstOrNull() ?: ""
                val svc = MenuboardControlService.instance
                when (type) {
                    "move" -> {
                        val dx = session.parameters["dx"]?.firstOrNull()?.toFloatOrNull() ?: 0f
                        val dy = session.parameters["dy"]?.firstOrNull()?.toFloatOrNull() ?: 0f
                        if (svc != null) svc.moveCursorBy(dx, dy)
                        else runOnUiThread {
                            val (realW, realH) = getRealScreenSize()
                            animatedOverlayView.cursorX = (animatedOverlayView.cursorX + dx).coerceIn(0f, (realW - 4).toFloat())
                            animatedOverlayView.cursorY = (animatedOverlayView.cursorY + dy).coerceIn(0f, (realH - 4).toFloat())
                            animatedOverlayView.showCursor = true; animatedOverlayView.bringToFront(); animatedOverlayView.invalidate()
                            handler.removeCallbacks(hideCursorRunnable); handler.postDelayed(hideCursorRunnable, 8000L)
                        }
                    }
                    "click" -> {
                        if (svc != null) svc.clickAtCursor()
                        else {
                            val cx = animatedOverlayView.cursorX; val cy = animatedOverlayView.cursorY
                            runOnUiThread {
                                val downTime = SystemClock.uptimeMillis()
                                val evDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, cx, cy, 0)
                                val evUp = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, cx, cy, 0)
                                window.decorView.dispatchTouchEvent(evDown); window.decorView.dispatchTouchEvent(evUp)
                                evDown.recycle(); evUp.recycle()
                            }
                        }
                    }
                    "scroll" -> svc?.scrollAtCursor(session.parameters["dy"]?.firstOrNull()?.toFloatOrNull() ?: 0f)
                    "nav" -> {
                        when (val act = session.parameters["act"]?.firstOrNull() ?: "") {
                            "MENUBOARD" -> bringMenuboardToFront()
                            "HOME_SETTINGS" -> runOnUiThread { try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {} }
                            "APP_DETAILS" -> runOnUiThread { try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", packageName, null); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {} }
                            "OVERLAY_SETTINGS" -> runOnUiThread { try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {} }
                            "ACCESSIBILITY_SETTINGS" -> runOnUiThread { try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {} }
                            else -> {
                                if (svc != null) svc.performGlobalNav(act)
                                else if (act == "HOME") startActivity(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            }
                        }
                    }
                    "text" -> {
                        val text = session.parameters["val"]?.firstOrNull() ?: ""
                        if (svc != null) svc.inputKeyboardText(text, isBackspace = false)
                        else if (text.isNotEmpty()) Thread { try { Instrumentation().sendStringSync(text) } catch (_: Exception) {} }.start()
                    }
                    "key" -> {
                        val code = session.parameters["code"]?.firstOrNull() ?: ""
                        if (svc != null && code == "BACKSPACE") svc.inputKeyboardText("", isBackspace = true)
                        else Thread { try { if (code == "BACKSPACE") Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DEL) else if (code == "ENTER") Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER) } catch (_: Exception) {} }.start()
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", if (svc != null) "OK_GLOBAL" else "NEED_SERVICE")
            }

            if (uri == "/api/sefim/bgobject" && method == Method.POST) {
                val mode = session.parameters["mode"]?.firstOrNull() ?: "none"
                activeBgObject = mode
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("bg_object", mode).apply()
                runOnUiThread { animatedOverlayView.setAnimationMode(mode); animatedOverlayView.bringToFront() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/sefim/backup" && method == Method.GET) {
                val backupFile = File(filesDir, "sefim_backup_v118.json")
                return newFixedLengthResponse(Response.Status.OK, "application/json", if (backupFile.exists()) backupFile.readText() else "")
            }

            if (uri == "/api/sefim/backup" && method == Method.POST) {
                val files = HashMap<String, String>(); session.parseBody(files)
                val postData = files["postData"] ?: ""
                if (postData.isNotEmpty()) File(filesDir, "sefim_backup_v118.json").writeText(postData)
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SAVED")
            }

            if (uri == "/api/upload_raw" && method == Method.POST) {
                val filename = session.parameters["filename"]?.firstOrNull() ?: "media_${System.currentTimeMillis()}"
                val isApk = session.parameters["apk"]?.firstOrNull() == "true"
                val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
                val targetFile = if (isApk) File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "remote_pushed_${System.currentTimeMillis()}.apk") else File(filesDir, filename)
                return try {
                    val input = session.inputStream
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(65536); var remaining = contentLength
                        while (remaining > 0) {
                            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read <= 0) break; output.write(buffer, 0, read); remaining -= read
                        }
                        output.flush()
                    }
                    if (isApk) runOnUiThread { Toast.makeText(this@MainActivity, "APK alındı, kurulum başlatılıyor...", Toast.LENGTH_LONG).show(); installLocalApkFile(targetFile) }
                    newFixedLengthResponse(Response.Status.OK, "text/plain", filename)
                } catch (e: Exception) { newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.localizedMessage ?: "ERR") }
            }

            if (uri == "/api/apps" && method == Method.GET) {
                val pm = packageManager
                val resolveInfos = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }, 0)
                val appList = mutableListOf<InstalledAppInfo>()
                for (ri in resolveInfos) {
                    val pkg = ri.activityInfo.packageName
                    if (appList.none { it.packageName == pkg }) appList.add(InstalledAppInfo(ri.loadLabel(pm).toString(), pkg))
                }
                appList.sortBy { it.appName.lowercase() }
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(appList))
            }

            if (uri == "/api/apps/launch" && method == Method.POST) {
                val pkg = session.parameters["pkg"]?.firstOrNull() ?: ""
                if (pkg.isNotEmpty()) {
                    if (pkg == packageName) { bringMenuboardToFront(); return newFixedLengthResponse(Response.Status.OK, "text/plain", "LAUNCHED") }
                    val svc = MenuboardControlService.instance
                    if (svc != null) svc.bringPackageToFront(pkg)
                    else packageManager.getLaunchIntentForPackage(pkg)?.let { li -> li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP); runOnUiThread { try { startActivity(li) } catch (_: Exception) {} } }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", "LAUNCHED")
                }
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "NOT_FOUND")
            }

            if (uri == "/api/screen/start" && method == Method.POST) {
                isScreenSharing = true
                runOnUiThread {
                    stopAllPlayback(); applyImmersiveMode(true)
                    qrOverlay.visibility = View.GONE; imgCornerLogo.visibility = View.GONE; btnSettings.visibility = View.GONE
                    imgLiveStream.setBackgroundColor(Color.BLACK); imgLiveStream.scaleType = ImageView.ScaleType.FIT_CENTER
                    imgLiveStream.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    imgLiveStream.visibility = View.VISIBLE; imgLiveStream.bringToFront()
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SCREEN_STARTED")
            }

            if (uri == "/api/screen/stop" && method == Method.POST) {
                isScreenSharing = false
                runOnUiThread {
                    imgLiveStream.visibility = View.GONE; btnSettings.visibility = View.VISIBLE; applyImmersiveMode(immersiveModeEnabled)
                    if (getActivePlaylist().isNotEmpty()) { hideQrOverlay(); startPlayback() } else showQrOverlay()
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SCREEN_STOPPED")
            }

            if (uri == "/api/screen/frame" && method == Method.POST) {
                val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val buffer = ByteArray(contentLength); var totalRead = 0; val input = session.inputStream
                    while (totalRead < contentLength) { val r = input.read(buffer, totalRead, contentLength - totalRead); if (r <= 0) break; totalRead += r }
                    if (totalRead > 0) BitmapFactory.decodeByteArray(buffer, 0, totalRead)?.let { bmp ->
                        runOnUiThread { if (isScreenSharing) { if (qrOverlay.visibility == View.VISIBLE) qrOverlay.visibility = View.GONE; imgLiveStream.visibility = View.VISIBLE; imgLiveStream.setImageBitmap(bmp) } }
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/tag" && method == Method.POST) {
                val newTag = session.parameters["tag"]?.firstOrNull() ?: ""
                if (newTag.isNotEmpty()) { deviceTag = newTag; getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("device_tag", newTag).apply() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/identify" && method == Method.POST) {
                val show = session.parameters["show"]?.firstOrNull() == "true"
                val tag = session.parameters["tag"]?.firstOrNull() ?: deviceTag
                runOnUiThread { if (show) { txtFindMeTag.text = tag; findMeOverlay.visibility = View.VISIBLE; findMeOverlay.bringToFront() } else findMeOverlay.visibility = View.GONE }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/ping" && method == Method.GET) {
                val curVer = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(mapOf(
                    "id" to deviceId, "tag" to deviceTag, "bgObject" to activeBgObject,
                    "serviceActive" to (MenuboardControlService.instance != null).toString(), "version" to curVer,
                    "orientation" to screenOrientationMode, "blurBg" to blurBackgroundEnabled.toString(),
                    "blurDarkness" to blurDarknessPercent.toString(), "blurIntensity" to blurIntensityLevel.toString(),
                    "blurQuality" to blurQualityLevel.toString(), "blurIg" to blurApplyInstagram.toString(),
                    "immersive" to immersiveModeEnabled.toString(), "autoStart" to autoStartOnBoot.toString()
                )))
            }

            if (uri == "/api/playlist" && method == Method.GET) return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(playlist))

            if (uri == "/api/playlist" && method == Method.POST) {
                val files = HashMap<String, String>(); session.parseBody(files)
                val postData = files["postData"] ?: ""
                playlist = gson.fromJson(postData, object : TypeToken<MutableList<MediaItemModel>>() {}.type)
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("playlist_json", postData).apply()
                runOnUiThread { if (getActivePlaylist().isNotEmpty()) { hideQrOverlay(); currentIndex = 0; startPlayback() } else showQrOverlay() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/thumbnail" && method == Method.GET) {
                val filename = session.parameters["filename"]?.firstOrNull() ?: ""
                val file = File(filesDir, filename)
                if (file.exists()) {
                    val isVideo = filename.endsWith(".mp4", ignoreCase = true) || filename.endsWith(".mkv", ignoreCase = true)
                    val bitmap: Bitmap? = if (isVideo) {
                        val retriever = MediaMetadataRetriever()
                        try { retriever.setDataSource(file.absolutePath); retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) } catch (_: Exception) { null }
                    } else {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, bounds)
                        var s = 1; while ((bounds.outWidth / s) > 240 || (bounds.outHeight / s) > 240) s *= 2
                        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
                    }
                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        Bitmap.createScaledBitmap(bitmap, 120, 120, true).compress(Bitmap.CompressFormat.JPEG, 70, stream)
                        val bytes = stream.toByteArray()
                        return newFixedLengthResponse(Response.Status.OK, "image/jpeg", ByteArrayInputStream(bytes), bytes.size.toLong())
                    }
                }
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isBroadcasting = false
        handler.removeCallbacks(ipCheckRunnable); handler.removeCallbacks(webScrollRunnable); handler.removeCallbacks(igSafetyRunnable); handler.removeCallbacks(videoBlurRunnable)
        blurExecutor.shutdownNow(); httpServer?.stop(); exoPlayer?.release()
    }
}
