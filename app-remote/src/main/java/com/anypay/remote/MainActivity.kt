package com.anypay.remote

import android.app.Activity
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
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.sql.DriverManager
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

data class DeviceItem(
    val id: String,
    var host: String,
    var nameTag: String,
    var isFinding: Boolean = false,
    var isOnline: Boolean = false,
    var lastSeenMs: Long = 0L
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

data class SefimProductItem(
    val id: String,
    val productName: String,
    val price: String,
    val subDetails: String,
    var isSelected: Boolean = true,
    var imageBase64: String? = null
)

data class SefimBackupPayload(
    val dbServer: String = "",
    val dbName: String = "",
    val dbUser: String = "",
    val dbPass: String = "",
    val themeIndex: Int = 0,
    val fontSizeIndex: Int = 1,
    val isBold: Boolean = true,
    val bgObjectMode: String = "none",
    val products: List<SefimProductItem> = emptyList(),
    val imageCacheByName: Map<String, String> = emptyMap()
)

data class RemoteAppInfo(
    val appName: String,
    val packageName: String
)

class MainActivity : AppCompatActivity() {

    private val GITHUB_REPO = "KULLANICI_ADINIZ/menuboard-signage"

    private lateinit var tabDevices: Button
    private lateinit var tabMedia: Button
    private lateinit var tabSefim: Button
    private lateinit var panelTabDevices: View
    private lateinit var panelTabMedia: View
    private lateinit var panelTabSefim: View

    private lateinit var txtActiveDeviceName: TextView
    private lateinit var txtRemoteVersion: TextView
    private lateinit var txtConnectionStatus: TextView

    private lateinit var btnScanNetwork: Button
    private lateinit var btnScanQr: Button
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private var deviceList = mutableListOf<DeviceItem>()
    private var activeDevice: DeviceItem? = null

    private lateinit var btnToggleScreenShare: Button
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var edtDuration: EditText
    private lateinit var edtWaitAfter: EditText
    private lateinit var spinnerAnim: Spinner
    private lateinit var btnPickMedia: Button
    private lateinit var recyclerViewPlaylist: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private var playlist = mutableListOf<MediaItemModel>()

    private lateinit var btnDbConfig: Button
    private lateinit var btnFetchSefim: Button
    private lateinit var spinnerSefimTheme: Spinner
    private lateinit var spinnerSefimFontSize: Spinner
    private lateinit var spinnerSefimBgObject: Spinner
    private lateinit var cbSefimBold: CheckBox
    private lateinit var rvSefimProducts: RecyclerView
    private lateinit var btnPublishSefimMenu: Button
    private lateinit var sefimAdapter: SefimProductAdapter
    private var sefimProducts = mutableListOf<SefimProductItem>()

    // Ürün İsmine Göre Kalıcı Görsel Önbelleği (productName -> Base64 Image)
    private var productImageCache = mutableMapOf<String, String>()
    private var pendingPhotoProductName: String? = null

    // Uzaktan APK Gönderilecek Hedef Cihaz
    private var targetApkDevice: DeviceItem? = null

    private lateinit var imgFooterLogo: ImageView
    private lateinit var btnSupportCall: Button

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    private var isSharingScreen = false
    private var isUdpListening = true

    private val animOptions = arrayOf(
        "Solma (Fade)", "Soldan Kay", "Sağdan Kay", "Yukarıdan Kay",
        "Aşağıdan Kay", "Yakınlaş (Zoom In)", "Uzaklaş (Zoom Out)", "Animasyonsuz"
    )
    private val animValues = arrayOf("fade", "slide_left", "slide_right", "slide_up", "slide_down", "zoom_in", "zoom_out", "none")

    private val themeNames = arrayOf(
        "🖤 Koyu Lüks (Gold)",
        "🤍 Modern Beyaz",
        "🔥 Restoran Ateşi",
        "🌌 Neon Cyberpunk"
    )
    private val fontSizes = arrayOf("Küçük (14pt)", "Orta (18pt)", "Büyük (22pt)", "Dev (26pt)")
    private val fontSizeValues = arrayOf(14f, 18f, 22f, 26f)

    private val bgObjectNames = arrayOf(
        "Yok (Sade Arka Plan)",
        "🐟 Yüzen Balıklar",
        "🥙 Dönen Döner & Buhar",
        "💨 Nargile Dumanı"
    )
    private val bgObjectValues = arrayOf("none", "fish", "doner", "hookah")

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            checkAllDevicesStatus()
            handler.postDelayed(this, 8000L)
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

                updateOrAddDevice(id, host, tag, true)
                switchTab(1)
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

    // Ürün İçin Görsel Seçici (İsme Göre Önbelleğe Kaydeder)
    private val pickProductPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val prodName = pendingPhotoProductName
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null && !prodName.isNullOrEmpty()) {
            val uri = res.data!!.data!!
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val rawBmp = BitmapFactory.decodeStream(input)
                    if (rawBmp != null) {
                        val scaled = Bitmap.createScaledBitmap(rawBmp, 220, 220, true)
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                        productImageCache[prodName] = b64
                        sefimProducts.filter { it.productName.equals(prodName, ignoreCase = true) }.forEach {
                            it.imageBase64 = b64
                        }
                        saveLocalSefimState()
                        pushSefimBackupToMenuboard()
                        sefimAdapter.notifyDataSetChanged()
                        Toast.makeText(this, "'$prodName' görseli önbelleğe kaydedildi.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(this, "Görsel işlenemedi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Uzaktan Menuboard'a Kurulacak APK Dosya Seçici
    private val pickRemoteApkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val target = targetApkDevice
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null && target != null) {
            uploadAndInstallApkOnMenuboard(target, res.data!!.data!!)
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
        tabSefim = findViewById(R.id.tabSefim)
        panelTabDevices = findViewById(R.id.panelTabDevices)
        panelTabMedia = findViewById(R.id.panelTabMedia)
        panelTabSefim = findViewById(R.id.panelTabSefim)

        txtActiveDeviceName = findViewById(R.id.txtActiveDeviceName)
        txtRemoteVersion = findViewById(R.id.txtRemoteVersion)
        txtConnectionStatus = findViewById(R.id.txtConnectionStatus)

        try {
            val vName = packageManager.getPackageInfo(packageName, 0).versionName
            txtRemoteVersion.text = "v$vName"
        } catch (_: Exception) {}

        btnScanNetwork = findViewById(R.id.btnScanNetwork)
        btnScanQr = findViewById(R.id.btnScanQr)
        rvDevices = findViewById(R.id.rvDevices)

        btnToggleScreenShare = findViewById(R.id.btnToggleScreenShare)
        uploadProgressBar = findViewById(R.id.uploadProgressBar)
        edtDuration = findViewById(R.id.edtDuration)
        edtWaitAfter = findViewById(R.id.edtWaitAfter)
        spinnerAnim = findViewById(R.id.spinnerAnim)
        btnPickMedia = findViewById(R.id.btnPickMedia)
        recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylist)

        btnDbConfig = findViewById(R.id.btnDbConfig)
        btnFetchSefim = findViewById(R.id.btnFetchSefim)
        spinnerSefimTheme = findViewById(R.id.spinnerSefimTheme)
        spinnerSefimFontSize = findViewById(R.id.spinnerSefimFontSize)
        spinnerSefimBgObject = findViewById(R.id.spinnerSefimBgObject)
        cbSefimBold = findViewById(R.id.cbSefimBold)
        rvSefimProducts = findViewById(R.id.rvSefimProducts)
        btnPublishSefimMenu = findViewById(R.id.btnPublishSefimMenu)

        imgFooterLogo = findViewById(R.id.imgFooterLogo)
        btnSupportCall = findViewById(R.id.btnSupportCall)

        spinnerAnim.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, animOptions)
        spinnerSefimTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeNames)
        spinnerSefimFontSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontSizes)
        spinnerSefimBgObject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bgObjectNames)
        spinnerSefimFontSize.setSelection(1)

        loadSavedDevices()
        loadLocalSefimState()

        deviceAdapter = DeviceAdapter()
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter

        mediaAdapter = MediaAdapter()
        recyclerViewPlaylist.layoutManager = LinearLayoutManager(this)
        recyclerViewPlaylist.adapter = mediaAdapter

        sefimAdapter = SefimProductAdapter()
        rvSefimProducts.layoutManager = LinearLayoutManager(this)
        rvSefimProducts.adapter = sefimAdapter

        val styleListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applySefimPreviewBackground()
                sefimAdapter.notifyDataSetChanged()
                saveLocalSefimState()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerSefimTheme.onItemSelectedListener = styleListener
        spinnerSefimFontSize.onItemSelectedListener = styleListener
        cbSefimBold.setOnCheckedChangeListener { _, _ ->
            sefimAdapter.notifyDataSetChanged()
            saveLocalSefimState()
        }

        // Hareketli Arka Plan Objesi Seçildiğinde Menuboard'a Anında İlet
        spinnerSefimBgObject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveLocalSefimState()
                val mode = bgObjectValues[position]
                val host = activeDevice?.host ?: return
                val req = Request.Builder().url("http://$host/api/sefim/bgobject?mode=$mode").post("".toRequestBody(null)).build()
                httpClient.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) { response.close() }
                })
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        tabDevices.setOnClickListener { switchTab(0) }
        tabMedia.setOnClickListener { switchTab(1) }
        tabSefim.setOnClickListener { switchTab(2) }

        btnScanQr.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt("Menuboard üzerindeki QR kodu taratın")
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            qrLauncher.launch(options)
        }

        btnScanNetwork.setOnClickListener {
            Toast.makeText(this, "Ağdaki tüm cihazların durumu kontrol ediliyor...", Toast.LENGTH_SHORT).show()
            checkAllDevicesStatus()
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

        btnDbConfig.setOnClickListener { showDbConfigDialog() }
        btnFetchSefim.setOnClickListener { fetchSefimProductsFromDb() }
        btnPublishSefimMenu.setOnClickListener { publishSefimMenuToMenuboard() }

        imgFooterLogo.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.interfon.com.tr"))
            startActivity(browserIntent)
        }

        btnSupportCall.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:03243292001"))
            startActivity(callIntent)
        }

        if (deviceList.isNotEmpty()) {
            selectDevice(deviceList.first())
        }

        startUdpListener()
        checkForUpdate()
    }

    // --- BUL YANINDAKİ AYAR (⚙️) MENÜSÜ: UZAKTAN APK KURMA & UYGULAMA BAŞLATMA ---

    private fun showDeviceAdvancedMenu(device: DeviceItem) {
        val options = arrayOf(
            "📦 Uzaktan APK Gönder & Kur",
            "🚀 Menuboard'da Kurulu Uygulamaları Gör / Başlat"
        )
        AlertDialog.Builder(this)
            .setTitle("${device.nameTag} - Cihaz Yönetimi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        targetApkDevice = device
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        pickRemoteApkLauncher.launch(intent)
                    }
                    1 -> fetchAndShowInstalledApps(device)
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun uploadAndInstallApkOnMenuboard(device: DeviceItem, uri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val totalLength = try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) { -1L }

        Toast.makeText(this, "APK ${device.nameTag} cihazına gönderiliyor...", Toast.LENGTH_LONG).show()
        uploadProgressBar.visibility = View.VISIBLE
        uploadProgressBar.progress = 0

        val customBody = object : RequestBody() {
            override fun contentType() = "application/vnd.android.package-archive".toMediaTypeOrNull()
            override fun contentLength(): Long = totalLength

            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var uploaded: Long = 0
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        if (totalLength > 0) {
                            uploaded += read
                            val p = ((uploaded * 100) / totalLength).toInt()
                            runOnUiThread { uploadProgressBar.progress = p }
                        }
                    }
                }
            }
        }

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "remote_install.apk", customBody)
            .build()

        val req = Request.Builder()
            .url("http://${device.host}/api/app/install")
            .post(multipart)
            .build()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    uploadProgressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "APK gönderimi başarısız!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                runOnUiThread {
                    uploadProgressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "APK gönderildi ve Menuboard'da kurulum başlatıldı!", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun fetchAndShowInstalledApps(device: DeviceItem) {
        Toast.makeText(this, "${device.nameTag} üzerindeki uygulamalar getiriliyor...", Toast.LENGTH_SHORT).show()
        val req = Request.Builder().url("http://${device.host}/api/apps").build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Cihaza ulaşılamadı!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "[]"
                response.close()
                try {
                    val type = object : TypeToken<List<RemoteAppInfo>>() {}.type
                    val apps: List<RemoteAppInfo> = gson.fromJson(body, type)
                    if (apps.isEmpty()) {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Uygulama listesi boş döndü.", Toast.LENGTH_SHORT).show() }
                        return
                    }
                    val labels = apps.map { "${it.appName}\n(${it.packageName})" }.toTypedArray()
                    runOnUiThread {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("${device.nameTag} - Uygulama Başlat")
                            .setItems(labels) { _, idx ->
                                launchRemoteApp(device, apps[idx])
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    }
                } catch (_: Exception) {}
            }
        })
    }

    private fun launchRemoteApp(device: DeviceItem, app: RemoteAppInfo) {
        val req = Request.Builder()
            .url("http://${device.host}/api/apps/launch?pkg=${Uri.encode(app.packageName)}")
            .post("".toRequestBody(null))
            .build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Başlatma komutu gönderilemedi!", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "${app.appName} Menuboard'da başlatıldı!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // --- ÇİFT YÖNLÜ ŞEFİM YEDEKLEME VE OTOMATİK KURTARMA (TWO-WAY SYNC) ---

    private fun saveLocalSefimState() {
        val dbPrefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val payload = SefimBackupPayload(
            dbServer = dbPrefs.getString("db_server", "192.168.2.167\\YIRMIBES") ?: "",
            dbName = dbPrefs.getString("db_name", "BALIKCISUKRU") ?: "",
            dbUser = dbPrefs.getString("db_user", "sa") ?: "",
            dbPass = dbPrefs.getString("db_pass", "") ?: "",
            themeIndex = spinnerSefimTheme.selectedItemPosition,
            fontSizeIndex = spinnerSefimFontSize.selectedItemPosition,
            isBold = cbSefimBold.isChecked,
            bgObjectMode = bgObjectValues.getOrElse(spinnerSefimBgObject.selectedItemPosition) { "none" },
            products = sefimProducts,
            imageCacheByName = productImageCache
        )
        dbPrefs.edit().putString("sefim_full_backup_json", gson.toJson(payload)).apply()
    }

    private fun loadLocalSefimState() {
        val dbPrefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val json = dbPrefs.getString("sefim_full_backup_json", null) ?: return
        try {
            val payload = gson.fromJson(json, SefimBackupPayload::class.java)
            productImageCache.clear()
            productImageCache.putAll(payload.imageCacheByName)

            sefimProducts.clear()
            sefimProducts.addAll(payload.products)
            sefimProducts.forEach { prod ->
                if (prod.imageBase64.isNullOrEmpty() && productImageCache.containsKey(prod.productName)) {
                    prod.imageBase64 = productImageCache[prod.productName]
                }
            }
        } catch (_: Exception) {}
    }

    // Kumandadaki Şefim verilerini Menuboard'a yedekle (Kumanda sıfırlanırsa oradan geri gelsin)
    private fun pushSefimBackupToMenuboard() {
        val host = activeDevice?.host ?: return
        val dbPrefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val json = dbPrefs.getString("sefim_full_backup_json", null) ?: return

        val req = Request.Builder()
            .url("http://$host/api/sefim/backup")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    // Bağlantı kurulduğunda: Kumanda boşsa Menuboard'dan çek; Menuboard boşsa Kumanda'dan gönder
    private fun syncSefimBackupWithMenuboard() {
        val host = activeDevice?.host ?: return
        val req = Request.Builder().url("http://$host/api/sefim/backup").build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val remoteJson = response.body?.string()?.trim() ?: ""
                response.close()

                if (remoteJson.isNotEmpty() && sefimProducts.isEmpty()) {
                    // Kumanda sıfırlanmış -> Menuboard'daki yedeği Kumandaya otomatik yükle!
                    try {
                        val payload = gson.fromJson(remoteJson, SefimBackupPayload::class.java)
                        val dbPrefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
                        dbPrefs.edit()
                            .putString("db_server", payload.dbServer)
                            .putString("db_name", payload.dbName)
                            .putString("db_user", payload.dbUser)
                            .putString("db_pass", payload.dbPass)
                            .putString("sefim_full_backup_json", remoteJson)
                            .apply()

                        productImageCache.putAll(payload.imageCacheByName)
                        sefimProducts.clear()
                        sefimProducts.addAll(payload.products)

                        runOnUiThread {
                            sefimAdapter.notifyDataSetChanged()
                            Toast.makeText(this@MainActivity, "Şefim ürünleri ve görselleri Menuboard'dan geri yüklendi!", Toast.LENGTH_LONG).show()
                        }
                    } catch (_: Exception) {}
                } else if (remoteJson.isEmpty() && sefimProducts.isNotEmpty()) {
                    // Menuboard sıfırlanmış -> Kumandadaki yedeği Menuboard'a gönder!
                    pushSefimBackupToMenuboard()
                } else if (remoteJson.isNotEmpty() && sefimProducts.isNotEmpty()) {
                    // Her ikisinde de veri varsa eksik görsel önbelleklerini birleştir
                    try {
                        val payload = gson.fromJson(remoteJson, SefimBackupPayload::class.java)
                        var updated = false
                        for ((k, v) in payload.imageCacheByName) {
                            if (!productImageCache.containsKey(k) && v.isNotEmpty()) {
                                productImageCache[k] = v
                                updated = true
                            }
                        }
                        if (updated) {
                            sefimProducts.forEach { prod ->
                                if (prod.imageBase64.isNullOrEmpty() && productImageCache.containsKey(prod.productName)) {
                                    prod.imageBase64 = productImageCache[prod.productName]
                                }
                            }
                            runOnUiThread { sefimAdapter.notifyDataSetChanged() }
                        }
                    } catch (_: Exception) {}
                }
            }
        })
    }

    // --- ŞEFİM POS VERİTABANI (MS SQL) VE NAMED INSTANCE MOTORU ---

    private fun showDbConfigDialog() {
        val prefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val edtServer = EditText(this).apply {
            hint = "Örn: 192.168.2.167\\YIRMIBES veya 192.168.2.167:1433"
            setText(prefs.getString("db_server", "192.168.2.167\\YIRMIBES"))
        }
        val edtDbName = EditText(this).apply {
            hint = "Veritabanı Adı (Örn: BALIKCISUKRU)"
            setText(prefs.getString("db_name", "BALIKCISUKRU"))
        }
        val edtUser = EditText(this).apply {
            hint = "Kullanıcı Adı (Örn: sa)"
            setText(prefs.getString("db_user", "sa"))
        }
        val edtPass = EditText(this).apply {
            hint = "Şifre"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("db_pass", ""))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
            addView(TextView(this@MainActivity).apply { text = "SQL Server IP (veya IP\\INSTANCE):" })
            addView(edtServer)
            addView(TextView(this@MainActivity).apply { text = "Database Adı:" })
            addView(edtDbName)
            addView(TextView(this@MainActivity).apply { text = "Kullanıcı Adı:" })
            addView(edtUser)
            addView(TextView(this@MainActivity).apply { text = "Şifre:" })
            addView(edtPass)
        }

        AlertDialog.Builder(this)
            .setTitle("Şefim MS SQL Bağlantı Ayarları")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                prefs.edit()
                    .putString("db_server", edtServer.text.toString().trim())
                    .putString("db_name", edtDbName.text.toString().trim())
                    .putString("db_user", edtUser.text.toString().trim())
                    .putString("db_pass", edtPass.text.toString())
                    .apply()
                saveLocalSefimState()
                pushSefimBackupToMenuboard()
                Toast.makeText(this, "Veritabanı bilgileri kaydedildi.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun buildJdbcConnectionUrl(rawServerInput: String, dbName: String): String {
        val normalized = rawServerInput.trim().replace("/", "\\")
        var hostPart = normalized
        var instancePart = ""

        if (normalized.contains("\\")) {
            val split = normalized.split("\\", limit = 2)
            hostPart = split[0].trim()
            instancePart = split[1].trim()
        }

        return if (instancePart.isNotEmpty()) {
            "jdbc:jtds:sqlserver://$hostPart/$dbName;instance=$instancePart"
        } else {
            val cleanHost = if (hostPart.contains(":")) hostPart else "$hostPart:1433"
            "jdbc:jtds:sqlserver://$cleanHost/$dbName"
        }
    }

    private fun fetchSefimProductsFromDb() {
        val prefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val server = prefs.getString("db_server", "") ?: ""
        val dbName = prefs.getString("db_name", "") ?: ""
        val user = prefs.getString("db_user", "") ?: ""
        val pass = prefs.getString("db_pass", "") ?: ""

        if (server.isEmpty() || dbName.isEmpty()) {
            Toast.makeText(this, "Lütfen önce ⚙️ DB Ayarları butonundan bilgileri girin!", Toast.LENGTH_LONG).show()
            showDbConfigDialog()
            return
        }

        Toast.makeText(this, "Şefim veritabanına bağlanılıyor...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver")
                DriverManager.setLoginTimeout(8)

                val primaryUrl = buildJdbcConnectionUrl(server, dbName)
                val conn = try {
                    DriverManager.getConnection(primaryUrl, user, pass)
                } catch (firstEx: Exception) {
                    if (server.contains("\\") && !server.contains(":")) {
                        val hostOnly = server.split("\\")[0].trim()
                        val fallbackUrl = "jdbc:jtds:sqlserver://$hostOnly:1433/$dbName"
                        DriverManager.getConnection(fallbackUrl, user, pass)
                    } else {
                        throw firstEx
                    }
                }

                val choice1Map = mutableMapOf<String, MutableList<String>>()
                val choice2Map = mutableMapOf<String, MutableList<String>>()
                val optionsMap = mutableMapOf<String, MutableList<String>>()

                try {
                    val st1 = conn.createStatement()
                    val rs1 = st1.executeQuery("SELECT * FROM Choice1")
                    val meta1 = rs1.metaData
                    while (rs1.next()) {
                        val pId = getColString(rs1, meta1, listOf("ProductId", "Product_Id", "UrunId", "Id"))
                        val cName = getColString(rs1, meta1, listOf("Name", "ChoiceName", "ProductName", "Header", "Description"))
                        if (pId.isNotEmpty() && cName.isNotEmpty()) {
                            choice1Map.getOrPut(pId) { mutableListOf() }.add(cName)
                        }
                    }
                    rs1.close()
                } catch (_: Exception) {}

                try {
                    val st2 = conn.createStatement()
                    val rs2 = st2.executeQuery("SELECT * FROM Choice2")
                    val meta2 = rs2.metaData
                    while (rs2.next()) {
                        val pId = getColString(rs2, meta2, listOf("ProductId", "Choice1Id", "Product_Id", "Id"))
                        val cName = getColString(rs2, meta2, listOf("Name", "ChoiceName", "ProductName", "Header", "Description"))
                        if (pId.isNotEmpty() && cName.isNotEmpty()) {
                            choice2Map.getOrPut(pId) { mutableListOf() }.add(cName)
                        }
                    }
                    rs2.close()
                } catch (_: Exception) {}

                try {
                    val stOpt = conn.createStatement()
                    val rsOpt = stOpt.executeQuery("SELECT * FROM [Option]")
                    val metaOpt = rsOpt.metaData
                    while (rsOpt.next()) {
                        val pId = getColString(rsOpt, metaOpt, listOf("ProductId", "Product_Id", "Id"))
                        val oName = getColString(rsOpt, metaOpt, listOf("Name", "OptionName", "Header", "Description"))
                        if (pId.isNotEmpty() && oName.isNotEmpty()) {
                            optionsMap.getOrPut(pId) { mutableListOf() }.add(oName)
                        }
                    }
                    rsOpt.close()
                } catch (_: Exception) {}

                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SELECT * FROM Product")
                val meta = rs.metaData
                val fetchedList = mutableListOf<SefimProductItem>()

                // Önceki seçim durumlarını koru
                val previousSelectionMap = sefimProducts.associate { it.productName to it.isSelected }

                while (rs.next()) {
                    val id = getColString(rs, meta, listOf("Id", "ProductId", "RECNO", "Kod"))
                    val name = getColString(rs, meta, listOf("ProductName", "Name", "UrunAdi", "Header", "Description"))
                    val rawPrice = getColString(rs, meta, listOf("Price", "Price1", "Fiyat", "SatisFiyati", "Amount"))

                    val inlineC1 = getColString(rs, meta, listOf("Choice1", "Choice1Name", "Secenek1"))
                    val inlineC2 = getColString(rs, meta, listOf("Choice2", "Choice2Name", "Secenek2"))
                    val inlineOpt = getColString(rs, meta, listOf("Option", "Options", "OptionName", "Opsiyon"))

                    val detailsBuilder = mutableListOf<String>()
                    if (inlineC1.isNotEmpty()) detailsBuilder.add("Seçenek 1: $inlineC1")
                    choice1Map[id]?.let { if (it.isNotEmpty()) detailsBuilder.add("Seçenek 1: ${it.joinToString(", ")}") }

                    if (inlineC2.isNotEmpty()) detailsBuilder.add("Seçenek 2: $inlineC2")
                    choice2Map[id]?.let { if (it.isNotEmpty()) detailsBuilder.add("Seçenek 2: ${it.joinToString(", ")}") }

                    if (inlineOpt.isNotEmpty()) detailsBuilder.add("Opsiyon: $inlineOpt")
                    optionsMap[id]?.let { if (it.isNotEmpty()) detailsBuilder.add("Opsiyon: ${it.joinToString(", ")}") }

                    if (name.isNotEmpty()) {
                        val formattedPrice = if (rawPrice.isNotEmpty()) "$rawPrice ₺" else ""
                        // Ürün ismine göre önbellekteki görseli otomatik eşleştir!
                        val cachedImage = productImageCache[name]
                        val keepSelected = previousSelectionMap[name] ?: true

                        fetchedList.add(
                            SefimProductItem(
                                id = id,
                                productName = name,
                                price = formattedPrice,
                                subDetails = detailsBuilder.distinct().joinToString(" | "),
                                isSelected = keepSelected,
                                imageBase64 = cachedImage
                            )
                        )
                    }
                }
                rs.close()
                conn.close()

                runOnUiThread {
                    sefimProducts.clear()
                    sefimProducts.addAll(fetchedList)
                    saveLocalSefimState()
                    pushSefimBackupToMenuboard()
                    sefimAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "${fetchedList.size} ürün getirildi (Önbellekteki resimler otomatik eşleştirildi)!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Veritabanı Bağlantı Hatası")
                        .setMessage("Hata: ${e.localizedMessage}\n\nLütfen IP\\INSTANCE, SQL Browser servisi ve TCP/IP port izinlerini kontrol edin.")
                        .setPositiveButton("Tamam", null)
                        .show()
                }
            }
        }.start()
    }

    private fun getColString(rs: java.sql.ResultSet, meta: java.sql.ResultSetMetaData, candidates: List<String>): String {
        for (colName in candidates) {
            for (i in 1..meta.columnCount) {
                if (meta.getColumnName(i).equals(colName, ignoreCase = true)) {
                    val v = rs.getString(i)?.trim() ?: ""
                    if (v.isNotEmpty() && v != "null" && v != "0" && v != "0.00") return v
                }
            }
        }
        return ""
    }

    private fun getSelectedThemeColors(): Triple<Int, Int, Int> {
        return when (spinnerSefimTheme.selectedItemPosition) {
            1 -> Triple(Color.parseColor("#FFFFFF"), Color.parseColor("#212121"), Color.parseColor("#D32F2F"))
            2 -> Triple(Color.parseColor("#1B0000"), Color.parseColor("#FFFFFF"), Color.parseColor("#FF5252"))
            3 -> Triple(Color.parseColor("#0A0E27"), Color.parseColor("#E0F7FA"), Color.parseColor("#00E5FF"))
            else -> Triple(Color.parseColor("#141414"), Color.parseColor("#FFFFFF"), Color.parseColor("#FFD700"))
        }
    }

    private fun applySefimPreviewBackground() {
        val (bgColor, _, _) = getSelectedThemeColors()
        rvSefimProducts.setBackgroundColor(bgColor)
    }

    // --- ÇOK SAYFALI (MULTI-PAGE) VE GÖRSELLİ MENÜ OLUŞTURUP YAYINLAMA ---

    private fun publishSefimMenuToMenuboard() {
        val host = activeDevice?.host
        if (host.isNullOrEmpty()) {
            Toast.makeText(this, "Lütfen önce Cihazlar sekmesinden bir Menuboard seçin!", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedItems = sefimProducts.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Yayınlanacak en az 1 ürün seçmelisiniz!", Toast.LENGTH_SHORT).show()
            return
        }

        saveLocalSefimState()
        pushSefimBackupToMenuboard()

        // Hareketli Arka Plan Objesini de Menuboard'a Bildir
        val bgMode = bgObjectValues[spinnerSefimBgObject.selectedItemPosition]
        httpClient.newCall(
            Request.Builder().url("http://$host/api/sefim/bgobject?mode=$bgMode").post("".toRequestBody(null)).build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })

        Toast.makeText(this, "Çok sayfalı menü hazırlanıyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show()

        Thread {
            val width = 1080
            val height = 1920
            val (bgColor, textColor, accentColor) = getSelectedThemeColors()
            val isBold = cbSefimBold.isChecked
            val baseFontPt = fontSizeValues[spinnerSefimFontSize.selectedItemPosition]
            val titleSizePx = baseFontPt * 2.1f
            val subSizePx = baseFontPt * 1.35f

            // Ürünleri sayfalara böl (Her sayfaya font boyutuna göre 8 ila 11 ürün sığar)
            val pages = mutableListOf<List<SefimProductItem>>()
            var currentPageItems = mutableListOf<SefimProductItem>()
            var testY = 210f

            for (item in selectedItems) {
                val hasImg = !item.imageBase64.isNullOrEmpty()
                val rowHeight = if (hasImg) {
                    145f
                } else {
                    titleSizePx + (if (item.subDetails.isNotEmpty()) subSizePx + 20f else 0f) + 52f
                }

                if (testY + rowHeight > height - 110f && currentPageItems.isNotEmpty()) {
                    pages.add(currentPageItems)
                    currentPageItems = mutableListOf()
                    testY = 210f
                }
                currentPageItems.add(item)
                testY += rowHeight
            }
            if (currentPageItems.isNotEmpty()) {
                pages.add(currentPageItems)
            }

            // Eski sefim_menu_ sayfalarını çalma listesinden temizle
            playlist.removeAll { it.fileName.startsWith("sefim_menu_") }

            val batchTime = System.currentTimeMillis()
            val totalPages = pages.size

            for ((pageIdx, pageItems) in pages.withIndex()) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(bgColor)

                val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 58f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                val paintPageBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    alpha = 180
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }

                val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    textSize = titleSizePx
                    typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                }

                val paintPrice = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = titleSizePx
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }

                val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    alpha = 215
                    textSize = subSizePx
                    typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD_ITALIC else Typeface.ITALIC)
                }

                val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    alpha = 65
                    strokeWidth = 2f
                }

                canvas.drawText("MENÜ", width / 2f, 105f, paintHeader)
                if (totalPages > 1) {
                    canvas.drawText("Sayfa ${pageIdx + 1} / $totalPages", width - 65f, 105f, paintPageBadge)
                }
                canvas.drawLine(65f, 140f, width - 65f, 140f, paintLine)

                var currentY = 200f
                for (item in pageItems) {
                    var textStartX = 65f
                    val imgB64 = item.imageBase64 ?: productImageCache[item.productName]

                    if (!imgB64.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(imgB64, Base64.DEFAULT)
                            val prodBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (prodBmp != null) {
                                val dstRect = RectF(65f, currentY - 35f, 175f, currentY + 75f)
                                canvas.drawBitmap(prodBmp, null, dstRect, null)
                                textStartX = 200f
                            }
                        } catch (_: Exception) {}
                    }

                    canvas.drawText(item.productName, textStartX, currentY + 15f, paintTitle)
                    canvas.drawText(item.price, width - 65f, currentY + 15f, paintPrice)

                    if (item.subDetails.isNotEmpty()) {
                        canvas.drawText("↳ ${item.subDetails}", textStartX + 15f, currentY + 15f + subSizePx + 14f, paintSub)
                    }

                    val stepY = if (!imgB64.isNullOrEmpty()) {
                        140f
                    } else {
                        titleSizePx + (if (item.subDetails.isNotEmpty()) subSizePx + 20f else 0f) + 48f
                    }

                    currentY += stepY - 22f
                    canvas.drawLine(65f, currentY, width - 65f, currentY, paintLine)
                    currentY += 28f
                }

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                val bytes = stream.toByteArray()
                val fileName = "sefim_menu_${batchTime}_p${pageIdx + 1}.jpg"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                    .build()

                val uploadRequest = Request.Builder()
                    .url("http://$host/api/upload?filename=$fileName")
                    .post(requestBody)
                    .build()

                try {
                    httpClient.newCall(uploadRequest).execute().use { resp ->
                        if (resp.isSuccessful) {
                            playlist.add(
                                MediaItemModel(
                                    id = "${batchTime}_${pageIdx + 1}",
                                    fileName = fileName,
                                    type = "image",
                                    durationSec = 12,
                                    waitAfterSec = 0,
                                    animation = "fade"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            runOnUiThread {
                Toast.makeText(this, "Toplam $totalPages sayfa menü oluşturuldu ve yayınlandı!", Toast.LENGTH_LONG).show()
                syncPlaylistToMenuboard()
            }
        }.start()
    }

    private fun checkAllDevicesStatus() {
        val now = System.currentTimeMillis()
        if (deviceList.isEmpty()) return

        for (dev in deviceList) {
            val req = Request.Builder().url("http://${dev.host}/api/ping").build()
            pingClient.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (now - dev.lastSeenMs > 10000L) {
                        dev.isOnline = false
                    }
                    if (activeDevice?.id == dev.id) {
                        updateConnectionUi(dev.isOnline)
                    }
                    runOnUiThread { deviceAdapter.notifyDataSetChanged() }
                }

                override fun onResponse(call: Call, response: Response) {
                    val ok = response.isSuccessful
                    response.close()
                    dev.isOnline = ok
                    if (ok) dev.lastSeenMs = System.currentTimeMillis()

                    if (activeDevice?.id == dev.id) {
                        val wasDisconnected = !isConnected
                        updateConnectionUi(ok)
                        if (ok && (wasDisconnected || playlist.isEmpty())) {
                            fetchPlaylist()
                            syncSefimBackupWithMenuboard()
                        }
                    }
                    runOnUiThread { deviceAdapter.notifyDataSetChanged() }
                }
            })
        }
    }

    private fun checkForUpdate() {
        if (GITHUB_REPO.startsWith("KULLANICI_ADINIZ")) return
        val url = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Anypay-Remote-App")
            .header("Accept", "application/vnd.github+json")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
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
                            if (name.contains("remote") && name.endsWith(".apk")) {
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
            .setMessage("Kumanda için yeni bir sürüm mevcut ($versionTitle).\nŞimdi indirip yüklemek ister misiniz?")
            .setPositiveButton("Güncelle") { _, _ ->
                downloadAndInstallApk(downloadUrl)
            }
            .setNegativeButton("Daha Sonra", null)
            .show()
    }

    private fun downloadAndInstallApk(url: String) {
        Toast.makeText(this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()
        val fileName = "remote_update_${System.currentTimeMillis()}.apk"
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Kumanda Güncellemesi")
            .setDescription("Yeni sürüm indiriliyor...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    unregisterReceiver(this)
                    val apkUri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", destination)
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(installIntent)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
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
                                updateOrAddDevice(id, host, tag, true)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun updateOrAddDevice(id: String, host: String, tag: String, online: Boolean) {
        val now = System.currentTimeMillis()
        val existing = deviceList.find { it.id == id }
        if (existing != null) {
            existing.host = host
            existing.isOnline = online
            if (online) existing.lastSeenMs = now
        } else {
            deviceList.add(DeviceItem(id, host, tag, isOnline = online, lastSeenMs = if (online) now else 0L))
        }
        saveDevices()
        deviceAdapter.notifyDataSetChanged()

        if (activeDevice?.id == id) {
            activeDevice?.host = host
            activeDevice?.isOnline = online
            txtActiveDeviceName.text = "Aktif: ${activeDevice?.nameTag} ($host)"
            updateConnectionUi(online)
        }
    }

    private fun switchTab(tabIndex: Int) {
        panelTabDevices.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        panelTabMedia.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        panelTabSefim.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE

        tabDevices.setTextColor(if (tabIndex == 0) 0xFF00E676.toInt() else 0xFF888888.toInt())
        tabMedia.setTextColor(if (tabIndex == 1) 0xFF00E676.toInt() else 0xFF888888.toInt())
        tabSefim.setTextColor(if (tabIndex == 2) 0xFF00E676.toInt() else 0xFF888888.toInt())
    }

    private fun selectDevice(device: DeviceItem) {
        if (isSharingScreen) stopScreenShare()
        activeDevice = device
        txtActiveDeviceName.text = "Aktif: ${device.nameTag} (${device.host})"

        playlist.clear()
        mediaAdapter.notifyDataSetChanged()

        saveDevices()
        deviceAdapter.notifyDataSetChanged()
        checkAllDevicesStatus()
        syncSefimBackupWithMenuboard()
    }

    private fun loadSavedDevices() {
        val prefs = getSharedPreferences("remote_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("device_list_json", null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<DeviceItem>>() {}.type
            deviceList = gson.fromJson(json, type)
            deviceList.forEach { it.isOnline = false }
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

    private fun updateConnectionUi(connected: Boolean) {
        isConnected = connected
        runOnUiThread {
            if (connected) {
                txtConnectionStatus.text = "Bağlı"
                txtConnectionStatus.setTextColor(0xFF2E7D32.toInt())
            } else {
                txtConnectionStatus.text = "Bağlı Değil"
                txtConnectionStatus.setTextColor(0xFFD32F2F.toInt())
            }
        }
    }

    private fun fetchPlaylist() {
        val host = activeDevice?.host ?: return
        val request = Request.Builder().url("http://$host/api/playlist").build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

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

        val totalLength = try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) { -1L }

        uploadProgressBar.visibility = View.VISIBLE
        uploadProgressBar.progress = 0

        val customRequestBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
            override fun contentLength(): Long = totalLength

            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var uploaded: Long = 0
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        if (totalLength > 0) {
                            uploaded += read
                            val progress = ((uploaded * 100) / totalLength).toInt()
                            runOnUiThread { uploadProgressBar.progress = progress }
                        }
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
                runOnUiThread {
                    uploadProgressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Yükleme Başarısız!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { uploadProgressBar.visibility = View.GONE }
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

    inner class SefimProductAdapter : RecyclerView.Adapter<SefimProductAdapter.SefimViewHolder>() {
        inner class SefimViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val cbInclude: CheckBox = v.findViewById(R.id.cbProductInclude)
            val imgPhoto: ImageView = v.findViewById(R.id.imgProductPhoto)
            val btnPickPhoto: ImageButton = v.findViewById(R.id.btnPickProductPhoto)
            val txtName: TextView = v.findViewById(R.id.txtProductName)
            val txtChoices: TextView = v.findViewById(R.id.txtProductChoices)
            val txtPrice: TextView = v.findViewById(R.id.txtProductPrice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SefimViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sefim_product, parent, false)
            return SefimViewHolder(v)
        }

        override fun onBindViewHolder(holder: SefimViewHolder, position: Int) {
            val item = sefimProducts[position]
            val (_, textColor, accentColor) = getSelectedThemeColors()
            val fontSize = fontSizeValues[spinnerSefimFontSize.selectedItemPosition]
            val style = if (cbSefimBold.isChecked) Typeface.BOLD else Typeface.NORMAL

            holder.cbInclude.setOnCheckedChangeListener(null)
            holder.cbInclude.isChecked = item.isSelected
            holder.cbInclude.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                saveLocalSefimState()
            }

            // Ürün Görseli Önbellekte Varsa Göster, Yoksa Kamera İkonu Göster
            val cachedB64 = item.imageBase64 ?: productImageCache[item.productName]
            if (!cachedB64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(cachedB64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.imgPhoto.setImageBitmap(bmp)
                    holder.btnPickPhoto.alpha = 0.15f
                } catch (_: Exception) {
                    holder.imgPhoto.setImageDrawable(null)
                    holder.btnPickPhoto.alpha = 0.85f
                }
            } else {
                holder.imgPhoto.setImageDrawable(null)
                holder.btnPickPhoto.alpha = 0.85f
            }

            holder.btnPickPhoto.setOnClickListener {
                if (!cachedB64.isNullOrEmpty()) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(item.productName)
                        .setItems(arrayOf("📷 Yeni Görsel Seç", "🗑️ Mevcut Görseli Kaldır")) { _, which ->
                            if (which == 0) {
                                pendingPhotoProductName = item.productName
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "image/*"
                                }
                                pickProductPhotoLauncher.launch(intent)
                            } else {
                                productImageCache.remove(item.productName)
                                item.imageBase64 = null
                                saveLocalSefimState()
                                pushSefimBackupToMenuboard()
                                notifyDataSetChanged()
                            }
                        }
                        .show()
                } else {
                    pendingPhotoProductName = item.productName
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                    pickProductPhotoLauncher.launch(intent)
                }
            }

            holder.txtName.text = item.productName
            holder.txtName.setTextColor(textColor)
            holder.txtName.textSize = fontSize
            holder.txtName.setTypeface(null, style)

            holder.txtPrice.text = item.price
            holder.txtPrice.setTextColor(accentColor)
            holder.txtPrice.textSize = fontSize
            holder.txtPrice.setTypeface(null, Typeface.BOLD)

            if (item.subDetails.isNotEmpty()) {
                holder.txtChoices.visibility = View.VISIBLE
                holder.txtChoices.text = "↳ ${item.subDetails}"
                holder.txtChoices.setTextColor(accentColor)
                holder.txtChoices.textSize = (fontSize - 4f).coerceAtLeast(11f)
            } else {
                holder.txtChoices.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = sefimProducts.size
    }

    inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
        inner class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val viewStatusDot: View = v.findViewById(R.id.viewStatusDot)
            val txtDeviceTag: TextView = v.findViewById(R.id.txtDeviceTag)
            val txtDeviceIp: TextView = v.findViewById(R.id.txtDeviceIp)
            val btnEditTag: ImageButton = v.findViewById(R.id.btnEditTag)
            val btnFindDevice: Button = v.findViewById(R.id.btnFindDevice)
            val btnDeviceSettings: ImageButton = v.findViewById(R.id.btnDeviceSettings)
            val btnDeleteDevice: ImageButton = v.findViewById(R.id.btnDeleteDevice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            return DeviceViewHolder(v)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val dev = deviceList[position]
            val activePrefix = if (activeDevice?.id == dev.id) "✓ " else ""
            holder.txtDeviceTag.text = "$activePrefix${dev.nameTag}"
            holder.txtDeviceIp.text = dev.host

            if (dev.isOnline) {
                holder.viewStatusDot.setBackgroundColor(0xFF00C853.toInt())
            } else {
                holder.viewStatusDot.setBackgroundColor(0xFFD32F2F.toInt())
            }

            if (dev.isFinding) {
                holder.btnFindDevice.text = "Gizle"
                holder.btnFindDevice.setBackgroundColor(0xFFD32F2F.toInt())
            } else {
                holder.btnFindDevice.text = "Bul"
                holder.btnFindDevice.setBackgroundColor(0xFFF57C00.toInt())
            }

            holder.itemView.setOnClickListener {
                selectDevice(dev)
                switchTab(1)
            }

            holder.btnEditTag.setOnClickListener { showEditTagDialog(dev) }
            holder.btnFindDevice.setOnClickListener { toggleFindDevice(dev) }
            holder.btnDeviceSettings.setOnClickListener { showDeviceAdvancedMenu(dev) }

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
