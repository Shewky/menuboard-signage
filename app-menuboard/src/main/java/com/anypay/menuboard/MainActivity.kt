package com.anypay.menuboard

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.View
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

data class MediaItemModel(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val type: String, // "video" veya "image"
    val durationSec: Int = 10, // Resim veya video oynatma süresi
    val waitAfterSec: Int = 0  // İki medya arası siyah ekranda bekleme süresi
)

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var imageView: ImageView
    private lateinit var qrOverlay: View
    private lateinit var imgQrCode: ImageView
    private lateinit var txtIpAddress: TextView
    private lateinit var btnSettings: ImageView

    private var exoPlayer: ExoPlayer? = null
    private var httpServer: SignageServer? = null
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())

    private var playlist = mutableListOf<MediaItemModel>()
    private var currentIndex = 0
    private var currentPairToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.playerView)
        imageView = findViewById(R.id.imageView)
        qrOverlay = findViewById(R.id.qrOverlay)
        imgQrCode = findViewById(R.id.imgQrCode)
        txtIpAddress = findViewById(R.id.txtIpAddress)
        btnSettings = findViewById(R.id.btnSettings)

        initPlayer()
        loadSavedState()

        btnSettings.setOnClickListener {
            showQrOverlay()
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
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(payload, BarcodeFormat.QR_CODE, 500, 500)
            imgQrCode.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
    }

    private fun showQrOverlay() {
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.pause()
        qrOverlay.visibility = View.VISIBLE
    }

    private fun hideQrOverlay() {
        qrOverlay.visibility = View.GONE
    }

    private fun startPlayback() {
        if (playlist.isEmpty()) {
            showQrOverlay()
            return
        }
        playItem(currentIndex % playlist.size)
    }

    private fun playItem(index: Int) {
        if (playlist.isEmpty()) return
        currentIndex = index
        val item = playlist[index]
        val file = File(filesDir, item.fileName)
        if (!file.exists()) {
            scheduleNextMedia()
            return
        }

        if (item.type == "video") {
            imageView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            exoPlayer?.prepare()
            exoPlayer?.play()
        } else {
            playerView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setImageURI(Uri.fromFile(file))

            handler.postDelayed({
                scheduleNextMedia()
            }, item.durationSec * 1000L)
        }
    }

    private fun scheduleNextMedia() {
        val currentItem = if (playlist.isNotEmpty()) playlist[currentIndex % playlist.size] else null
        val waitTime = (currentItem?.waitAfterSec ?: 0) * 1000L

        playerView.visibility = View.GONE
        imageView.visibility = View.GONE

        handler.postDelayed({
            currentIndex = (currentIndex + 1) % playlist.size
            playItem(currentIndex)
        }, waitTime)
    }

    // Gömülü HTTP Sunucusu (Telefonun iletişim kuracağı API)
    inner class SignageServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

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

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        exoPlayer?.release()
    }
}
