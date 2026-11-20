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
    val igMediaFilter: String? = "all", // "all", "photo", "video"
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
                    entities.add(
                        Entity(
                            x = Random.nextFloat() * 1080f,
                            y = Random.nextFloat() * 1920f,
                            vx = (Random.nextFloat() - 0.5f) * 1.1f,
                            vy = -(1.2f + Random.nextFloat() * 1.8f),
                            size = 18f + Random.nextFloat() * 14f,
                            phase = Random.nextFloat() * 6.28f,
                            kind = 0,
                            angle = Random.nextFloat() * 360f,
                            spinSpeed = (Random.nextFloat() - 0.5f) * 4.5f,
                            color = if (i % 2 == 0) Color.rgb(78, 52, 46) else Color.rgb(62, 39, 35)
                        )
                    )
                }
                for (i in 0 until 16) {
                    entities.add(
                        Entity(
                            x = Random.nextFloat() * 1080f,
                            y = Random.nextFloat() * 1920f,
                            vx = 0f,
                            vy = -(1.6f + Random.nextFloat() * 2.2f),
                            size = 75f + Random.nextFloat() * 95f,
                            phase = Random.nextFloat() * 6.28f,
                            kind = 1,
                            angle = 0f,
                            spinSpeed = 0f,
                            color = Color.rgb(239, 235, 233)
                        )
                    )
                }
            }
            "doner" -> {
                for (i in 0 until 32) {
                    val k = when {
                        i < 14 -> 0
                        i < 26 -> 1
                        else -> 2
                    }
                    entities.add(
                        Entity(
                            x = Random.nextFloat() * 1080f,
                            y = Random.nextFloat() * 1920f,
                            vx = (Random.nextFloat() - 0.5f) * 1.4f,
                            vy = if (k == 2) (2.8f + Random.nextFloat() * 2f) else -(1.8f + Random.nextFloat() * 3.2f),
                            size = if (k == 0) (65f + Random.nextFloat() * 85f) else (4f + Random.nextFloat() * 5f),
                            phase = Random.nextFloat() * 6.28f,
                            kind = k,
                            angle = 0f,
                            spinSpeed = 0f,
                            color = Color.rgb(255, 110, 0)
                        )
                    )
                }
            }
            "hookah" -> {
                for (i in 0 until 26) {
                    entities.add(
                        Entity(
                            x = Random.nextFloat() * 1080f,
                            y = Random.nextFloat() * 1920f,
                            vx = (Random.nextFloat() - 0.5f) * 0.9f,
                            vy = -(1.3f + Random.nextFloat() * 2.0f),
                            size = if (i % 3 == 0) (28f + Random.nextFloat() * 35f) else (70f + Random.nextFloat() * 100f),
                            phase = Random.nextFloat() * 6.28f,
                            kind = if (i % 3 == 0) 1 else 0,
                            angle = 0f,
                            spinSpeed = 0f,
                            color = Color.rgb(215, 238, 255)
                        )
                    )
                }
            }
            "gold", "snow" -> {
                for (i in 0 until 38) {
                    entities.add(
                        Entity(
                            x = Random.nextFloat() * 1080f,
                            y = Random.nextFloat() * 1920f,
                            vx = (Random.nextFloat() - 0.5f) * 0.8f,
                            vy = if (mode == "snow") (1.8f + Random.nextFloat() * 2.6f) else -(1.0f + Random.nextFloat() * 1.6f),
                            size = 6f + Random.nextFloat() * 18f,
                            phase = Random.nextFloat() * 6.28f,
                            kind = 0,
                            angle = 0f,
                            spinSpeed = 1.5f,
                            color = if (mode == "gold") Color.rgb(255, 215, 0) else Color.WHITE
                        )
                    )
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
            if (e.y < -120f) {
                e.y = h + 80f
                e.x = Random.nextFloat() * w
            }
            if (e.kind == 1) {
                val progress = ((h - e.y) / h).coerceIn(0f, 1f)
                val rad = e.size * (0.85f + progress * 0.9f)
                val alpha = ((1f - progress * 0.6f) * 44f).toInt().coerceIn(8, 48)
                val steamGrad = RadialGradient(swayX, e.y, rad, intArrayOf(Color.argb(alpha, 245, 235, 225), Color.argb(alpha / 2, 230, 215, 200), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                paint.style = Paint.Style.FILL
                paint.shader = steamGrad
                canvas.drawCircle(swayX, e.y, rad, paint)
                paint.shader = null
            } else {
                e.angle = (e.angle + e.spinSpeed) % 360f
                canvas.save()
                canvas.translate(swayX, e.y)
                canvas.rotate(e.angle)
                val bw = e.size * 1.35f
                val bh = e.size * 0.92f
                paint.style = Paint.Style.FILL
                paint.color = e.color
                canvas.drawOval(RectF(-bw, -bh, bw, bh), paint)
                paint.color = Color.argb(70, 161, 136, 127)
                canvas.drawOval(RectF(-bw * 0.7f, -bh * 0.75f, bw * 0.5f, -bh * 0.15f), paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.5f
                paint.color = Color.argb(220, 27, 15, 12)
                val crease = Path().apply {
                    moveTo(-bw * 0.85f, 0f)
                    cubicTo(-bw * 0.3f, -bh * 0.45f, bw * 0.3f, bh * 0.45f, bw * 0.85f, 0f)
                }
                canvas.drawPath(crease, paint)
                canvas.restore()
            }
        }
    }

    private fun drawMarineEcosystem(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.strokeCap = Paint.Cap.ROUND
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
                    val primaryColor = when (e.kind) {
                        0 -> Color.argb(200, 255, 109, 0)
                        1 -> Color.argb(200, 2, 136, 209)
                        else -> Color.argb(200, 255, 196, 0)
                    }
                    val bodyH = if (e.kind == 2) e.size * 0.88f else e.size * 0.54f
                    val wag = sin((tick * 4.8f + e.phase).toDouble()).toFloat() * 13f
                    paint.style = Paint.Style.FILL
                    paint.color = primaryColor
                    val tail = Path().apply {
                        moveTo(e.x - dir * e.size * 0.75f, waveY)
                        lineTo(e.x - dir * e.size * 1.7f, waveY - e.size * 0.72f + wag)
                        lineTo(e.x - dir * e.size * 1.3f, waveY + wag * 0.3f)
                        lineTo(e.x - dir * e.size * 1.7f, waveY + e.size * 0.72f + wag)
                        close()
                    }
                    canvas.drawPath(tail, paint)
                    val dorsal = Path().apply {
                        moveTo(e.x + dir * e.size * 0.1f, waveY - bodyH * 0.85f)
                        quadTo(e.x - dir * e.size * 0.35f, waveY - bodyH * 1.55f, e.x - dir * e.size * 0.65f, waveY - bodyH * 0.75f)
                        close()
                    }
                    canvas.drawPath(dorsal, paint)
                    canvas.drawOval(RectF(e.x - e.size, waveY - bodyH, e.x + e.size, waveY + bodyH), paint)
                    if (e.kind == 0) {
                        paint.color = Color.argb(195, 255, 255, 255)
                        canvas.drawRoundRect(RectF(e.x - e.size * 0.15f, waveY - bodyH * 0.92f, e.x + e.size * 0.12f, waveY + bodyH * 0.92f), 6f, 6f, paint)
                    } else if (e.kind == 1) {
                        paint.color = Color.argb(180, 255, 235, 59)
                        canvas.drawPath(tail, paint)
                    }
                    paint.color = Color.WHITE
                    canvas.drawCircle(e.x + dir * e.size * 0.58f, waveY - bodyH * 0.22f, e.size * 0.16f, paint)
                    paint.color = Color.BLACK
                    canvas.drawCircle(e.x + dir * e.size * 0.63f, waveY - bodyH * 0.22f, e.size * 0.08f, paint)
                }
                3 -> {
                    e.x += e.vx
                    val bobY = e.y + sin((tick * 1.5f + e.phase).toDouble()).toFloat() * 38f
                    if (e.x > w - 85f || e.x < 85f) e.vx = -e.vx
                    paint.style = Paint.Style.FILL
                    paint.color = e.color
                    canvas.drawOval(RectF(e.x - e.size, bobY - e.size * 1.15f, e.x + e.size, bobY + e.size * 0.32f), paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6.5f
                    for (t in -3..4) {
                        val startX = e.x + t * (e.size * 0.22f)
                        val sway = sin((tick * 2.6f + t * 0.7f + e.phase).toDouble()).toFloat() * 24f
                        val tentacle = Path().apply {
                            moveTo(startX, bobY + e.size * 0.2f)
                            quadTo(startX + sway, bobY + e.size * 1.1f, startX - sway * 0.6f, bobY + e.size * 1.95f)
                        }
                        canvas.drawPath(tentacle, paint)
                    }
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
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
                    paint.style = Paint.Style.FILL
                    paint.color = e.color
                    canvas.drawOval(RectF(e.x - e.size, stepY - e.size * 0.55f, e.x + e.size, stepY + e.size * 0.55f), paint)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 4.5f
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
                    if (e.y < -30f) {
                        e.y = h + 20f
                        e.x = Random.nextFloat() * w
                    }
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = Color.argb(130, 200, 245, 255)
                    canvas.drawCircle(bx, e.y, e.size, paint)
                    paint.style = Paint.Style.FILL
                    paint.color = Color.argb(160, 255, 255, 255)
                    canvas.drawCircle(bx - e.size * 0.3f, e.y - e.size * 0.3f, e.size * 0.22f, paint)
                }
            }
        }
    }

    private fun drawRealisticDonerAndFire(canvas: Canvas, w: Float, h: Float) {
        val fireGrad = RadialGradient(w - 150f, h - 80f, 380f, intArrayOf(Color.argb(135, 255, 87, 34), Color.argb(60, 211, 47, 47), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        paint.shader = fireGrad
        paint.style = Paint.Style.FILL
        canvas.drawCircle(w - 150f, h - 80f, 380f, paint)
        paint.shader = null
        val cx = w - 145f
        val cy = h - 250f
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
            if (e.kind == 2 && e.y > h) {
                e.y = cy - 80f
                e.x = cx + (Random.nextFloat() - 0.5f) * 90f
            } else if (e.kind != 2 && e.y < -120f) {
                e.y = h + 60f
                e.x = Random.nextFloat() * w
            }
            when (e.kind) {
                0 -> {
                    val radius = e.size * (1f + ((h - e.y) / h) * 0.65f)
                    val steamGrad = RadialGradient(driftX, e.y, radius, intArrayOf(Color.argb(46, 255, 245, 238), Color.argb(20, 255, 235, 220), Color.TRANSPARENT), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
                    paint.shader = steamGrad
                    canvas.drawCircle(driftX, e.y, radius, paint)
                    paint.shader = null
                }
                1 -> {
                    val flicker = (130 + sin((tick * 8f + e.phase).toDouble()) * 85).toInt().coerceIn(45, 230)
                    paint.color = Color.argb(flicker, 255, 152, 0)
                    canvas.drawCircle(driftX, e.y, e.size, paint)
                }
                2 -> {
                    paint.color = Color.argb(210, 255, 193, 7)
                    canvas.drawOval(RectF(e.x - 3.5f, e.y - 7f, e.x + 3.5f, e.y + 7f), paint)
                }
            }
        }
    }

    private fun drawHookahCloudsAndRings(canvas: Canvas, w: Float, h: Float) {
        for (e in entities) {
            e.y += e.vy
            val waveX = e.x + cos((tick * 0.9f + e.phase).toDouble()).toFloat() * 48f
            if (e.y < -160f) {
                e.y = h + 120f
                e.x = Random.nextFloat() * w
            }
            val progress = ((h - e.y) / h).coerceIn(0f, 1f)
            if (e.kind == 1) {
                val ringRad = e.size * (0.9f + progress * 1.4f)
                val ringAlpha = ((1f - progress * 0.7f) * 85f).toInt().coerceIn(12, 90)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 9f + progress * 8f
                paint.color = Color.argb(ringAlpha, 220, 240, 255)
                canvas.drawOval(RectF(waveX - ringRad, e.y - ringRad * 0.62f, waveX + ringRad, e.y + ringRad * 0.62f), paint)
            } else {
                paint.style = Paint.Style.FILL
                val rad = e.size * (0.8f + progress * 1.15f)
                val peakAlpha = ((1f - progress * 0.65f) * 54f).toInt().coerceIn(8, 60)
                val smokeShader = RadialGradient(waveX, e.y, rad, intArrayOf(Color.argb(peakAlpha, 215, 238, 255), Color.argb(peakAlpha / 2, 200, 230, 255), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                paint.shader = smokeShader
                canvas.drawCircle(waveX, e.y, rad, paint)
                paint.shader = null
            }
        }
    }

    private fun drawBokehOrSnow(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        for (e in entities) {
            e.y += e.vy
            val sx = e.x + sin((tick + e.phase).toDouble()).toFloat() * 20f
            if (mode == "snow" && e.y > h + 30f) {
                e.y = -20f
                e.x = Random.nextFloat() * w
            } else if (mode == "gold" && e.y < -30f) {
                e.y = h + 20f
                e.x = Random.nextFloat() * w
            }
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
                try {
                    val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(accIntent)
                } catch (_: Exception) {}
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
