package com.anypay.menuboard

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.google.zxing.BarcodeFormat
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

data class MediaItemModel(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val type: String,
    val durationSec: Int = 10,
    val waitAfterSec: Int = 0
)

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var imageView: ImageView
    private lateinit var qrOverlay: View
    private lateinit var imgQrCode: ImageView
    private lateinit var txtIpAddress: TextView
    private lateinit var btnSettings: ImageView
    private lateinit var btnCloseQr: Button

    private var exoPlayer: ExoPlayer? = null
    private var httpServer: SignageServer? = null
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())

    private var playlist = mutableListOf<MediaItemModel>()
    private var currentIndex = 0
    private var currentPairToken = ""

    private val mediaEndRunnable = Runnable { scheduleNextMedia() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        imageView = findViewById(R.id.imageView)
        qrOverlay = findViewById(R.id.qrOverlay)
        imgQrCode = findViewById(R.id.imgQrCode)
        txtIpAddress = findViewById(R.id.txtIpAddress)
        btnSettings = findViewById(R.id.btnSettings)
        btnCloseQr = findViewById(R.id.btnCloseQr)

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
        currentPairToken = prefs.getString("pair_token", "") ?: ""
        if (currentPairToken.isEmpty()) {
            currentPairToken = UUID.randomUUID().toString().take(8)
            prefs.edit().putString("pair_token", currentPairToken).apply()
        }

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
        val ip = getLocalIpAddress()
        txtIpAddress.text = "IP: $ip:8080"
        generateQr(ip, currentPairToken)

        httpServer = SignageServer(8080)
        httpServer?.start()
    }

    private fun generateQr(ip: String, token: String) {
        val payload = "{\"ip\":\"$ip\",\"port\":8080,\"token\":\"$token\"}"
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 500, 500)
            imgQrCode.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
    }

    private fun showQrOverlay() {
        stopAllPlayback()
        qrOverlay.visibility = View.VISIBLE
    }

    private fun hideQrOverlay() {
        qrOverlay.visibility = View.GONE
    }

    private fun stopAllPlayback() {
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        playerView.visibility = View.GONE
        imageView.visibility = View.GONE
    }

    private fun startPlayback() {
        if (playlist.isEmpty()) {
            showQrOverlay()
            return
        }
        playItem(currentIndex % playlist.size)
    }

    private fun playItem(index: Int) {
        stopAllPlayback()
        if (playlist.isEmpty()) return

        currentIndex = index
        val item = playlist[index]
        val file = File(filesDir, item.fileName)
        if (!file.exists()) {
            scheduleNextMedia()
            return
        }

        if (item.type == "video") {
            playerView.visibility = View.VISIBLE
            exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exoPlayer?.prepare()
            exoPlayer?.play()
        } else {
            imageView.visibility = View.VISIBLE
            imageView.setImageURI(Uri.fromFile(file))
            handler.postDelayed(mediaEndRunnable, item.durationSec * 1000L)
        }
    }

    private fun scheduleNextMedia() {
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

    // HTTP API
    inner class SignageServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            // 1. Çalma Listesi Çek
            if (uri == "/api/playlist" && method == Method.GET) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(playlist))
            }

            // 2. Çalma Listesi Güncelle (Sıralama, Süre Değişimi veya Silme Sonrası)
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

            // 3. Dosya Yükleme
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

            // 4. Kumanda İçin Hafif Thumbnail (Küçük Önizleme) Sağlayıcı
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
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
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
        httpServer?.stop()
        exoPlayer?.release()
    }
}
