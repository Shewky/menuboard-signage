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
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class MediaItemModel(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val type: String = "image", // "image", "video", "web", "instagram"
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
    val igMediaFilter: String? = "all",
    val igSlideSec: Int = 8
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
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var phase: Float,
        var kind: Int,
        var angle: Float,
        var spinSpeed: Float,
        var color: Int
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
                for (i in 0 until 2) {
                    entities.add(Entity(220f + i * 620f, 480f + i * 680f, 0.9f * (if (i == 0) 1f else -1f), 0.4f, 46f, i * 2.1f, 3, 0f, 0f, Color.argb(190, 171, 71, 188)))
                }
                for (i in 0 until 2) {
                    entities.add(Entity(280f + i * 480f, 1840f, if (i == 0) 1.6f else -1.6f, 0f, 36f, i * 1.5f, 4, 0f, 0f, Color.argb(215, 211, 47, 47)))
                }
                for (i in 0 until 25) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, 0f, -(2.0f + Random.nextFloat() * 2.8f), 4f + Random.nextFloat() * 11f, Random.nextFloat() * 6.28f, 5, 0f, 0f, Color.WHITE))
                }
            }
            "coffee" -> {
                for (i in 0 until 18) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 1.1f, -(1.2f + Random.nextFloat() * 1.8f), 18f + Random.nextFloat() * 14f, Random.nextFloat() * 6.28f, 0, Random.nextFloat() * 360f, (Random.nextFloat() - 0.5f) * 4.5f, if (i % 2 == 0) Color.rgb(78, 52, 46) else Color.rgb(62, 39, 35)))
                }
                for (i in 0 until 16) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, 0f, -(1.6f + Random.nextFloat() * 2.2f), 75f + Random.nextFloat() * 95f, Random.nextFloat() * 6.28f, 1, 0f, 0f, Color.rgb(239, 235, 233)))
                }
            }
            "doner" -> {
                for (i in 0 until 32) {
                    val k = when { i < 14 -> 0; i < 26 -> 1; else -> 2 }
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 1.4f, if (k == 2) (2.8f + Random.nextFloat() * 2f) else -(1.8f + Random.nextFloat() * 3.2f), if (k == 0) (65f + Random.nextFloat() * 85f) else (4f + Random.nextFloat() * 5f), Random.nextFloat() * 6.28f, k, 0f, 0f, Color.rgb(255, 110, 0)))
                }
            }
            "hookah" -> {
                for (i in 0 until 26) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 0.9f, -(1.3f + Random.nextFloat() * 2.0f), if (i % 3 == 0) (28f + Random.nextFloat() * 35f) else (70f + Random.nextFloat() * 100f), Random.nextFloat() * 6.28f, if (i % 3 == 0) 1 else 0, 0f, 0f, Color.rgb(215, 238, 255)))
                }
            }
            "gold", "snow" -> {
                for (i in 0 until 38) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 0.8f, if (mode == "snow") (1.8f + Random.nextFloat() * 2.6f) else -(1.0f + Random.nextFloat() * 1.6f), 6f + Random.nextFloat() * 18f, Random.nextFloat() * 6.28f, 0, 0f, 1.5f, if (mode == "gold") Color.rgb(255, 215, 0) else Color.WHITE))
                }
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

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FF1744")
            canvas.drawPath(cursorPath, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.WHITE
            canvas.drawPath(cursorPath, paint)
        }

        if (mode != "none" || showCursor) {
            postInvalidateDelayed(20L)
        }
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
                val steamGrad = RadialGradient(swayX, e.y, rad, intArrayOf(Color.argb(alpha, 245, 235, 225), Color.argb(alpha / 2, 230, 215, 200), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                paint.style = Paint.Style.FILL; paint.shader = steamGrad
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
                val crease = Path().apply { moveTo(-bw * 0.85f, 0f); cubicTo(-bw * 0.3f, -bh * 0.45f, bw * 0.3f, bh * 0.45f, bw * 0.85f, 0f) }
                canvas.drawPath(crease, paint); canvas.restore()
            }
        }
    }

    private fun drawMarineEcosystem(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 8f; paint.strokeCap = Paint.Cap.ROUND
        for (k in 1..9) {
            val baseX = (w / 10f) * k
            paint.color = if (k % 2 == 0) Color.argb(110, 46, 125, 50) else Color.argb(100, 0, 150, 136)
            val path = Path().apply {
                moveTo(baseX, h)
                val sway1 = sin((tick * 1.6f + k).toDouble()).toFloat() * 28f
                val sway2 = cos((tick * 1.6f + k).toDouble()).toFloat() * 38f
                cubicTo(baseX + sway1, h - 75f, baseX - sway1, h - 145f, baseX + sway2, h - 220f)
            }
            canvas.drawPath(path, paint)
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
                    val dorsal = Path().apply { moveTo(e.x + dir * e.size * 0.1f, waveY - bodyH * 0.85f); quadTo(e.x - dir * e.size * 0.35f, waveY - bodyH * 1.55f, e.x - dir * e.size * 0.65f, waveY - bodyH * 0.75f); close() }
                    canvas.drawPath(dorsal, paint)
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
                        val tentacle = Path().apply { moveTo(startX, bobY + e.size * 0.2f); quadTo(startX + sway, bobY + e.size * 1.1f, startX - sway * 0.6f, bobY + e.size * 1.95f) }
                        canvas.drawPath(tentacle, paint)
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
                    paint.color = Color.WHITE
                    canvas.drawCircle(e.x - 10f, stepY - e.size * 0.7f, 5.5f, paint)
                    canvas.drawCircle(e.x + 10f, stepY - e.size * 0.7f, 5.5f, paint)
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
        val fireGrad = RadialGradient(w - 150f, h - 80f, 380f, intArrayOf(Color.argb(135, 255, 87, 34), Color.argb(60, 211, 47, 47), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        paint.shader = fireGrad; paint.style = Paint.Style.FILL
        canvas.drawCircle(w - 150f, h - 80f, 380f, paint); paint.shader = null
        val cx = w - 145f; val cy = h - 250f
        paint.color = Color.argb(190, 190, 195, 200)
        canvas.drawRect(cx - 5f, cy - 155f, cx + 5f, cy + 155f, paint)
        for (layer in -5..5) {
            val yPos = cy + layer * 20f
            val coneRadius = (90f - (layer + 5) * 3.8f)
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
                    val steamGrad = RadialGradient(driftX, e.y, radius, intArrayOf(Color.argb(46, 255, 245, 238), Color.argb(20, 255, 235, 220), Color.TRANSPARENT), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
                    paint.shader = steamGrad; canvas.drawCircle(driftX, e.y, radius, paint); paint.shader = null
                }
                1 -> {
                    val flicker = (130 + sin((tick * 8f + e.phase).toDouble()) * 85).toInt().coerceIn(45, 230)
                    paint.color = Color.argb(flicker, 255, 152, 0); canvas.drawCircle(driftX, e.y, e.size, paint)
                }
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
                val ringAlpha = ((1f - progress * 0.7f) * 85f).toInt().coerceIn(12, 90)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 9f + progress * 8f; paint.color = Color.argb(ringAlpha, 220, 240, 255)
                canvas.drawOval(RectF(waveX - ringRad, e.y - ringRad * 0.62f, waveX + ringRad, e.y + ringRad * 0.62f), paint)
            } else {
                paint.style = Paint.Style.FILL
                val rad = e.size * (0.8f + progress * 1.15f)
                val peakAlpha = ((1f - progress * 0.65f) * 54f).toInt().coerceIn(8, 60)
                val smokeShader = RadialGradient(waveX, e.y, rad, intArrayOf(Color.argb(peakAlpha, 215, 238, 255), Color.argb(peakAlpha / 2, 200, 230, 255), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                paint.shader = smokeShader; canvas.drawCircle(waveX, e.y, rad, paint); paint.shader = null
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

    private lateinit var webView: WebView
    private lateinit var webQrContainer: FrameLayout
    private lateinit var imgWebQr: ImageView
    private var activeWebItem: MediaItemModel? = null
    private var webScrollActive = false

    private var exoPlayer: ExoPlayer? = null
    private var httpServer: SignageServer? = null
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val updateHttpClient = OkHttpClient()

    private var playlist = mutableListOf<MediaItemModel>()
    private var currentIndex = 0
    private var deviceId = ""
    private var deviceTag = "Menuboard"
    private var isScreenSharing = false
    private var isBroadcasting = true
    private var currentKnownIp = ""
    private var activeBgObject = "none"

    private var awaitingSequentialPermissionChain = false

    private val mediaEndRunnable = Runnable { scheduleNextMedia() }
    private val hideCursorRunnable = Runnable {
        animatedOverlayView.showCursor = false
        animatedOverlayView.invalidate()
    }

    private val webScrollRunnable = object : Runnable {
        override fun run() {
            if (!webScrollActive || webView.visibility != View.VISIBLE) return
            val item = activeWebItem ?: return

            val (stepPx, delayMs) = when (item.webScrollSpeed) {
                "slow" -> Pair(1, 32L)
                "fast" -> Pair(6, 22L)
                else -> Pair(3, 28L)
            }

            if (webView.canScrollVertically(1)) {
                webView.scrollBy(0, stepPx)
                handler.postDelayed(this, delayMs)
            } else {
                if (item.scrollUntilEnd) {
                    webScrollActive = false
                    handler.removeCallbacks(mediaEndRunnable)
                    handler.postDelayed(mediaEndRunnable, 2500L)
                }
            }
        }
    }

    private val ipCheckRunnable = object : Runnable {
        override fun run() {
            val freshIp = getActiveLocalIpAddress()
            if (freshIp != currentKnownIp && freshIp != "127.0.0.1") {
                currentKnownIp = freshIp
                txtIpAddress.text = "IP: $freshIp:8080"
                generateQr(freshIp, deviceId)
            }
            handler.postDelayed(this, 5000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        setupWebPlayerAndQrOverlay()

        val rootFrame = mediaContainer.parent as? FrameLayout ?: mediaContainer
        animatedOverlayView = AnimatedOverlayView(this)
        rootFrame.addView(
            animatedOverlayView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        try {
            val vName = packageManager.getPackageInfo(packageName, 0).versionName
            txtStatus.text = "Sürüm: v$vName | Kumandadan QR Okutun veya Ağı Tarayın"
        } catch (_: Exception) {}

        initPlayer()
        loadSavedState()

        btnSettings.setOnClickListener { showQrOverlay() }
        btnCloseQr.setOnClickListener {
            if (playlist.isNotEmpty()) {
                hideQrOverlay()
                startPlayback()
            }
        }

        startLocalServer()
        startUdpBeacon()
        handler.post(ipCheckRunnable)
        checkForUpdate()
    }

    private fun setupWebPlayerAndQrOverlay() {
        webView = WebView(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            addJavascriptInterface(object {
                @JavascriptInterface
                fun onInstagramFinished() {
                    runOnUiThread {
                        if (activeWebItem?.type == "instagram" && activeWebItem?.scrollUntilEnd == true) {
                            handler.removeCallbacks(mediaEndRunnable)
                            scheduleNextMedia()
                        }
                    }
                }
            }, "AnypayIg")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val item = activeWebItem ?: return
                    if (view?.visibility == View.VISIBLE) {
                        if (item.type == "instagram") {
                            injectInstagramSlideshowJs(view, item)
                        } else {
                            injectWebCustomStyles(view, item)
                            if (item.autoScroll || item.scrollUntilEnd) {
                                handler.removeCallbacks(webScrollRunnable)
                                webScrollActive = true
                                handler.postDelayed(webScrollRunnable, 2000L)
                            }
                        }
                    }
                }
            }
        }
        mediaContainer.addView(
            webView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        webQrContainer = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            elevation = 16f
        }
        imgWebQr = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        webQrContainer.addView(
            imgWebQr,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        mediaContainer.addView(webQrContainer)
    }

    private fun injectInstagramSlideshowJs(view: WebView, item: MediaItemModel) {
        val filter = item.igMediaFilter ?: "all"
        val slideMs = (if (item.igSlideSec > 0) item.igSlideSec else 8) * 1000
        val stopAtEnd = item.scrollUntilEnd

        val js = """
            (function() {
                if (window._anypayIgRunning) return;
                window._anypayIgRunning = true;
                var filter = '$filter';
                var slideMs = $slideMs;
                var stopAtEnd = $stopAtEnd;

                setTimeout(function() {
                    var items = [];
                    var imgs = document.querySelectorAll('article img, main img');
                    var vids = document.querySelectorAll('article video, main video');

                    if (filter === 'all' || filter === 'photo') {
                        imgs.forEach(function(img) {
                            if (img.naturalWidth > 200 && img.src) {
                                items.push({type: 'photo', src: img.src});
                            }
                        });
                    }
                    if (filter === 'all' || filter === 'video') {
                        vids.forEach(function(v) {
                            if (v.src) items.push({type: 'video', src: v.src});
                        });
                    }

                    if (items.length === 0) {
                        var sc = setInterval(function() {
                            window.scrollBy(0, 4);
                            if ((window.innerHeight + window.scrollY) >= document.body.scrollHeight - 20) {
                                clearInterval(sc);
                                if (stopAtEnd && window.AnypayIg) window.AnypayIg.onInstagramFinished();
                            }
                        }, 30);
                        return;
                    }

                    var overlay = document.createElement('div');
                    overlay.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:#000;z-index:999999;display:flex;align-items:center;justify-content:center;';
                    document.body.appendChild(overlay);

                    var idx = 0;
                    function showNext() {
                        if (idx >= items.length) {
                            if (stopAtEnd) {
                                if (window.AnypayIg) window.AnypayIg.onInstagramFinished();
                                return;
                            }
                            idx = 0;
                        }
                        var cur = items[idx++];
                        overlay.innerHTML = '';
                        if (cur.type === 'video') {
                            var v = document.createElement('video');
                            v.src = cur.src;
                            v.autoplay = true;
                            v.muted = true;
                            v.playsInline = true;
                            v.style.cssText = 'max-width:100%;max-height:100%;object-fit:contain;';
                            v.onended = showNext;
                            v.onerror = showNext;
                            overlay.appendChild(v);
                        } else {
                            var im = document.createElement('img');
                            im.src = cur.src;
                            im.style.cssText = 'max-width:100%;max-height:100%;object-fit:contain;';
                            overlay.appendChild(im);
                            setTimeout(showNext, slideMs);
                        }
                    }
                    showNext();
                }, 2500);
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    private fun injectWebCustomStyles(view: WebView, item: MediaItemModel) {
        val cssRules = StringBuilder()
        val fontFamily = item.webFontFamily ?: "default"
        if (fontFamily != "default" && fontFamily.isNotEmpty()) {
            cssRules.append("body, p, div, span, li, td, th, a, h1, h2, h3, h4, h5, h6 { font-family: $fontFamily !important; } ")
        }
        if (item.webBoldHeaders) {
            cssRules.append("h1, h2, h3, h4, h5, h6, strong, b, .title, .header, [class*='title'], [class*='head'] { font-weight: 900 !important; } ")
        }
        if (cssRules.isNotEmpty()) {
            val safeCss = cssRules.toString().replace("'", "\\'")
            val js = "(function(){ var s = document.getElementById('anypay-custom-style'); if(!s){ s = document.createElement('style'); s.id='anypay-custom-style'; document.head.appendChild(s); } s.innerHTML = '$safeCss'; })();"
            view.evaluateJavascript(js, null)
        }
    }

    override fun onResume() {
        super.onResume()
        currentTaskId = taskId
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
        val expectedComponentName = "$packageName/${MenuboardControlService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServices.contains(expectedComponentName, ignoreCase = true) ||
               enabledServices.contains("$packageName/.MenuboardControlService", ignoreCase = true)
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
                if (am != null) {
                    am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        for (appTask in am.appTasks) appTask.moveToFront()
                    }
                }
                var tempBridgeView: View? = null
                val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this) && wm != null) {
                    try {
                        tempBridgeView = View(applicationContext)
                        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
                        val lp = WindowManager.LayoutParams(1, 1, overlayType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT)
                        wm.addView(tempBridgeView, lp)
                    } catch (_: Exception) {}
                }
                val frontIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                val optionsBundle: Bundle? = if (Build.VERSION.SDK_INT >= 34) {
                    try {
                        val opts = ActivityOptions.makeBasic()
                        ActivityOptions::class.java.getMethod("setPendingIntentBackgroundActivityStartMode", Int::class.javaPrimitiveType).invoke(opts, 1)
                        opts.toBundle()
                    } catch (_: Exception) { null }
                } else null
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                val pi = PendingIntent.getActivity(applicationContext, 999, frontIntent, flags)
                try {
                    if (optionsBundle != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) pi.send(applicationContext, 0, null, null, null, null, optionsBundle)
                    else pi.send()
                } catch (_: Exception) {}
                startActivity(frontIntent)
                try {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val chId = "menuboard_front_ch"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) nm.createNotificationChannel(NotificationChannel(chId, "Menuboard Öne Getirme", NotificationManager.IMPORTANCE_HIGH))
                    val notif = NotificationCompat.Builder(applicationContext, chId)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle("Anypay Menuboard")
                        .setContentText("Menuboard ekranı öne getiriliyor...")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pi, true)
                        .setAutoCancel(true)
                        .build()
                    nm.notify(777, notif)
                    handler.postDelayed({ nm.cancel(777) }, 1200L)
                } catch (_: Exception) {}
                if (tempBridgeView != null && wm != null) {
                    val viewToRemove = tempBridgeView
                    handler.postDelayed({ try { wm.removeView(viewToRemove) } catch (_: Exception) {} }, 500L)
                }
            } catch (_: Exception) {}
        }
    }

    private fun checkForUpdate() {
        if (GITHUB_REPO.startsWith("KULLANICI_ADINIZ")) return
        val request = Request.Builder().url("https://api.github.com/repos/$GITHUB_REPO/releases/latest").header("User-Agent", "Anypay-Menuboard-App").header("Accept", "application/vnd.github+json").build()
        updateHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (!response.isSuccessful) return
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val rawTag = json.optString("tag_name", "v1.00")
                    val remoteVersionCode = rawTag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 100
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt() else @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0).versionCode
                    if (remoteVersionCode > currentVersionCode) {
                        val assets = json.optJSONArray("assets") ?: return
                        var downloadUrl = ""
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name").lowercase()
                            if (name.contains("menuboard") && name.endsWith(".apk")) { downloadUrl = asset.getString("browser_download_url"); break }
                        }
                        if (downloadUrl.isNotEmpty()) runOnUiThread { showUpdateDialog(rawTag, downloadUrl) }
                    }
                } catch (_: Exception) {}
            }
        })
    }

    private fun showUpdateDialog(versionTitle: String, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Yeni Güncelleme Bulundu")
            .setMessage("Menuboard için yeni sürüm mevcut ($versionTitle).\nŞimdi indirip yüklemek ister misiniz?")
            .setPositiveButton("Güncelle") { _, _ -> downloadAndInstallApk(downloadUrl) }
            .setNegativeButton("Daha Sonra", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        Toast.makeText(this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "menuboard_update_${System.currentTimeMillis()}.apk")
        if (destination.exists()) destination.delete()
        val request = DownloadManager.Request(Uri.parse(url)).setTitle("Menuboard Güncellemesi").setDescription("Yeni sürüm indiriliyor...").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationUri(Uri.fromFile(destination))
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    unregisterReceiver(this)
                    installLocalApkFile(destination)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        else registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installLocalApkFile(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", apkFile)
            startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(apkUri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "APK Kurulum Hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            playerView.player = this
            playerView.useController = false
            addListener(object : Player.Listener {
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
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<MediaItemModel>>() {}.type
            playlist = gson.fromJson(json, type)
        }
        if (playlist.isNotEmpty()) { hideQrOverlay(); startPlayback() } else showQrOverlay()
    }

    private fun startLocalServer() {
        currentKnownIp = getActiveLocalIpAddress()
        txtIpAddress.text = "IP: $currentKnownIp:8080"
        generateQr(currentKnownIp, deviceId)
        if (httpServer == null) { httpServer = SignageServer(8080); httpServer?.start(600000, false) }
    }

    private fun startUdpBeacon() {
        Thread {
            val socket = DatagramSocket(); socket.broadcast = true
            while (isBroadcasting) {
                try {
                    val ip = getActiveLocalIpAddress()
                    val bytes = "ANYPAY_DISCOVERY:$deviceId:$deviceTag:$ip:8080".toByteArray()
                    socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), 8888))
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

    private fun showQrOverlay() { stopAllPlayback(); imgCornerLogo.visibility = View.GONE; qrOverlay.visibility = View.VISIBLE }
    private fun hideQrOverlay() { qrOverlay.visibility = View.GONE; imgCornerLogo.visibility = View.VISIBLE }

    private fun stopAllPlayback() {
        handler.removeCallbacks(mediaEndRunnable)
        handler.removeCallbacks(webScrollRunnable)
        webScrollActive = false
        activeWebItem = null
        exoPlayer?.stop(); exoPlayer?.clearMediaItems()
        playerView.visibility = View.GONE
        imageView.visibility = View.GONE
        imgLiveStream.visibility = View.GONE
        webView.stopLoading(); webView.visibility = View.GONE
        webQrContainer.visibility = View.GONE
    }

    private fun startPlayback() {
        if (isScreenSharing) return
        if (playlist.isEmpty()) { showQrOverlay(); return }
        playItem(currentIndex % playlist.size)
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
        handler.removeCallbacks(mediaEndRunnable)
        handler.removeCallbacks(webScrollRunnable)
        webScrollActive = false
        activeWebItem = null

        if (playlist.isEmpty()) return
        currentIndex = index
        val item = playlist[index]

        if (item.type == "instagram") {
            exoPlayer?.stop()
            playerView.visibility = View.GONE
            imageView.visibility = View.GONE
            webQrContainer.visibility = View.GONE
            webView.visibility = View.VISIBLE

            activeWebItem = item
            val prefs = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
            val savedCookie = prefs.getString("ig_cookie", "") ?: ""
            val savedUa = prefs.getString("ig_ua", "") ?: ""

            if (savedUa.isNotEmpty()) webView.settings.userAgentString = savedUa
            if (savedCookie.isNotEmpty()) {
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(webView, true)
                savedCookie.split(";").forEach { part ->
                    val trimmed = part.trim()
                    if (trimmed.isNotEmpty()) cm.setCookie("https://www.instagram.com", trimmed)
                }
                cm.flush()
            }

            val cleanUser = item.fileName.trim()
                .removePrefix("https://www.instagram.com/")
                .removePrefix("https://instagram.com/")
                .removePrefix("http://www.instagram.com/")
                .removePrefix("@")
                .trim('/')

            webView.setInitialScale(100)
            webView.settings.textZoom = 100
            webView.scrollTo(0, 0)
            webView.loadUrl("https://www.instagram.com/$cleanUser/")
            applyTransitionAnimation(item.animation)

            if (item.scrollUntilEnd) handler.postDelayed(mediaEndRunnable, 300000L)
            else handler.postDelayed(mediaEndRunnable, (if (item.durationSec > 0) item.durationSec else 30) * 1000L)

            animatedOverlayView.bringToFront()
            return
        }

        if (item.type == "web") {
            exoPlayer?.stop()
            playerView.visibility = View.GONE
            imageView.visibility = View.GONE
            webView.visibility = View.VISIBLE

            activeWebItem = item
            val targetUrl = if (item.fileName.startsWith("http://") || item.fileName.startsWith("https://")) item.fileName else "https://${item.fileName}"

            val zoom = if (item.webZoomPercent in 25..300) item.webZoomPercent else 100
            webView.setInitialScale(zoom)
            webView.settings.textZoom = if (item.webFontSizePercent in 50..250) item.webFontSizePercent else 100
            webView.scrollTo(0, 0)
            webView.loadUrl(targetUrl)
            applyTransitionAnimation(item.animation)

            if (item.showQr) {
                try {
                    val density = resources.displayMetrics.density
                    val qrSizePx = ((if (item.qrSizeDp in 60..400) item.qrSizeDp else 150) * density).toInt()
                    val borderPx = ((if (item.qrBorderDp in 0..40) item.qrBorderDp else 8) * density).toInt()
                    val marginPx = (20 * density).toInt()

                    imgWebQr.setImageBitmap(BarcodeEncoder().encodeBitmap(targetUrl, BarcodeFormat.QR_CODE, qrSizePx, qrSizePx))
                    val gravityVal = when (item.qrCorner) {
                        "top_left" -> Gravity.TOP or Gravity.START
                        "top_right" -> Gravity.TOP or Gravity.END
                        "bottom_left" -> Gravity.BOTTOM or Gravity.START
                        else -> Gravity.BOTTOM or Gravity.END
                    }

                    val totalBoxPx = qrSizePx + (borderPx * 2)
                    webQrContainer.setPadding(borderPx, borderPx, borderPx, borderPx)
                    webQrContainer.setBackgroundColor(if (borderPx > 0) Color.WHITE else Color.TRANSPARENT)
                    webQrContainer.layoutParams = FrameLayout.LayoutParams(totalBoxPx, totalBoxPx).apply {
                        gravity = gravityVal
                        setMargins(marginPx, marginPx, marginPx, marginPx)
                    }
                    webQrContainer.visibility = View.VISIBLE
                    webQrContainer.bringToFront()
                } catch (_: Exception) { webQrContainer.visibility = View.GONE }
            } else {
                webQrContainer.visibility = View.GONE
            }

            if (item.scrollUntilEnd) handler.postDelayed(mediaEndRunnable, 300000L)
            else handler.postDelayed(mediaEndRunnable, (if (item.durationSec > 0) item.durationSec else 20) * 1000L)

            animatedOverlayView.bringToFront()
            return
        }

        webView.stopLoading(); webView.visibility = View.GONE; webQrContainer.visibility = View.GONE
        val file = File(filesDir, item.fileName)
        if (!file.exists() || file.length() == 0L) { scheduleNextMedia(); return }

        applyTransitionAnimation(item.animation)

        if (item.type == "video") {
            imageView.visibility = View.GONE; playerView.visibility = View.VISIBLE
            exoPlayer?.stop(); exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file))); exoPlayer?.prepare(); exoPlayer?.play()
            if (item.durationSec > 0) handler.postDelayed(mediaEndRunnable, item.durationSec * 1000L)
        } else {
            exoPlayer?.stop(); playerView.visibility = View.GONE; imageView.visibility = View.VISIBLE
            imageView.setImageURI(null); imageView.setImageURI(Uri.fromFile(file))
            handler.postDelayed(mediaEndRunnable, (if (item.durationSec > 0) item.durationSec else 10) * 1000L)
        }
        animatedOverlayView.bringToFront()
    }

    private fun scheduleNextMedia() {
        if (isScreenSharing) return
        stopAllPlayback()
        if (playlist.isEmpty()) return
        val waitTime = playlist[currentIndex % playlist.size].waitAfterSec * 1000L
        handler.postDelayed({ currentIndex = (currentIndex + 1) % playlist.size; playItem(currentIndex) }, waitTime)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (qrOverlay.visibility == View.VISIBLE && playlist.isNotEmpty()) { hideQrOverlay(); startPlayback() }
        else super.onBackPressed()
    }

    inner class SignageServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            if (uri == "/api/instagram/status" && method == Method.GET) {
                val cookie = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).getString("ig_cookie", "") ?: ""
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(mapOf("active" to cookie.contains("sessionid="))))
            }

            if (uri == "/api/instagram/cookie" && method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                try {
                    val json = JSONObject(files["postData"] ?: "{}")
                    val cookie = json.optString("cookie", "")
                    val ua = json.optString("userAgent", "")
                    getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("ig_cookie", cookie).putString("ig_ua", ua).apply()
                    runOnUiThread {
                        val cm = CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cookie.split(";").forEach { part ->
                            val t = part.trim()
                            if (t.isNotEmpty()) cm.setCookie("https://www.instagram.com", t)
                        }
                        cm.flush()
                    }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
                } catch (_: Exception) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "ERR")
                }
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
                            val dm = resources.displayMetrics
                            animatedOverlayView.cursorX = (animatedOverlayView.cursorX + dx).coerceIn(10f, dm.widthPixels - 20f)
                            animatedOverlayView.cursorY = (animatedOverlayView.cursorY + dy).coerceIn(10f, dm.heightPixels - 20f)
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
                val files = HashMap<String, String>()
                session.parseBody(files)
                val postData = files["postData"] ?: ""
                if (postData.isNotEmpty()) File(filesDir, "sefim_backup_v118.json").writeText(postData)
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SAVED")
            }

            if (uri == "/api/upload_raw" && method == Method.POST) {
                val filename = session.parameters["filename"]?.firstOrNull() ?: "media_${System.currentTimeMillis()}"
                val isApk = session.parameters["apk"]?.firstOrNull() == "true"
                val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
                val targetFile = if (isApk) File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "remote_pushed_${System.currentTimeMillis()}.apk") else File(filesDir, filename)
                try {
                    val input = session.inputStream
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(65536)
                        var remaining = contentLength
                        while (remaining > 0) {
                            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                        output.flush()
                    }
                    if (isApk) runOnUiThread { Toast.makeText(this@MainActivity, "APK alındı, kurulum başlatılıyor...", Toast.LENGTH_LONG).show(); installLocalApkFile(targetFile) }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", filename)
                } catch (e: Exception) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.localizedMessage ?: "ERR")
                }
            }

            if (uri == "/api/app/install" && method == Method.POST) {
                val files = HashMap<String, String>(); session.parseBody(files)
                val tempFilePath = files["file"]
                if (tempFilePath != null) {
                    val targetApk = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "remote_pushed_${System.currentTimeMillis()}.apk")
                    FileInputStream(File(tempFilePath)).use { input -> FileOutputStream(targetApk).use { output -> input.copyTo(output) } }
                    runOnUiThread { Toast.makeText(this@MainActivity, "Kumandadan APK alındı, kurulum başlatılıyor...", Toast.LENGTH_LONG).show(); installLocalApkFile(targetApk) }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", "INSTALL_STARTED")
                }
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "NO_FILE")
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
                runOnUiThread { stopAllPlayback(); qrOverlay.visibility = View.GONE; imgLiveStream.visibility = View.VISIBLE; imgLiveStream.bringToFront() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SCREEN_STARTED")
            }

            if (uri == "/api/screen/stop" && method == Method.POST) {
                isScreenSharing = false
                runOnUiThread { imgLiveStream.visibility = View.GONE; if (playlist.isNotEmpty()) { hideQrOverlay(); startPlayback() } else showQrOverlay() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SCREEN_STOPPED")
            }

            if (uri == "/api/screen/frame" && method == Method.POST) {
                val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val buffer = ByteArray(contentLength); var totalRead = 0; val input = session.inputStream
                    while (totalRead < contentLength) { val r = input.read(buffer, totalRead, contentLength - totalRead); if (r <= 0) break; totalRead += r }
                    if (totalRead > 0) {
                        val bitmap = BitmapFactory.decodeByteArray(buffer, 0, totalRead)
                        if (bitmap != null) runOnUiThread { if (isScreenSharing) { if (qrOverlay.visibility == View.VISIBLE) qrOverlay.visibility = View.GONE; imgLiveStream.visibility = View.VISIBLE; imgLiveStream.setImageBitmap(bitmap) } }
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
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(mapOf("id" to deviceId, "tag" to deviceTag, "bgObject" to activeBgObject, "serviceActive" to (MenuboardControlService.instance != null).toString())))
            }

            if (uri == "/api/playlist" && method == Method.GET) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(playlist))
            }

            if (uri == "/api/playlist" && method == Method.POST) {
                val files = HashMap<String, String>(); session.parseBody(files)
                val postData = files["postData"] ?: ""
                playlist = gson.fromJson(postData, object : TypeToken<MutableList<MediaItemModel>>() {}.type)
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE).edit().putString("playlist_json", postData).apply()
                runOnUiThread { hideQrOverlay(); currentIndex = 0; startPlayback() }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/upload" && method == Method.POST) {
                val files = HashMap<String, String>(); session.parseBody(files)
                val filename = session.parameters["filename"]?.firstOrNull() ?: "media_${System.currentTimeMillis()}"
                val tempFilePath = files["file"]
                if (tempFilePath != null) {
                    FileInputStream(File(tempFilePath)).use { input -> FileOutputStream(File(filesDir, filename)).use { output -> input.copyTo(output) } }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", filename)
                }
            }

            if (uri == "/api/thumbnail" && method == Method.GET) {
                val filename = session.parameters["filename"]?.firstOrNull() ?: ""
                val file = File(filesDir, filename)
                if (file.exists()) {
                    val isVideo = filename.endsWith(".mp4", ignoreCase = true) || filename.endsWith(".mkv", ignoreCase = true)
                    val bitmap: Bitmap? = if (isVideo) {
                        val retriever = MediaMetadataRetriever()
                        try { retriever.setDataSource(file.absolutePath); retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) } catch (_: Exception) { null }
                    } else BitmapFactory.decodeFile(file.absolutePath)
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
        handler.removeCallbacks(ipCheckRunnable)
        handler.removeCallbacks(webScrollRunnable)
        httpServer?.stop()
        exoPlayer?.release()
    }
}
