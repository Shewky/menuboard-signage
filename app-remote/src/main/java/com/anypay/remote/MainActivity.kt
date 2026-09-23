package com.anypay.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class MediaItemModel(
    val id: String,
    val fileName: String,
    val type: String,
    val durationSec: Int,
    val waitAfterSec: Int
)

class MainActivity : AppCompatActivity() {

    private lateinit var btnScanQr: Button
    private lateinit var txtConnectionStatus: TextView
    private lateinit var panelControls: View
    private lateinit var edtDuration: EditText
    private lateinit var edtWaitAfter: EditText
    private lateinit var btnPickMedia: Button
    private lateinit var listViewPlaylist: ListView

    private val httpClient = OkHttpClient()
    private val gson = Gson()

    private var targetHost = "" // örn: "192.168.1.50:8080"
    private var playlist = mutableListOf<MediaItemModel>()

    // QR Tarama Sonucu
    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            try {
                val json = JSONObject(result.contents)
                val ip = json.getString("ip")
                val port = json.getInt("port")
                targetHost = "$ip:$port"

                getSharedPreferences("remote_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("saved_host", targetHost)
                    .apply()

                updateConnectionUi(true)
                fetchPlaylist()
            } catch (e: Exception) {
                Toast.makeText(this, "Geçersiz QR Kod!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Galeri / Dosya Seçme Sonucu
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null) {
            val uri = res.data!!.data!!
            uploadMediaFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScanQr = findViewById(R.id.btnScanQr)
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus)
        panelControls = findViewById(R.id.panelControls)
        edtDuration = findViewById(R.id.edtDuration)
        edtWaitAfter = findViewById(R.id.edtWaitAfter)
        btnPickMedia = findViewById(R.id.btnPickMedia)
        listViewPlaylist = findViewById(R.id.listViewPlaylist)

        btnScanQr.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt("Menuboard üzerindeki QR kodu taratın")
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            qrLauncher.launch(options)
        }

        btnPickMedia.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }
            pickMediaLauncher.launch(intent)
        }

        // Önceden kayıtlı eşleme var mı kontrol et
        val saved = getSharedPreferences("remote_prefs", Context.MODE_PRIVATE).getString("saved_host", null)
        if (!saved.isNullOrEmpty()) {
            targetHost = saved
            updateConnectionUi(true)
            fetchPlaylist()
        }
    }

    private fun updateConnectionUi(connected: Boolean) {
        runOnUiThread {
            if (connected) {
                txtConnectionStatus.text = "Bağlı ($targetHost)"
                txtConnectionStatus.setTextColor(0xFF2E7D32.toInt())
                panelControls.visibility = View.VISIBLE
            } else {
                txtConnectionStatus.text = "Bağlantı Yok"
                txtConnectionStatus.setTextColor(0xFFD32F2F.toInt())
                panelControls.visibility = View.GONE
            }
        }
    }

    private fun fetchPlaylist() {
        if (targetHost.isEmpty()) return
        val request = Request.Builder().url("http://$targetHost/api/playlist").build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateConnectionUi(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<MutableList<MediaItemModel>>() {}.type
                    playlist = gson.fromJson(body, type)

                    runOnUiThread {
                        val adapter = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_list_item_1,
                            playlist.map { "${it.type.uppercase()} | ${it.fileName} (${it.durationSec}s) [+${it.waitAfterSec}s ara]" }
                        )
                        listViewPlaylist.adapter = adapter
                    }
                }
            }
        })
    }

    private fun uploadMediaFile(uri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val isVideo = mimeType.startsWith("video")
        val type = if (isVideo) "video" else "image"

        var fileName = "media_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val inputStream = contentResolver.openInputStream(uri) ?: return
        val bytes = inputStream.readBytes()
        inputStream.close()

        Toast.makeText(this, "Yükleniyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        val uploadRequest = Request.Builder()
            .url("http://$targetHost/api/upload?filename=$fileName")
            .post(requestBody)
            .build()

        httpClient.newCall(uploadRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Yükleme Başarısız!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val dur = edtDuration.text.toString().toIntOrNull() ?: 10
                    val wait = edtWaitAfter.text.toString().toIntOrNull() ?: 0

                    val newItem = MediaItemModel(
                        id = System.currentTimeMillis().toString(),
                        fileName = fileName,
                        type = type,
                        durationSec = dur,
                        waitAfterSec = wait
                    )

                    playlist.add(newItem)
                    syncPlaylistToMenuboard()
                }
            }
        })
    }

    private fun syncPlaylistToMenuboard() {
        val jsonPayload = gson.toJson(playlist)
        val body = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("http://$targetHost/api/playlist")
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Liste senkronize edilemedi!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Medya Menuboard'a Eklendi!", Toast.LENGTH_SHORT).show()
                    fetchPlaylist()
                }
            }
        })
    }
}
