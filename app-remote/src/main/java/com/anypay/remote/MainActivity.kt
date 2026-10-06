package com.anypay.remote

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.projection.MediaProjectionManager
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
import androidx.core.content.ContextCompat
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.Collections

data class DeviceItem(
    val id: String,
    var host: String,
    var nameTag: String,
    var isFinding: Boolean = false
)

data class MediaItemModel(
    val id: String,
    var fileName: String,
    val type: String,
    var durationSec: Int,
    var waitAfterSec: Int,
    var animation: String = "fade",
    var fileDurationSec: Int = 0
)

class MainActivity : AppCompatActivity() {

    private lateinit var tabDevices: Button
    private lateinit var tabMedia: Button
    private lateinit var panelTabDevices: View
    private lateinit var panelTabMedia: View

    private lateinit var txtActiveDeviceName: TextView
    private lateinit var txtConnectionStatus: TextView

    private lateinit var btnScanNetwork: Button
    private lateinit var btnScanQr: Button
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private var deviceList = mutableListOf<DeviceItem>()
    private var activeDevice: DeviceItem? = null

    private lateinit var btnToggleScreenShare: Button
    private lateinit var edtDuration: EditText
    private lateinit var edtWaitAfter: EditText
    private lateinit var spinnerAnim: Spinner
    private lateinit var btnPickMedia: Button
    private lateinit var recyclerViewPlaylist: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private var playlist = mutableListOf<MediaItemModel>()

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var isSharingScreen = false
    private var isUdpListening = true

    private val animOptions = arrayOf("Solma (Fade)", "Soldan Kay", "Sağdan Kay", "Yakınlaş (Zoom)", "Animasyonsuz")
    private val animValues = arrayOf("fade", "slide_left", "slide_right", "zoom", "none")

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
                val id = json.optString("id", System.currentTimeMillis().toString())
                val ip = json.getString("ip")
                val port = json.getInt("port")
                val tag = json.optString("tag", "Menuboard ${deviceList.size + 1}")
                val host = "$ip:$port"

                updateOrAddDevice(id, host, tag)
                switchTab(false)
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

    private val screenShareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val host = activeDevice?.host ?: return@registerForActivityResult

