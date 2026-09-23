package com.anypay.remote

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
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
import java.util.Collections

data class MediaItemModel(
    val id: String,
    var fileName: String,
    val type: String,
    var durationSec: Int,
    var waitAfterSec: Int,
    var animation: String = "fade",
    var fileDurationSec: Int = 0 // Videonun gerçek süresi
)

class MainActivity : AppCompatActivity() {

    private lateinit var btnScanQr: Button
    private lateinit var txtConnectionStatus: TextView
    private lateinit var panelControls: View
    private lateinit var edtDuration: EditText
    private lateinit var edtWaitAfter: EditText
    private lateinit var spinnerAnim: Spinner
    private lateinit var btnPickMedia: Button
    private lateinit var recyclerViewPlaylist: RecyclerView

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private var targetHost = ""
    private var isConnected = false
    private var playlist = mutableListOf<MediaItemModel>()
    private lateinit var adapter: MediaAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val animOptions = arrayOf("Solma (Fade)", "Soldan Kay", "Sağdan Kay", "Yakınlaş (Zoom)", "Animasyonsuz")
    private val animValues = arrayOf("fade", "slide_left", "slide_right", "zoom", "none")

    // Her 10 saniyede bir bağlantıyı denetleyen Heartbeat döngüsü
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            checkConnectionAndSync()
            handler.postDelayed(this, 10000L)
        }
    }

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

                checkConnectionAndSync()
            } catch (_: Exception) {
                Toast.makeText(this, "Geçersiz QR Kod!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null) {
            uploadMediaFile(res.data!!.data!)
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
        spinnerAnim = findViewById(R.id.spinnerAnim)
        btnPickMedia = findViewById(R.id.btnPickMedia)
        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylist)

        spinnerAnim.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, animOptions)

        adapter = MediaAdapter()
        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaylist.adapter = adapter

        // Sürükle ve Bırak (Drag & Drop) Sıralama Desteği
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = vh.adapterPosition
                val toPos = target.adapterPosition
                Collections.swap(playlist, fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                syncPlaylistToMenuboard() // Taşıma bitince yeni sırayı menuboard'a anında kaydet
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerViewPlaylist)

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

        targetHost = getSharedPreferences("remote_prefs", Context.MODE_PRIVATE).getString("saved_host", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        handler.post(heartbeatRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(heartbeatRunnable)
    }

    private fun checkConnectionAndSync() {
        if (targetHost.isEmpty()) {
            updateConnectionUi(false)
            return
        }

        val request = Request.Builder().url("http://$targetHost/api/ping").build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateConnectionUi(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val wasDisconnected = !isConnected
                    updateConnectionUi(true)
                    if (wasDisconnected) {
                        fetchPlaylist()
                    }
                } else {
                    updateConnectionUi(false)
                }
            }
        })
    }

    private fun updateConnectionUi(connected: Boolean) {
        isConnected = connected
        runOnUiThread {
            if (connected) {
                txtConnectionStatus.text = "Bağlı ($targetHost)"
                txtConnectionStatus.setTextColor(0xFF2E7D32.toInt())
                panelControls.visibility = View.VISIBLE
            } else {
                txtConnectionStatus.text = if (targetHost.isEmpty()) "QR Okutun" else "Bağlantı Bekleniyor... ($targetHost)"
                txtConnectionStatus.setTextColor(0xFFE65100.toInt())
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

        // Video ise gerçek süresini oku
        var fileDurationSec = 0
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                fileDurationSec = ((time?.toLong() ?: 0L) / 1000).toInt()
            } catch (_: Exception) {}
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
                    val dur = edtDuration.text.toString().toIntOrNull() ?: 0
                    val wait = edtWaitAfter.text.toString().toIntOrNull() ?: 0
                    val anim = animValues[spinnerAnim.selectedItemPosition]

                    val newItem = MediaItemModel(
                        id = System.currentTimeMillis().toString(),
                        fileName = fileName,
                        type = type,
                        durationSec = dur,
                        waitAfterSec = wait,
                        animation = anim,
                        fileDurationSec = fileDurationSec
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
                    Toast.makeText(this@MainActivity, "Menuboard Güncellendi!", Toast.LENGTH_SHORT).show()
                    fetchPlaylist()
                }
            }
        })
    }

    private fun showEditDialog(item: MediaItemModel) {
        val edtDur = EditText(this).apply {
            hint = "Süre (sn / Video için 0=tamamı)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(item.durationSec.toString())
        }
        val edtWait = EditText(this).apply {
            hint = "Ara Bekleme (sn)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(item.waitAfterSec.toString())
        }
        val spAnim = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, animOptions)
            val index = animValues.indexOf(item.animation)
            if (index >= 0) setSelection(index)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(TextView(this@MainActivity).apply { text = "Oynatma Süresi (sn):" })
            addView(edtDur)
            addView(TextView(this@MainActivity).apply { text = "İki Medya Arası Bekleme (sn):" })
            addView(edtWait)
            addView(TextView(this@MainActivity).apply { text = "Giriş Animasyonu:" })
            addView(spAnim)
        }

        AlertDialog.Builder(this)
            .setTitle("Medya Ayarları")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                item.durationSec = edtDur.text.toString().toIntOrNull() ?: item.durationSec
                item.waitAfterSec = edtWait.text.toString().toIntOrNull() ?: item.waitAfterSec
                item.animation = animValues[spAnim.selectedItemPosition]
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
            val txtAnimBadge: TextView = v.findViewById(R.id.txtAnimBadge)
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

            if (item.type == "video") {
                val totalStr = if (item.fileDurationSec > 0) "${item.fileDurationSec}sn" else "Bilinmiyor"
                val playStr = if (item.durationSec > 0) "${item.durationSec}sn kesit" else "Tamamı ($totalStr)"
                holder.txtDetails.text = "Video: $playStr | +${item.waitAfterSec}s ara"
            } else {
                val dur = if (item.durationSec > 0) item.durationSec else 10
                holder.txtDetails.text = "Resim: ${dur}s süre | +${item.waitAfterSec}s ara"
            }

            val animName = animOptions.getOrNull(animValues.indexOf(item.animation)) ?: "Solma"
            holder.txtAnimBadge.text = "Animasyon: $animName"

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
