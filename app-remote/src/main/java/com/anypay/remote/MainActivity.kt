package com.anypay.remote

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
    var fileName: String,
    val type: String,
    var durationSec: Int,
    var waitAfterSec: Int
)

class MainActivity : AppCompatActivity() {

    private lateinit var btnScanQr: Button
    private lateinit var txtConnectionStatus: TextView
    private lateinit var panelControls: View
    private lateinit var edtDuration: EditText
    private lateinit var edtWaitAfter: EditText
    private lateinit var btnPickMedia: Button
    private lateinit var recyclerViewPlaylist: RecyclerView

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private var targetHost = ""
    private var playlist = mutableListOf<MediaItemModel>()
    private lateinit var adapter: MediaAdapter

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
            } catch (_: Exception) {
                Toast.makeText(this, "Geçersiz QR Kod!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null) {
            uploadMediaFile(res.data!!.data!!)
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
        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylist)

        adapter = MediaAdapter()
        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaylist.adapter = adapter

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
            override fun onFailure(call: Call, e: IOException) { updateConnectionUi(false) }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<MutableList<MediaItemModel>>() {}.type
                    playlist = gson.fromJson(body, type)
                    runOnUiThread { adapter.notifyDataSetChanged() }
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

        Toast.makeText(this, "Yükleniyor...", Toast.LENGTH_SHORT).show()

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
                runOnUiThread { Toast.makeText(this@MainActivity, "Yükleme Başarısız!", Toast.LENGTH_SHORT).show() }
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
                runOnUiThread { Toast.makeText(this@MainActivity, "Senkronizasyon Başarısız!", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Liste Güncellendi!", Toast.LENGTH_SHORT).show()
                    fetchPlaylist()
                }
            }
        })
    }

    private fun showEditDialog(item: MediaItemModel) {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        val edtDur = EditText(this).apply {
            hint = "Görsel Süresi (sn)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(item.durationSec.toString())
        }
        val edtWait = EditText(this).apply {
            hint = "Ara Bekleme (sn)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(item.waitAfterSec.toString())
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(edtDur)
            addView(edtWait)
        }

        AlertDialog.Builder(this)
            .setTitle("Parametreleri Düzenle")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                item.durationSec = edtDur.text.toString().toIntOrNull() ?: item.durationSec
                item.waitAfterSec = edtWait.text.toString().toIntOrNull() ?: item.waitAfterSec
                syncPlaylistToMenuboard()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // RecyclerView Adaptörü
    inner class MediaAdapter : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

        inner class MediaViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val imgThumb: ImageView = v.findViewById(R.id.imgThumb)
            val txtTypeBadge: TextView = v.findViewById(R.id.txtTypeBadge)
            val txtFileName: TextView = v.findViewById(R.id.txtFileName)
            val txtDetails: TextView = v.findViewById(R.id.txtDetails)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
            return MediaViewHolder(v)
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val item = playlist[position]
            holder.txtFileName.text = item.fileName
            holder.txtTypeBadge.text = if (item.type == "video") "VIDEO" else "RESIM"
            holder.txtTypeBadge.setBackgroundColor(if (item.type == "video") 0xCC1976D2.toInt() else 0xCC388E3C.toInt())
            
            val details = if (item.type == "video") "Oynatılıyor | +${item.waitAfterSec}s bekleme" else "${item.durationSec}s süre | +${item.waitAfterSec}s bekleme"
            holder.txtDetails.text = details

            // Menuboard'dan küçük resmi hafif olarak çek (Videoyu asla oynatmaz)
            val thumbUrl = "http://$targetHost/api/thumbnail?filename=${item.fileName}"
            Glide.with(holder.itemView.context)
                .load(thumbUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.imgThumb)

            holder.btnEdit.setOnClickListener { showEditDialog(item) }

            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Medyayı Sil")
                    .setMessage("${item.fileName} menuboard'dan silinsin mi?")
                    .setPositiveButton("Sil") { _, _ ->
                        playlist.removeAt(holder.adapterPosition)
                        syncPlaylistToMenuboard()
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        }

        override fun getItemCount(): Int = playlist.size
    }
}
