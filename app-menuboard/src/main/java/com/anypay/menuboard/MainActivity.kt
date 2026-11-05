package com.anypay.menuboard

import android.app.AlertDialog
import android.app.DownloadManager
import android.app.Instrumentation
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
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    val fileName: String,
    val type: String,
    val durationSec: Int = 10,
    val waitAfterSec: Int = 0,
    val animation: String = "fade"
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
        var alpha: Int,
        var color: Int
    )

    private val entities = mutableListOf<Entity>()

    fun setAnimationMode(newMode: String) {
        mode = newMode
        entities.clear()
        when (mode) {
            "fish" -> {
                for (i in 0 until 10) {
                    val dir = if (i % 2 == 0) 1f else -1f
                    entities.add(Entity(Random.nextFloat() * 1080f, 180f + Random.nextFloat() * 1450f, (1.6f + Random.nextFloat() * 2.4f) * dir, 0f, 24f + Random.nextFloat() * 22f, Random.nextFloat() * 6.28f, i % 3, 185, 0))
                }
                for (i in 0 until 2) {
                    entities.add(Entity(200f + i * 650f, 500f + i * 700f, 0.8f * (if (i == 0) 1f else -1f), 0.4f, 44f, i * 2.1f, 3, 180, Color.argb(180, 186, 104, 200)))
                }
                for (i in 0 until 2) {
                    entities.add(Entity(300f + i * 450f, 1830f, if (i == 0) 1.5f else -1.5f, 0f, 34f, i * 1.5f, 4, 210, Color.argb(210, 229, 57, 53)))
                }
                for (i in 0 until 22) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, 0f, -(1.8f + Random.nextFloat() * 2.5f), 4f + Random.nextFloat() * 10f, Random.nextFloat() * 6.28f, 5, 115, Color.WHITE))
                }
            }
            "doner" -> {
                for (i in 0 until 28) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 1.2f, -(1.5f + Random.nextFloat() * 2.8f), 50f + Random.nextFloat() * 80f, Random.nextFloat() * 6.28f, if (i < 16) 0 else 1, if (i < 16) 45 else 190, if (i < 16) Color.rgb(255, 235, 220) else Color.rgb(255, 110, 0)))
                }
            }
            "hookah", "coffee" -> {
                for (i in 0 until 26) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 1.0f, -(1.1f + Random.nextFloat() * 1.8f), 60f + Random.nextFloat() * 95f, Random.nextFloat() * 6.28f, i % 2, 42, if (mode == "hookah") Color.rgb(210, 235, 255) else Color.rgb(245, 225, 205)))
                }
            }
            "gold", "snow" -> {
                for (i in 0 until 35) {
                    entities.add(Entity(Random.nextFloat() * 1080f, Random.nextFloat() * 1920f, (Random.nextFloat() - 0.5f) * 0.8f, if (mode == "snow") (1.5f + Random.nextFloat() * 2.5f) else -(0.8f + Random.nextFloat() * 1.5f), 6f + Random.nextFloat() * 18f, Random.nextFloat() * 6.28f, 0, 145, if (mode == "gold") Color.rgb(255, 215, 0) else Color.WHITE))
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
            "doner" -> drawRealisticDonerAndFire(canvas, w, h)
            "hookah", "coffee" -> drawRealisticVolumetricSmoke(canvas, w, h)
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
            postInvalidateDelayed(22L)
        }
    }

    private fun drawMarineEcosystem(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.color = Color.argb(95, 46, 125, 50)
        for (k in 1..8) {
            val baseX = (w / 9f) * k
            val path = Path().apply {
                moveTo(baseX, h)
                val sway1 = sin((tick * 1.5f + k).toDouble()).toFloat() * 25f
                val sway2 = cos((tick * 1.5f + k).toDouble()).toFloat() * 35f
                quadTo(baseX + sway1, h - 90f, baseX + sway2, h - 180f)
            }
            canvas.drawPath(path, paint)
        }

        for (e in entities) {
            when (e.kind) {
                0, 1, 2 -> {
                    e.x += e.vx
                    val dir = if (e.vx >= 0) 1f else -1f
                    val waveY = e.y + sin((tick * 2f + e.phase).toDouble()).toFloat() * 18f
                    if (dir > 0 && e.x > w + 120f) e.x = -100f
                    if (dir < 0 && e.x < -120f) e.x = w + 100f

                    paint.style = Paint.Style.FILL
                    paint.color = when (e.kind) {
                        0 -> Color.argb(185, 255, 112, 67)
                        1 -> Color.argb(185, 41, 182, 246)
                        else -> Color.argb(185, 255, 213, 79)
                    }
                    val bodyH = if (e.kind == 2) e.size * 0.85f else e.size * 0.52f
                    canvas.drawOval(RectF(e.x - e.size, waveY - bodyH, e.x + e.size, waveY + bodyH), paint)

                    if (e.kind == 0) {
                        paint.color = Color.argb(180, 255, 255, 255)
                        canvas.drawRect(e.x - e.size * 0.2f, waveY - bodyH * 0.9f, e.x + e.size * 0.1f, waveY + bodyH * 0.9f, paint)
                        paint.color = Color.argb(185, 255, 112, 67)
                    }

                    val wag = sin((tick * 4f + e.phase).toDouble()).toFloat() * 12f
                    val tail = Path().apply {
                        moveTo(e.x - dir * e.size * 0.85f, waveY)
                        lineTo(e.x - dir * e.size * 1.65f, waveY - e.size * 0.65f + wag)
                        lineTo(e.x - dir * e.size * 1.4f, waveY)
                        lineTo(e.x - dir * e.size * 1.65f, waveY + e.size * 0.65f + wag)
                        close()
                    }
                    canvas.drawPath(tail, paint)

                    paint.color = Color.WHITE
                    canvas.drawCircle(e.x + dir * e.size * 0.55f, waveY - bodyH * 0.25f, e.size * 0.15f, paint)
                    paint.color = Color.BLACK
                    canvas.drawCircle(e.x + dir * e.size * 0.60f, waveY - bodyH * 0.25f, e.size * 0.07f, paint)
                }
                3 -> {
                    e.x += e.vx
                    val bobY = e.y + sin((tick * 1.4f + e.phase).toDouble()).toFloat() * 35f
                    if (e.x > w - 80f || e.x < 80f) e.vx = -e.vx

                    paint.style = Paint.Style.FILL
                    paint.color = e.color
                    canvas.drawOval(RectF(e.x - e.size, bobY - e.size * 1.1f, e.x + e.size, bobY + e.size * 0.3f), paint)

                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    for (t in -3..4) {
                        val startX = e.x + t * (e.size * 0.22f)
                        val sway = sin((tick * 2.5f + t * 0.7f + e.phase).toDouble()).toFloat() * 22f
                        val tentacle = Path().apply {
                            moveTo(startX, bobY + e.size * 0.2f)
                            quadTo(startX + sway, bobY + e.size * 1.1f, startX - sway * 0.6f, bobY + e.size * 1.9f)
                        }
                        canvas.drawPath(tentacle, paint)
                    }
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    canvas.drawCircle(e.x - e.size * 0.35f, bobY - e.size * 0.2f, 7f, paint)
                    canvas.drawCircle(e.x + e.size * 0.35f, bobY - e.size * 0.2f, 7f, paint)
                    paint.color = Color.BLACK
                    canvas.drawCircle(e.x - e.size * 0.35f, bobY - e.size * 0.2f, 3.5f, paint)
                    canvas.drawCircle(e.x + e.size * 0.35f, bobY - e.size * 0.2f, 3.5f, paint)
                }
                4 -> {
                    e.x += e.vx
                    if (e.x > w - 90f || e.x < 90f) e.vx = -e.vx
                    val stepY = (h - 65f) + kotlin.math.abs(sin((tick * 5f + e.phase).toDouble()).toFloat()) * 8f

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
                    canvas.drawCircle(e.x - e.size * 1.25f, stepY - e.size * 0.85f, 11f, paint)
                    canvas.drawCircle(e.x + e.size * 1.25f, stepY - e.size * 0.85f, 11f, paint)
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
                    paint.color = Color.argb(115, 200, 245, 255)
                    canvas.drawCircle(bx, e.y, e.size, paint)
                }
            }
        }
    }

    private fun drawRealisticDonerAndFire(canvas: Canvas, w: Float, h: Float) {
        val fireGrad = RadialGradient(
            w - 150f, h - 80f, 360f,
            intArrayOf(Color.argb(120, 255, 87, 34), Color.argb(50, 211, 47, 47), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = fireGrad
        paint.style = Paint.Style.FILL
        canvas.drawCircle(w - 150f, h - 80f, 360f, paint)
        paint.shader = null

        val cx = w - 145f
        val cy = h - 250f
        paint.color = Color.argb(180, 190, 195, 200)
        canvas.drawRect(cx - 5f, cy - 150f, cx + 5f, cy + 150f, paint)

        for (layer in -5..5) {
            val yPos = cy + layer * 20f
            val coneRadius = (88f - (layer + 5) * 3.8f)
            val shimmer = sin((tick * 2.5f + layer * 0.8f).toDouble()).toFloat() * 6f
            paint.color = if ((layer + (tick * 2).toInt()) % 2 == 0) Color.argb(175, 150, 55, 20) else Color.argb(175, 195, 82, 32)
            canvas.drawOval(RectF(cx - coneRadius + shimmer, yPos - 13f, cx + coneRadius + shimmer, yPos + 13f), paint)
        }

        for (e in entities) {
            e.y += e.vy
            val driftX = e.x + sin((tick * 1.2f + e.phase).toDouble()).toFloat() * 28f
            if (e.y < -120f) {
                e.y = h + 60f
                e.x = Random.nextFloat() * w
            }
            if (e.kind == 0) {
                val radius = e.size * (1f + ((h - e.y) / h) * 0.6f)
                val steamGrad = RadialGradient(
                    driftX, e.y, radius,
                    intArrayOf(Color.argb(42, 255, 245, 238), Color.argb(18, 255, 235, 220), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = steamGrad
                canvas.drawCircle(driftX, e.y, radius, paint)
                paint.shader = null
            } else {
                val flicker = (120 + sin((tick * 8f + e.phase).toDouble()) * 85).toInt().coerceIn(40, 220)
                paint.color = Color.argb(flicker, 255, 145, 0)
                canvas.drawCircle(driftX, e.y, 4.5f, paint)
            }
        }
    }

    private fun drawRealisticVolumetricSmoke(canvas: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        for (e in entities) {
            e.y += e.vy
            val waveX = e.x + cos((tick * 0.9f + e.phase).toDouble()).toFloat() * 48f
            if (e.y < -160f) {
                e.y = h + 120f
                e.x = Random.nextFloat() * w
            }
            val progress = ((h - e.y) / h).coerceIn(0f, 1f)
            val rad = e.size * (0.8f + progress * 1.1f)
            val peakAlpha = ((1f - progress * 0.65f) * 52f).toInt().coerceIn(8, 60)
            val r = Color.red(e.color)
            val g = Color.green(e.color)
            val b = Color.blue(e.color)

            val smokeShader = RadialGradient(
                waveX, e.y, rad,
                intArrayOf(Color.argb(peakAlpha, r, g, b), Color.argb(peakAlpha / 2, r, g, b), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = smokeShader
            canvas.drawCircle(waveX, e.y, rad, paint)
            paint.shader = null
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
            val pulseAlpha = (80 + sin((tick * 3f + e.phase).toDouble()) * 65).toInt().coerceIn(30, 180)
            paint.color = if (mode == "gold") Color.argb(pulseAlpha, 255, 215, 0) else Color.argb(pulseAlpha, 255, 255, 255)
            canvas.drawCircle(sx, e.y, e.size, paint)
        }
    }
}

class MainActivity : AppCompatActivity() {

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

    private val mediaEndRunnable = Runnable { scheduleNextMedia() }
    private val hideCursorRunnable = Runnable {
        animatedOverlayView.showCursor = false
        animatedOverlayView.invalidate()
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

        val rootFrame = mediaContainer.parent as? FrameLayout ?: mediaContainer
        animatedOverlayView = AnimatedOverlayView(this)
        rootFrame.addView(
            animatedOverlayView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        try {
            val vName = packageManager.getPackageInfo(packageName, 0).versionName
            txtStatus.text = "Sürüm: v$vName (YENİ) | Kumandadan QR Okutun veya Ağı Tarayın"
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

    private fun bringMenuboardToFront() {
        runOnUiThread {
            try {
                val frontIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(applicationContext, 999, frontIntent, flags)
                pendingIntent.send()
                startActivity(frontIntent)
            } catch (_: Exception) {}
        }
        Thread {
            try {
                Runtime.getRuntime().exec(arrayOf("am", "start", "-n", "$packageName/.MainActivity"))
            } catch (_: Exception) {}
        }.start()
    }

    private fun checkForUpdate() {
        if (GITHUB_REPO.startsWith("KULLANICI_ADINIZ")) return
        val url = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Anypay-Menuboard-App")
            .header("Accept", "application/vnd.github+json")
            .build()

        updateHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (!response.isSuccessful) return
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val rawTag = json.optString("tag_name", "v1.00")
                    val digitsOnly = rawTag.replace(Regex("[^0-9]"), "")
                    val remoteVersionCode = digitsOnly.toIntOrNull() ?: 100

                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    }

                    if (remoteVersionCode > currentVersionCode) {
                        val assets = json.optJSONArray("assets") ?: return
                        var downloadUrl = ""
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.getString("name").lowercase()
                            if (name.contains("menuboard") && name.endsWith(".apk")) {
                                downloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                        if (downloadUrl.isNotEmpty()) {
                            runOnUiThread {
                                showUpdateDialog(rawTag, downloadUrl)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        })
    }

    private fun showUpdateDialog(versionTitle: String, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Yeni Güncelleme Bulundu")
            .setMessage("Menuboard için yeni sürüm mevcut ($versionTitle).\nŞimdi indirip yüklemek ister misiniz?")
            .setPositiveButton("Güncelle") { _, _ ->
                downloadAndInstallApk(downloadUrl)
            }
            .setNegativeButton("Daha Sonra", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        Toast.makeText(this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()
        val fileName = "menuboard_update_${System.currentTimeMillis()}.apk"
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Menuboard Güncellemesi")
            .setDescription("Yeni sürüm indiriliyor...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installLocalApkFile(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(installIntent)
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "APK Kurulum Hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            playerView.player = this
            playerView.useController = false
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        scheduleNextMedia()
                    }
                }
            })
        }
    }

    private fun loadSavedState() {
        val prefs = getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
        deviceId = prefs.getString("device_id", "") ?: ""
        if (deviceId.isEmpty()) {
            deviceId = UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_id", deviceId).apply()
        }
        deviceTag = prefs.getString("device_tag", "Menuboard") ?: "Menuboard"
        activeBgObject = prefs.getString("bg_object", "none") ?: "none"
        animatedOverlayView.setAnimationMode(activeBgObject)

        val json = prefs.getString("playlist_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<MediaItemModel>>() {}.type
            playlist = gson.fromJson(json, type)
        }

        if (playlist.isNotEmpty()) {
            hideQrOverlay()
            startPlayback()
        } else {
            showQrOverlay()
        }
    }

    private fun startLocalServer() {
        currentKnownIp = getActiveLocalIpAddress()
        txtIpAddress.text = "IP: $currentKnownIp:8080"
        generateQr(currentKnownIp, deviceId)

        if (httpServer == null) {
            httpServer = SignageServer(8080)
            httpServer?.start(600000, false)
        }
    }

    private fun startUdpBeacon() {
        Thread {
            val socket = DatagramSocket()
            socket.broadcast = true
            while (isBroadcasting) {
                try {
                    val ip = getActiveLocalIpAddress()
                    val payload = "ANYPAY_DISCOVERY:$deviceId:$deviceTag:$ip:8080"
                    val bytes = payload.toByteArray()
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), 8888)
                    socket.send(packet)
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
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun generateQr(ip: String, id: String) {
        val payload = "{\"id\":\"$id\",\"tag\":\"$deviceTag\",\"ip\":\"$ip\",\"port\":8080}"
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 500, 500)
            imgQrCode.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun showQrOverlay() {
        stopAllPlayback()
        imgCornerLogo.visibility = View.GONE
        qrOverlay.visibility = View.VISIBLE
    }

    private fun hideQrOverlay() {
        qrOverlay.visibility = View.GONE
        imgCornerLogo.visibility = View.VISIBLE
    }

    private fun stopAllPlayback() {
        handler.removeCallbacks(mediaEndRunnable)
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        playerView.visibility = View.GONE
        imageView.visibility = View.GONE
        imgLiveStream.visibility = View.GONE
    }

    private fun startPlayback() {
        if (isScreenSharing) return
        if (playlist.isEmpty()) {
            showQrOverlay()
            return
        }
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
        if (playlist.isEmpty()) return

        currentIndex = index
        val item = playlist[index]
        val file = File(filesDir, item.fileName)
        if (!file.exists() || file.length() == 0L) {
            scheduleNextMedia()
            return
        }

        applyTransitionAnimation(item.animation)

        if (item.type == "video") {
            imageView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            exoPlayer?.stop()
            exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exoPlayer?.prepare()
            exoPlayer?.play()

            if (item.durationSec > 0) {
                handler.postDelayed(mediaEndRunnable, item.durationSec * 1000L)
            }
        } else {
            exoPlayer?.stop()
            playerView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setImageURI(null)
            imageView.setImageURI(Uri.fromFile(file))
            val dur = if (item.durationSec > 0) item.durationSec else 10
            handler.postDelayed(mediaEndRunnable, dur * 1000L)
        }
        animatedOverlayView.bringToFront()
    }

    private fun scheduleNextMedia() {
        if (isScreenSharing) return
        stopAllPlayback()
        if (playlist.isEmpty()) return

        val currentItem = playlist[currentIndex % playlist.size]
        val waitTime = currentItem.waitAfterSec * 1000L

        handler.postDelayed({
            currentIndex = (currentIndex + 1) % playlist.size
            playItem(currentIndex)
        }, waitTime)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (qrOverlay.visibility == View.VISIBLE && playlist.isNotEmpty()) {
            hideQrOverlay()
            startPlayback()
        } else {
            super.onBackPressed()
        }
    }

    inner class SignageServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            if (uri == "/api/input" && method == Method.POST) {
                val type = session.parameters["type"]?.firstOrNull() ?: ""
                when (type) {
                    "move" -> {
                        val dx = session.parameters["dx"]?.firstOrNull()?.toFloatOrNull() ?: 0f
                        val dy = session.parameters["dy"]?.firstOrNull()?.toFloatOrNull() ?: 0f
                        runOnUiThread {
                            val dm = resources.displayMetrics
                            animatedOverlayView.cursorX = (animatedOverlayView.cursorX + dx).coerceIn(10f, dm.widthPixels - 20f)
                            animatedOverlayView.cursorY = (animatedOverlayView.cursorY + dy).coerceIn(10f, dm.heightPixels - 20f)
                            animatedOverlayView.showCursor = true
                            animatedOverlayView.bringToFront()
                            animatedOverlayView.invalidate()
                            handler.removeCallbacks(hideCursorRunnable)
                            handler.postDelayed(hideCursorRunnable, 8000L)
                        }
                    }
                    "click" -> {
                        val cx = animatedOverlayView.cursorX
                        val cy = animatedOverlayView.cursorY
                        runOnUiThread {
                            val downTime = SystemClock.uptimeMillis()
                            val evDown = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, cx, cy, 0)
                            val evUp = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, cx, cy, 0)
                            window.decorView.dispatchTouchEvent(evDown)
                            window.decorView.dispatchTouchEvent(evUp)
                            evDown.recycle()
                            evUp.recycle()
                        }
                        Thread {
                            try {
                                Runtime.getRuntime().exec(arrayOf("input", "tap", cx.toInt().toString(), cy.toInt().toString()))
                            } catch (_: Exception) {}
                        }.start()
                    }
                    "scroll" -> {
                        val dy = session.parameters["dy"]?.firstOrNull()?.toIntOrNull() ?: 0
                        val cx = animatedOverlayView.cursorX.toInt()
                        val cy = animatedOverlayView.cursorY.toInt()
                        Thread {
                            try {
                                Runtime.getRuntime().exec(arrayOf("input", "swipe", cx.toString(), cy.toString(), cx.toString(), (cy + dy).toString(), "200"))
                            } catch (_: Exception) {}
                        }.start()
                    }
                    "nav" -> {
                        when (session.parameters["act"]?.firstOrNull() ?: "") {
                            "MENUBOARD" -> bringMenuboardToFront()
                            "APP_DETAILS" -> {
                                runOnUiThread {
                                    try {
                                        val detIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(detIntent)
                                        Toast.makeText(this@MainActivity, "Sağ üstteki 3 noktaya (⋮) basıp 'Kısıtlanmış ayarlara izin ver' seçeneğini açın.", Toast.LENGTH_LONG).show()
                                    } catch (_: Exception) {}
                                }
                            }
                            "OVERLAY_SETTINGS" -> {
                                runOnUiThread {
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val ovIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            startActivity(ovIntent)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                            "ACCESSIBILITY_SETTINGS" -> {
                                runOnUiThread {
                                    try {
                                        val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        startActivity(accIntent)
                                    } catch (_: Exception) {}
                                }
                            }
                            "HOME" -> {
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(homeIntent)
                            }
                            "BACK" -> {
                                Thread {
                                    try { Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK) } catch (_: Exception) {}
                                }.start()
                            }
                        }
                    }
                    "text" -> {
                        val text = session.parameters["val"]?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            Thread {
                                try { Instrumentation().sendStringSync(text) } catch (_: Exception) {}
                            }.start()
                        }
                    }
                    "key" -> {
                        val code = session.parameters["code"]?.firstOrNull() ?: ""
                        Thread {
                            try {
                                if (code == "BACKSPACE") Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DEL)
                                else if (code == "ENTER") Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
                            } catch (_: Exception) {}
                        }.start()
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/sefim/bgobject" && method == Method.POST) {
                val mode = session.parameters["mode"]?.firstOrNull() ?: "none"
                activeBgObject = mode
                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
                    .edit().putString("bg_object", mode).apply()
                runOnUiThread {
                    animatedOverlayView.setAnimationMode(mode)
                    animatedOverlayView.bringToFront()
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/sefim/backup" && method == Method.GET) {
                val backupFile = File(filesDir, "sefim_backup_v117.json")
                val content = if (backupFile.exists()) backupFile.readText() else ""
                return newFixedLengthResponse(Response.Status.OK, "application/json", content)
            }

            if (uri == "/api/sefim/backup" && method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val postData = files["postData"] ?: ""
                if (postData.isNotEmpty()) {
                    File(filesDir, "sefim_backup_v117.json").writeText(postData)
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SAVED")
            }

            if (uri == "/api/upload_raw" && method == Method.POST) {
                val filename = session.parameters["filename"]?.firstOrNull() ?: "media_${System.currentTimeMillis()}"
                val isApk = session.parameters["apk"]?.firstOrNull() == "true"
                val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L

                val targetFile = if (isApk) {
                    File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "remote_pushed_${System.currentTimeMillis()}.apk")
                } else {
                    File(filesDir, filename)
                }

                try {
                    val input = session.inputStream
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(65536)
                        var remaining = contentLength
                        while (remaining > 0) {
                            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                            val read = input.read(buffer, 0, toRead)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            remaining -= read
                        }
                        output.flush()
                    }

                    if (isApk) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "APK alındı, kurulum başlatılıyor...", Toast.LENGTH_LONG).show()
                            installLocalApkFile(targetFile)
                        }
                    }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", filename)
                } catch (e: Exception) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.localizedMessage ?: "ERR")
                }
            }

            if (uri == "/api/app/install" && method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val tempFilePath = files["file"]
                if (tempFilePath != null) {
                    val tempFile = File(tempFilePath)
                    val targetApk = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "remote_pushed_${System.currentTimeMillis()}.apk")
                    FileInputStream(tempFile).use { input ->
                        FileOutputStream(targetApk).use { output ->
                            input.copyTo(output)
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Kumandadan APK alındı, kurulum başlatılıyor...", Toast.LENGTH_LONG).show()
                        installLocalApkFile(targetApk)
                    }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", "INSTALL_STARTED")
                }
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "NO_FILE")
            }

            if (uri == "/api/apps" && method == Method.GET) {
                val pm = packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                val appList = mutableListOf<InstalledAppInfo>()
                for (ri in resolveInfos) {
                    val pkg = ri.activityInfo.packageName
                    val label = ri.loadLabel(pm).toString()
                    if (appList.none { it.packageName == pkg }) {
                        appList.add(InstalledAppInfo(label, pkg))
                    }
                }
                appList.sortBy { it.appName.lowercase() }
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(appList))
            }

            if (uri == "/api/apps/launch" && method == Method.POST) {
                val pkg = session.parameters["pkg"]?.firstOrNull() ?: ""
                if (pkg.isNotEmpty()) {
                    if (pkg == packageName) {
                        bringMenuboardToFront()
                        return newFixedLengthResponse(Response.Status.OK, "text/plain", "LAUNCHED")
                    }
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        runOnUiThread {
                            try { startActivity(launchIntent) } catch (_: Exception) {}
                        }
                    }
                    return newFixedLengthResponse(Response.Status.OK, "text/plain", "LAUNCHED")
                }
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "NOT_FOUND")
            }

            if (uri == "/api/screen/start" && method == Method.POST) {
                isScreenSharing = true
                runOnUiThread {
                    stopAllPlayback()
                    qrOverlay.visibility = View.GONE
                    imgLiveStream.visibility = View.VISIBLE
                    imgLiveStream.bringToFront()
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SCREEN_STARTED")
            }

            if (uri == "/api/screen/stop" && method == Method.POST) {
                isScreenSharing = false
                runOnUiThread {
                    imgLiveStream.visibility = View.GONE
                    if (playlist.isNotEmpty()) {
                        hideQrOverlay()
                        startPlayback()
                    } else {
                        showQrOverlay()
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SCREEN_STOPPED")
            }

            if (uri == "/api/screen/frame" && method == Method.POST) {
                val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val buffer = ByteArray(contentLength)
                    var totalRead = 0
                    val input = session.inputStream
                    while (totalRead < contentLength) {
                        val readCount = input.read(buffer, totalRead, contentLength - totalRead)
                        if (readCount <= 0) break
                        totalRead += readCount
                    }
                    if (totalRead > 0) {
                        val bitmap = BitmapFactory.decodeByteArray(buffer, 0, totalRead)
                        if (bitmap != null) {
                            runOnUiThread {
                                if (isScreenSharing) {
                                    if (qrOverlay.visibility == View.VISIBLE) qrOverlay.visibility = View.GONE
                                    imgLiveStream.visibility = View.VISIBLE
                                    imgLiveStream.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/tag" && method == Method.POST) {
                val newTag = session.parameters["tag"]?.firstOrNull() ?: ""
                if (newTag.isNotEmpty()) {
                    deviceTag = newTag
                    getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
                        .edit().putString("device_tag", newTag).apply()
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/identify" && method == Method.POST) {
                val show = session.parameters["show"]?.firstOrNull() == "true"
                val tag = session.parameters["tag"]?.firstOrNull() ?: deviceTag
                runOnUiThread {
                    if (show) {
                        txtFindMeTag.text = tag
                        findMeOverlay.visibility = View.VISIBLE
                        findMeOverlay.bringToFront()
                    } else {
                        findMeOverlay.visibility = View.GONE
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/ping" && method == Method.GET) {
                val resObj = mapOf("id" to deviceId, "tag" to deviceTag, "bgObject" to activeBgObject)
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(resObj))
            }

            if (uri == "/api/playlist" && method == Method.GET) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(playlist))
            }

            if (uri == "/api/playlist" && method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val postData = files["postData"] ?: ""
                val type = object : TypeToken<MutableList<MediaItemModel>>() {}.type
                playlist = gson.fromJson(postData, type)

                getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("playlist_json", postData)
                    .apply()

                runOnUiThread {
                    hideQrOverlay()
                    currentIndex = 0
                    startPlayback()
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }

            if (uri == "/api/upload" && method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val params = session.parameters
                val filename = params["filename"]?.firstOrNull() ?: "media_${System.currentTimeMillis()}"

                val tempFilePath = files["file"]
                if (tempFilePath != null) {
                    val tempFile = File(tempFilePath)
                    val targetFile = File(filesDir, filename)
                    FileInputStream(tempFile).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
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
                        try {
                            retriever.setDataSource(file.absolutePath)
                            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } catch (_: Exception) { null }
                    } else {
                        BitmapFactory.decodeFile(file.absolutePath)
                    }

                    if (bitmap != null) {
                        val thumb = Bitmap.createScaledBitmap(bitmap, 120, 120, true)
                        val stream = ByteArrayOutputStream()
                        thumb.compress(Bitmap.CompressFormat.JPEG, 70, stream)
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
        httpServer?.stop()
        exoPlayer?.release()
    }
}