            val req = Request.Builder().url("http://$host/api/screen/start").post("".toRequestBody(null)).build()
            httpClient.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })

            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("result_code", res.resultCode)
                putExtra("result_data", res.data)
                putExtra("target_host", host)
            }
            ContextCompat.startForegroundService(this, serviceIntent)

            isSharingScreen = true
            btnToggleScreenShare.text = "⏹ Paylaşımı Durdur"
            btnToggleScreenShare.setBackgroundColor(0xFFD32F2F.toInt())

            moveTaskToBack(true)
        } else {
            Toast.makeText(this, "Ekran paylaşımına izin verilmedi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabDevices = findViewById(R.id.tabDevices)
        tabMedia = findViewById(R.id.tabMedia)
        panelTabDevices = findViewById(R.id.panelTabDevices)
        panelTabMedia = findViewById(R.id.panelTabMedia)

        txtActiveDeviceName = findViewById(R.id.txtActiveDeviceName)
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus)

        btnScanNetwork = findViewById(R.id.btnScanNetwork)
        btnScanQr = findViewById(R.id.btnScanQr)
        rvDevices = findViewById(R.id.rvDevices)

        btnToggleScreenShare = findViewById(R.id.btnToggleScreenShare)
        edtDuration = findViewById(R.id.edtDuration)
        edtWaitAfter = findViewById(R.id.edtWaitAfter)
        spinnerAnim = findViewById(R.id.spinnerAnim)
        btnPickMedia = findViewById(R.id.btnPickMedia)
        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylist)

        spinnerAnim.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, animOptions)

        loadSavedDevices()

        deviceAdapter = DeviceAdapter()
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter

        mediaAdapter = MediaAdapter()
        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaylist.adapter = mediaAdapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = vh.adapterPosition
                val toPos = target.adapterPosition
                Collections.swap(playlist, fromPos, toPos)
                mediaAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                syncPlaylistToMenuboard()
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerViewPlaylist)

        tabDevices.setOnClickListener { switchTab(true) }
        tabMedia.setOnClickListener { switchTab(false) }

        btnScanQr.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt("Menuboard üzerindeki QR kodu taratın")
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            qrLauncher.launch(options)
        }

        btnScanNetwork.setOnClickListener {
            Toast.makeText(this, "Ağ taranıyor...", Toast.LENGTH_SHORT).show()
        }

        btnToggleScreenShare.setOnClickListener {
            if (activeDevice == null) {
                Toast.makeText(this, "Önce bir Menuboard seçin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isSharingScreen) {
                val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                screenShareLauncher.launch(mpManager.createScreenCaptureIntent())
            } else {
                stopScreenShare()
            }
        }

        btnPickMedia.setOnClickListener {
            if (activeDevice == null) {
                Toast.makeText(this, "Lütfen önce bir Menuboard seçin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }
            pickMediaLauncher.launch(intent)
        }

        if (deviceList.isNotEmpty()) {
            selectDevice(deviceList.first())
        }

        startUdpListener()
    }

    private fun stopScreenShare() {
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply { action = "STOP" }
        startService(serviceIntent)
        isSharingScreen = false
        btnToggleScreenShare.text = "📱 Ekranı Paylaş"
        btnToggleScreenShare.setBackgroundColor(0xFF7B1FA2.toInt())
    }

    private fun startUdpListener() {
        Thread {
            try {
                val socket = DatagramSocket(8888)
                val buffer = ByteArray(1024)
                while (isUdpListening) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length)
                    if (msg.startsWith("ANYPAY_DISCOVERY:")) {
                        val parts = msg.split(":")
                        if (parts.size >= 5) {
                            val id = parts[1]
                            val tag = parts[2]
                            val ip = parts[3]
                            val port = parts[4]
                            val host = "$ip:$port"
                            runOnUiThread {
                                updateOrAddDevice(id, host, tag)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun updateOrAddDevice(id: String, host: String, tag: String) {
        val existing = deviceList.find { it.id == id }
        if (existing != null) {
            existing.host = host
        } else {
            deviceList.add(DeviceItem(id, host, tag))
        }
        saveDevices()
        deviceAdapter.notifyDataSetChanged()

        if (activeDevice?.id == id) {
            activeDevice?.host = host
            txtActiveDeviceName.text = "Aktif: ${activeDevice?.nameTag} ($host)"
        }
    }

    private fun switchTab(showDevices: Boolean) {
        if (showDevices) {
            panelTabDevices.visibility = View.VISIBLE
            panelTabMedia.visibility = View.GONE
            tabDevices.setTextColor(0xFF00E676.toInt())
            tabMedia.setTextColor(0xFF888888.toInt())
        } else {
            panelTabDevices.visibility = View.GONE
            panelTabMedia.visibility = View.VISIBLE
            tabDevices.setTextColor(0xFF888888.toInt())
            tabMedia.setTextColor(0xFF00E676.toInt())
        }
    }

    private fun selectDevice(device: DeviceItem) {
        if (isSharingScreen) stopScreenShare()
        activeDevice = device
        txtActiveDeviceName.text = "Aktif: ${device.nameTag} (${device.host})"

        playlist.clear()
        mediaAdapter.notifyDataSetChanged()

        saveDevices()
        deviceAdapter.notifyDataSetChanged()
        checkConnectionAndSync()
    }

    private fun loadSavedDevices() {
        val prefs = getSharedPreferences("remote_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("device_list_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<DeviceItem>>() {}.type
            deviceList = gson.fromJson(json, type)
        }
    }

    private fun saveDevices() {
        val prefs = getSharedPreferences("remote_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("device_list_json", gson.toJson(deviceList)).apply()
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
        val host = activeDevice?.host ?: return
        val request = Request.Builder().url("http://$host/api/ping").build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { updateConnectionUi(false) }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val wasDisconnected = !isConnected
                    updateConnectionUi(true)
                    if (wasDisconnected || playlist.isEmpty()) fetchPlaylist()
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
                txtConnectionStatus.text = "Bağlı"
                txtConnectionStatus.setTextColor(0xFF2E7D32.toInt())
            } else {
                txtConnectionStatus.text = "Bağlantı Yok"
                txtConnectionStatus.setTextColor(0xFFD32F2F.toInt())
            }
        }
    }

    private fun fetchPlaylist() {
        val host = activeDevice?.host ?: return
        val request = Request.Builder().url("http://$host/api/playlist").build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { updateConnectionUi(false) }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<MutableList<MediaItemModel>>() {}.type
                    playlist = gson.fromJson(body, type)
                    runOnUiThread { mediaAdapter.notifyDataSetChanged() }
                }
            }
        })
    }

    private fun uploadMediaFile(uri: Uri) {
        val host = activeDevice?.host ?: return
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

        var fileDurationSec = 0
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                fileDurationSec = ((time?.toLong() ?: 0L) / 1000).toInt()
            } catch (_: Exception) {}
        }

        Toast.makeText(this, "Yükleniyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show()

        // RAM Tasarruflu Streaming RequestBody (Video Yüklerken Çökmeyi Önler)
        val customRequestBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

            override fun contentLength(): Long {
                return try {
                    contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                } catch (_: Exception) { -1L }
            }

            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, customRequestBody)
            .build()

        val uploadRequest = Request.Builder()
            .url("http://$host/api/upload?filename=$fileName")
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
        val host = activeDevice?.host ?: return
        val jsonPayload = gson.toJson(playlist)
        val body = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder().url("http://$host/api/playlist").post(body).build()
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

    private fun showEditTagDialog(device: DeviceItem) {
        val input = EditText(this).apply {
            hint = "Örn: 1, 2 veya Menuboard Kasa"
            setText(device.nameTag)
        }

        AlertDialog.Builder(this)
            .setTitle("Nametag Belirle")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val newTag = input.text.toString().trim()
                if (newTag.isNotEmpty()) {
                    device.nameTag = newTag
                    saveDevices()
                    deviceAdapter.notifyDataSetChanged()
                    if (activeDevice?.id == device.id) {
                        txtActiveDeviceName.text = "Aktif: ${device.nameTag} (${device.host})"
                    }
                    val req = Request.Builder().url("http://${device.host}/api/tag?tag=${Uri.encode(newTag)}").post("".toRequestBody(null)).build()
                    httpClient.newCall(req).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {}
                        override fun onResponse(call: Call, response: Response) { response.close() }
                    })
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun toggleFindDevice(device: DeviceItem) {
        device.isFinding = !device.isFinding
        deviceAdapter.notifyDataSetChanged()

        val url = "http://${device.host}/api/identify?show=${device.isFinding}&tag=${Uri.encode(device.nameTag)}"
        val request = Request.Builder().url(url).post("".toRequestBody(null)).build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        isUdpListening = false
    }

    // --- RECYCLERVIEW ADAPTÖRLERİ ---

    inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
        inner class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val rbSelected: RadioButton = v.findViewById(R.id.rbSelected)
            val txtDeviceTag: TextView = v.findViewById(R.id.txtDeviceTag)
            val txtDeviceIp: TextView = v.findViewById(R.id.txtDeviceIp)
            val btnEditTag: ImageButton = v.findViewById(R.id.btnEditTag)
            val btnFindDevice: Button = v.findViewById(R.id.btnFindDevice)
            val btnDeleteDevice: ImageButton = v.findViewById(R.id.btnDeleteDevice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(v)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val dev = deviceList[position]
            holder.txtDeviceTag.text = dev.nameTag
            holder.txtDeviceIp.text = dev.host
            holder.rbSelected.isChecked = (activeDevice?.id == dev.id)

            if (dev.isFinding) {
                holder.btnFindDevice.text = "Gizle"
                holder.btnFindDevice.setBackgroundColor(0xFFD32F2F.toInt())
            } else {
                holder.btnFindDevice.text = "Bul"
                holder.btnFindDevice.setBackgroundColor(0xFFF57C00.toInt())
            }

            holder.itemView.setOnClickListener {
                selectDevice(dev)
                switchTab(false)
            }

            holder.btnEditTag.setOnClickListener { showEditTagDialog(dev) }
            holder.btnFindDevice.setOnClickListener { toggleFindDevice(dev) }

            holder.btnDeleteDevice.setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Cihazı Kaldır")
                    .setMessage("${dev.nameTag} silinsin mi?")
                    .setPositiveButton("Kaldır") { _, _ ->
                        deviceList.removeAt(holder.adapterPosition)
                        if (activeDevice?.id == dev.id) {
                            activeDevice = deviceList.firstOrNull()
                            if (activeDevice != null) selectDevice(activeDevice!!)
                            else {
                                txtActiveDeviceName.text = "Seçili Cihaz Yok"
                                playlist.clear()
                                mediaAdapter.notifyDataSetChanged()
                            }
                        }
                        saveDevices()
                        notifyDataSetChanged()
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        }

        override fun getItemCount(): Int = deviceList.size
    }

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
            val host = activeDevice?.host ?: ""

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

            val thumbUrl = "http://$host/api/thumbnail?filename=${item.fileName}"
            Glide.with(holder.itemView.context)
                .load(thumbUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.imgThumb)

            holder.btnEdit.setOnClickListener {
                val edtDur = EditText(this@MainActivity).apply {
                    hint = "Süre (0=tamamı)"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setText(item.durationSec.toString())
                }
                val edtWait = EditText(this@MainActivity).apply {
                    hint = "Ara Bekleme (sn)"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setText(item.waitAfterSec.toString())
                }
                val spAnim = Spinner(this@MainActivity).apply {
                    adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, animOptions)
                    val idx = animValues.indexOf(item.animation)
                    if (idx >= 0) setSelection(idx)
                }

                val layout = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 40, 50, 10)
                    addView(TextView(this@MainActivity).apply { text = "Oynatma Süresi (sn):" })
                    addView(edtDur)
                    addView(TextView(this@MainActivity).apply { text = "İki Medya Arası Bekleme (sn):" })
                    addView(edtWait)
                    addView(TextView(this@MainActivity).apply { text = "Giriş Animasyonu:" })
                    addView(spAnim)
                }

                AlertDialog.Builder(this@MainActivity)
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

            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Medyayı Sil")
                    .setMessage("${item.fileName} silinsin mi?")
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
