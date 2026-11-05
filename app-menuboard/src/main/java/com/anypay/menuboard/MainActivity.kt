package com.anypay.menuboard

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
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

// Canlı Hareketli Arka Plan / Tema Objeleri Çizici (Balık, Döner, Nargile)
class AnimatedOverlayView(context: Context) : View(context) {
    private var mode: String = "none"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tick = 0f

    private data class Particle(var x: Float, var y: Float, var speed: Float, var size: Float, var phase: Float, var color: Int)
    private val particles = mutableListOf<Particle>()

    fun setAnimationMode(newMode: String) {
        mode = newMode
        particles.clear()
        if (mode == "none") {
            visibility = GONE
            return
        }
        visibility = VISIBLE
        val count = when (mode) {
            "fish" -> 12
            "doner" -> 25
            "hookah" -> 30
            else -> 0
        }
        val colors = listOf(
            Color.argb(140, 255, 152, 0),
            Color.argb(140, 3, 169, 244),
            Color.argb(140, 255, 193, 7),
            Color.argb(140, 0, 230, 118)
        )
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * 1080f,
                    y = Random.nextFloat() * 1920f,
                    speed = 2f + Random.nextFloat() * 4f,
                    size = 18f + Random.nextFloat() * 28f,
                    phase = Random.nextFloat() * 6.28f,
                    color = colors[i % colors.size]
                )
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mode == "none") return

        val w = width.toFloat().coerceAtLeast(1080f)
        val h = height.toFloat().coerceAtLeast(1920f)
        tick += 0.05f

        when (mode) {
            "fish" -> drawSwimmingFish(canvas, w, h)
            "doner" -> drawRotatingDoner(canvas, w, h)
            "hookah" -> drawHookahSmoke(canvas, w, h)
        }

        postInvalidateDelayed(20L)
    }

    private fun drawSwimmingFish(canvas: Canvas, w: Float, h: Float) {
        for (p in particles) {
            p.x += p.speed
            val waveY = p.y + sin((tick + p.phase).toDouble()).toFloat() * 25f
            if (p.x > w + 100f) {
                p.x = -100f
                p.y = Random.nextFloat() * h
            }

            paint.color = p.color
            paint.style = Paint.Style.FILL
            // Balık gövdesi
            val oval = RectF(p.x - p.size, waveY - p.size * 0.5f, p.x + p.size, waveY + p.size * 0.5f)
            canvas.drawOval(oval, paint)

            // Balık kuyruğu
            val tailWag = sin((tick * 3f + p.phase).toDouble()).toFloat() * 10f
            val tailPath = Path().apply {
                moveTo(p.x - p.size * 0.8f, waveY)
                lineTo(p.x - p.size * 1.6f, waveY - p.size * 0.6f + tailWag)
                lineTo(p.x - p.size * 1.6f, waveY + p.size * 0.6f + tailWag)
                close()
            }
            canvas.drawPath(tailPath, paint)

            // Göz ve Su Kabarcığı
            paint.color = Color.argb(180, 255, 255, 255)
            canvas.drawCircle(p.x + p.size * 0.5f, waveY - p.size * 0.1f, p.size * 0.12f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(p.x + p.size * 1.3f, waveY - (tick * 15f) % 80f, 6f, paint)
        }
    }

    private fun drawRotatingDoner(canvas: Canvas, w: Float, h: Float) {
        // Sağ alt ve sol üst köşede dönen döner silüeti ve yükselen sıcak duman/kıvılcım
        val cx = w - 130f
        val cy = h - 220f

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(110, 180, 180, 180)
        canvas.drawRect(cx - 4f, cy - 110f, cx + 4f, cy + 110f, paint)

        for (i in -3..3) {
            val sliceY = cy + i * 22f
            val baseWidth = 65f - kotlin.math.abs(i) * 6f
            val rotScale = 0.85f + 0.15f * sin((tick * 2f + i).toDouble()).toFloat()
            paint.color = if (i % 2 == 0) Color.argb(120, 165, 65, 25) else Color.argb(120, 205, 95, 40)
            canvas.drawOval(RectF(cx - baseWidth * rotScale, sliceY - 14f, cx + baseWidth * rotScale, sliceY + 14f), paint)
        }

        // Yükselen sıcak buhar ve baharat/ateş kıvılcımları
        for (p in particles) {
            p.y -= p.speed
            val swayX = p.x + sin((tick + p.phase).toDouble()).toFloat() * 20f
            if (p.y < -50f) {
                p.y = h + 50f
                p.x = Random.nextFloat() * w
            }
            paint.color = Color.argb(75, 255, 110, 64)
            canvas.drawCircle(swayX, p.y, p.size * 0.45f, paint)
        }
    }

    private fun drawHookahSmoke(canvas: Canvas, w: Float, h: Float) {
        // Nargile dumanı halkaları ve yükselen yumuşak bulutlar
        paint.style = Paint.Style.STROKE
        for (p in particles) {
            p.y -= p.speed * 0.7f
            val driftX = p.x + cos((tick * 0.8f + p.phase).toDouble()).toFloat() * 35f
            if (p.y < -100f) {
                p.y = h + 80f
                p.x = Random.nextFloat() * w
            }
            val expand = p.size + ((h - p.y) / h) * 45f
            paint.strokeWidth = 6f
            paint.color = Color.argb(55, 200, 230, 255)
            canvas.drawCircle(driftX, p.y, expand, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(28, 220, 240, 255)
            canvas.drawCircle(driftX + 12f, p.y + 8f, expand * 0.7f, paint)
            paint.style = Paint.Style.STROKE
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

        // Hareketli Arka Plan Objeleri Katmanını MediaContainer içine ekle
        animatedOverlayView = AnimatedOverlayView(this)
        mediaContainer.addView(
            animatedOverlayView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        try {
            val vName = packageManager.getPackageInfo(packageName, 0).versionName
            txtStatus.text = "Sürüm: v$vName | QR Kodu Okutun veya Ağı Tarayın"
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

        httpServer = SignageServer(8080)
        httpServer?.start()
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

            // Canlı Hareketli Arka Plan Objesi Seçimi (fish, doner, hookah, none)
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

            // Şefim Ürünleri + DB Bilgileri + Görsel Önbelleği Çift Yönlü Yedekleme (Two-Way Sync)
            if (uri == "/api/sefim/backup" && method == Method.GET) {
                val backupFile = File(filesDir, "sefim_backup.json")
                val content = if (backupFile.exists()) backupFile.readText() else ""
                return newFixedLengthResponse(Response.Status.OK, "application/json", content)
            }

            if (uri == "/api/sefim/backup" && method == Method.POST) {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val postData = files["postData"] ?: ""
                if (postData.isNotEmpty()) {
                    File(filesDir, "sefim_backup.json").writeText(postData)
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "SAVED")
            }

            // Uzaktan APK Yükleme ve Kurulum Tetikleme
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

            // Menuboard'da Kurulu Uygulamaların Listesini Getir
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

            // Seçilen Uygulamayı Menuboard Üzerinde Başlat
            if (uri == "/api/apps/launch" && method == Method.POST) {
                val pkg = session.parameters["pkg"]?.firstOrNull() ?: ""
                if (pkg.isNotEmpty()) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runOnUiThread {
                            startActivity(launchIntent)
                        }
                        return newFixedLengthResponse(Response.Status.OK, "text/plain", "LAUNCHED")
                    }
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
