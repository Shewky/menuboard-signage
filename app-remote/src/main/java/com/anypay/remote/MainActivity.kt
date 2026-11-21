package com.anypay.remote

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
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
import kotlin.math.hypot

data class DeviceItem(
    val id: String,
    var host: String,
    var nameTag: String,
    var isFinding: Boolean = false,
    var isOnline: Boolean = false,
    var lastSeenMs: Long = 0L,
    var version: String? = "",
    var orientation: String? = "portrait",
    var blurBg: Boolean = false,
    var blurDarkness: Int? = 30,
    var blurIntensity: Int? = 1,
    var blurQuality: Int? = 1,
    var blurApplyIg: Boolean? = false,
    var immersiveMode: Boolean? = true
)

data class MediaItemModel(
    val id: String = "",
    var fileName: String = "",
    val type: String = "image",
    var durationSec: Int = 10,
    var waitAfterSec: Int = 0,
    var animation: String = "fade",
    var fileDurationSec: Int = 0,
    var autoScroll: Boolean = false,
    var scrollUntilEnd: Boolean = false,
    var webScrollSpeed: String? = "medium",
    var showQr: Boolean = false,
    var qrCorner: String? = "bottom_right",
    var qrSizeDp: Int = 150,
    var qrBorderDp: Int = 8,
    var webZoomPercent: Int = 100,
    var webFontFamily: String? = "default",
    var webFontSizePercent: Int = 0,
    var webBoldHeaders: Boolean = false,
    var webImageScalePercent: Int = 100,
    var igMediaFilter: String? = "all",
    var igSlideSec: Int = 8,
    var igAudioEnabled: Boolean = false,
    var isHidden: Boolean = false
)

data class SefimProductItem(
    val id: String? = "",
    val productName: String? = "",
    val price: String? = "",
    val choice1Text: String? = "",
    val choice2Text: String? = "",
    val optionsText: String? = "",
    val subDetails: String? = "",
    var recipeContent: String? = "",
    var manualAllergen: String? = "",
    var isSelected: Boolean = true,
    var imageBase64: String? = null
)

data class ProductMetaCache(
    var imageBase64: String? = null,
    var recipeContent: String? = "",
    var manualAllergen: String? = ""
)

data class SefimBackupPayload(
    val dbServer: String? = "",
    val dbName: String? = "",
    val dbUser: String? = "",
    val dbPass: String? = "",
    val restaurantName: String? = "BALIKÇI ŞÜKRÜ",
    val headerNote: String? = "",
    val footerNote: String? = "Afiyet Olsun • KDV Dahildir",
    val websiteUrl: String? = "",
    val pageDurationSec: Int = 12,
    val showChoice1: Boolean = true,
    val showChoice2: Boolean = true,
    val showOptions: Boolean = true,
    val themeIndex: Int = 0,
    val fontSizeIndex: Int = 1,
    val isBold: Boolean = true,
    val bgObjectMode: String? = "none",
    val products: List<SefimProductItem>? = emptyList(),
    val metaCacheByName: Map<String, ProductMetaCache>? = emptyMap()
)

data class RemoteAppInfo(
    val appName: String,
    val packageName: String
)

class MainActivity : AppCompatActivity() {

    private val GITHUB_REPO = "KULLANICI_ADINIZ/menuboard-signage"

    private lateinit var tabDevices: Button
    private lateinit var tabMedia: Button
    private var tabSefim: Button? = null
    private lateinit var panelTabDevices: View
    private lateinit var panelTabMedia: View
    private var panelTabSefimWrapper: FrameLayout? = null
    private lateinit var sefimScrollView: NestedScrollView

    private var isSefimUnlocked = false
    private var sefimTapCount = 0
    private var lastSefimTapMs = 0L

    private lateinit var txtActiveDeviceName: TextView
    private lateinit var txtConnectionStatus: TextView

    private lateinit var btnScanNetwork: Button
    private lateinit var btnScanQr: Button
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private var deviceList = mutableListOf<DeviceItem>()
    private var activeDevice: DeviceItem? = null

    private lateinit var btnToggleScreenShare: Button
    private var uploadProgressBar: ProgressBar? = null
    private lateinit var edtDuration: EditText
    private lateinit var edtWaitAfter: EditText
    private lateinit var spinnerAnim: Spinner
    private lateinit var btnPickMedia: Button
    private lateinit var recyclerViewPlaylist: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private var playlist = mutableListOf<MediaItemModel>()

    private var activeUrlInputField: EditText? = null

    private lateinit var edtRestaurantName: EditText
    private lateinit var edtMenuPageDuration: EditText
    private lateinit var edtHeaderNote: EditText
    private lateinit var edtFooterNote: EditText
    private lateinit var edtWebsiteLink: EditText
    private lateinit var edtProductSearch: EditText
    private lateinit var cbShowChoice1: CheckBox
    private lateinit var cbShowChoice2: CheckBox
    private lateinit var cbShowOptions: CheckBox
    private lateinit var spinnerSefimTheme: Spinner
    private lateinit var spinnerSefimFontSize: Spinner
    private lateinit var spinnerSefimBgObject: Spinner
    private lateinit var cbSefimBold: CheckBox
    private lateinit var rvSefimProducts: RecyclerView
    private lateinit var sefimAdapter: SefimProductAdapter

    private var sefimProducts = mutableListOf<SefimProductItem>()
    private var filteredSefimProducts = mutableListOf<SefimProductItem>()
    private var currentSearchQuery = ""

    private var productMetaCache = mutableMapOf<String, ProductMetaCache>()
    private var pendingPhotoProductName: String? = null
    private var targetApkDevice: DeviceItem? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
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

    private val animOptions = arrayOf("Solma (Fade)", "Soldan Kay", "Sağdan Kay", "Yukarıdan Kay", "Aşağıdan Kay", "Yakınlaş (Zoom In)", "Uzaklaş (Zoom Out)", "Animasyonsuz")
    private val animValues = arrayOf("fade", "slide_left", "slide_right", "slide_up", "slide_down", "zoom_in", "zoom_out", "none")

    private val scrollSpeedLabels = arrayOf("Yavaş", "Yavaş - Orta", "Orta", "Orta - Hızlı", "Hızlı")
    private val scrollSpeedValues = arrayOf("slow", "slow_medium", "medium", "medium_fast", "fast")

    private val qrCornerLabels = arrayOf("Sağ Alt Köşe", "Sol Alt Köşe", "Sağ Üst Köşe", "Sol Üst Köşe")
    private val qrCornerValues = arrayOf("bottom_right", "bottom_left", "top_right", "top_left")

    private val qrSizeLabels = arrayOf("Çok Küçük (75dp)", "Küçük (110dp)", "Orta (150dp)", "Büyük (190dp)", "Dev (240dp)")
    private val qrSizeValues = arrayOf(75, 110, 150, 190, 240)

    private val qrBorderLabels = arrayOf("Çerçevesiz (0dp)", "İnce (4dp)", "Orta (8dp)", "Kalın (14dp)", "Çok Kalın (22dp)")
    private val qrBorderValues = arrayOf(0, 4, 8, 14, 22)

    private val webZoomLabels = arrayOf("%30", "%40", "%50", "%60", "%70", "%75", "%80", "%90", "%100 (Normal)", "%110", "%125", "%140", "%150", "%175", "%200", "%225", "%250")
    private val webZoomValues = arrayOf(30, 40, 50, 60, 70, 75, 80, 90, 100, 110, 125, 140, 150, 175, 200, 225, 250)

    private val webFontFamilyLabels = arrayOf(
        "Site Varsayılanı",
        "Roboto (Modern)",
        "Open Sans (Ferah)",
        "Montserrat (Şık)",
        "Poppins (Yuvarlak)",
        "Lato (Kurumsal)",
        "Oswald (Dar Başlık)",
        "Playfair Display (Lüks)",
        "Merriweather (Klasik)",
        "Sistem Sans-Serif",
        "Sistem Serif",
        "Daktilo (Monospace)",
        "El Yazısı (Cursive)"
    )
    private val webFontFamilyValues = arrayOf(
        "default",
        "Roboto",
        "Open Sans",
        "Montserrat",
        "Poppins",
        "Lato",
        "Oswald",
        "Playfair Display",
        "Merriweather",
        "sans-serif",
        "serif",
        "monospace",
        "cursive"
    )

    private val webFontSizeLabels = arrayOf("Site Varsayılanı", "%85 (Küçük)", "%100 (Normal)", "%115 (Orta)", "%130 (Büyük)", "%150 (Çok Büyük)", "%175 (Dev)")
    private val webFontSizeValues = arrayOf(0, 85, 100, 115, 130, 150, 175)

    private val webImageScaleLabels = arrayOf("%50 (Çok Küçük)", "%75 (Küçük)", "%100 (Normal)", "%125 (Büyük)", "%150 (Çok Büyük)", "%175", "%200 (Dev)")
    private val webImageScaleValues = arrayOf(50, 75, 100, 125, 150, 175, 200)

    private val themeNames = arrayOf(
        "🖤 Koyu Lüks (Gold & Siyah)", "🤍 Modern Beyaz (Ferah)", "🔥 Restoran Ateşi (Kırmızı & Kömür)",
        "🌌 Neon Cyberpunk (Gece & Cyan)", "🌊 Okyanus Derinliği (Balık & Deniz)", "🌿 Zümrüt Bahçesi (Doğal & Vegan)",
        "👑 Osmanlı Kehribar (Bordo & Altın)", "🧁 Paris Pastanesi (Tatlı & Cafe)", "🍕 İtalyan Trattoria (Sıcak Terracotta)", "💎 Minimalist Slate (Modern Gri)"
    )

    private val fontSizes = arrayOf("Küçük (14pt)", "Orta (18pt)", "Büyük (22pt)", "Dev (26pt)")
    private val fontSizeValues = arrayOf(14f, 18f, 22f, 26f)

    private val bgObjectNames = arrayOf(
        "Yok (Sade Arka Plan)", "🐟 Deniz Akvaryumu (Balık, Ahtapot, Yengeç)", "🥙 Gerçekçi Döner & Kor Ateş Buharı",
        "💨 Gerçekçi Nargile & Duman Halkaları", "☕ Sıcak Kahve Buharı & Çekirdekleri", "✨ Altın Işıltı (Luxury Bokeh)", "❄️ Kış Kar Yağışı"
    )
    private val bgObjectValues = arrayOf("none", "fish", "doner", "hookah", "coffee", "gold", "snow")

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
                val scannedDevice = updateOrAddDevice(id, host, tag, true)
                selectDevice(scannedDevice)
                switchTab(1)
                Toast.makeText(this, "${scannedDevice.nameTag} ($host) seçildi!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Geçersiz QR Kod!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val urlQrLauncher = registerForActivityResult(ScanContract()) { result ->
        val scanned = result.contents?.trim()
        if (!scanned.isNullOrEmpty()) {
            activeUrlInputField?.setText(scanned)
            activeUrlInputField?.setSelection(scanned.length)
            Toast.makeText(this, "URL QR koddan okundu!", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null) uploadMediaFile(res.data!!.data!!)
    }

    private val pickProductPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val prodName = pendingPhotoProductName
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null && !prodName.isNullOrEmpty()) {
            try {
                contentResolver.openInputStream(res.data!!.data!!)?.use { input ->
                    val rawBmp = BitmapFactory.decodeStream(input)
                    if (rawBmp != null) {
                        val scaled = Bitmap.createScaledBitmap(rawBmp, 220, 220, true)
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        productMetaCache.getOrPut(prodName) { ProductMetaCache() }.imageBase64 = b64
                        sefimProducts.filter { (it.productName ?: "").equals(prodName, ignoreCase = true) }.forEach { it.imageBase64 = b64 }
                        saveLocalSefimState(); pushSefimBackupToMenuboard(); filterSefimProductList(currentSearchQuery)
                        Toast.makeText(this, "'$prodName' görseli kaydedildi.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) { Toast.makeText(this, "Görsel işlenemedi!", Toast.LENGTH_SHORT).show() }
        }
    }

    private val pickRemoteApkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val target = targetApkDevice
        if (res.resultCode == Activity.RESULT_OK && res.data?.data != null && target != null) uploadAndInstallApkOnMenuboard(target, res.data!!.data!!)
    }

    private val screenShareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val host = activeDevice?.host ?: return@registerForActivityResult
            httpClient.newCall(Request.Builder().url("http://$host/api/screen/start").post("".toRequestBody(null)).build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("result_code", res.resultCode); putExtra("result_data", res.data); putExtra("target_host", host)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            isSharingScreen = true; btnToggleScreenShare.text = "⏹ Paylaşımı Durdur"; btnToggleScreenShare.setBackgroundColor(0xFFD32F2F.toInt())
            moveTaskToBack(true)
        } else { Toast.makeText(this, "Ekran paylaşımına izin verilmedi", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabDevices = findViewById(R.id.tabDevices); tabMedia = findViewById(R.id.tabMedia)
        panelTabDevices = findViewById(R.id.panelTabDevices); panelTabMedia = findViewById(R.id.panelTabMedia)
        txtActiveDeviceName = findViewById(R.id.txtActiveDeviceName); txtConnectionStatus = findViewById(R.id.txtConnectionStatus)
        btnScanNetwork = findViewById(R.id.btnScanNetwork); btnScanQr = findViewById(R.id.btnScanQr); rvDevices = findViewById(R.id.rvDevices)
        btnToggleScreenShare = findViewById(R.id.btnToggleScreenShare); edtDuration = findViewById(R.id.edtDuration)
        edtWaitAfter = findViewById(R.id.edtWaitAfter); spinnerAnim = findViewById(R.id.spinnerAnim)
        btnPickMedia = findViewById(R.id.btnPickMedia); recyclerViewPlaylist = findViewById(R.id.recyclerViewPlaylist)

        val resIdProg = resources.getIdentifier("uploadProgressBar", "id", packageName)
        if (resIdProg != 0) uploadProgressBar = findViewById(resIdProg)
        val vName = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { "1.40" }
        supportActionBar?.title = "Menuboard Kumanda"
        val resIdVer = resources.getIdentifier("txtRemoteVersion", "id", packageName)
        if (resIdVer != 0) findViewById<TextView>(resIdVer)?.text = "v$vName"

        injectAddUrlButton()
        setupDynamicSefimTabAndControls()
        spinnerAnim.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, animOptions)

        loadSavedDevices(); loadLocalSefimState()
        deviceAdapter = DeviceAdapter(); rvDevices.layoutManager = LinearLayoutManager(this); rvDevices.adapter = deviceAdapter
        mediaAdapter = MediaAdapter(); recyclerViewPlaylist.layoutManager = LinearLayoutManager(this); recyclerViewPlaylist.adapter = mediaAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                Collections.swap(playlist, vh.adapterPosition, target.adapterPosition); mediaAdapter.notifyItemMoved(vh.adapterPosition, target.adapterPosition); return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) { super.clearView(recyclerView, viewHolder); syncPlaylistToMenuboard() }
        }).attachToRecyclerView(recyclerViewPlaylist)

        tabDevices.setOnClickListener { switchTab(0) }
        tabMedia.setOnClickListener { switchTab(1) }
        tabSefim?.setOnClickListener { handleLockedSefimTabClick() }

        btnScanQr.setOnClickListener { qrLauncher.launch(ScanOptions().apply { setPrompt("Menuboard üzerindeki QR kodu taratın"); setBeepEnabled(true); setOrientationLocked(false) }) }
        btnScanNetwork.setOnClickListener { Toast.makeText(this, "Ağdaki tüm cihazların durumu kontrol ediliyor...", Toast.LENGTH_SHORT).show(); checkAllDevicesStatus() }
        btnToggleScreenShare.setOnClickListener {
            if (activeDevice == null) { Toast.makeText(this, "Önce bir Menuboard seçin!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (!isSharingScreen) screenShareLauncher.launch((getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent())
            else stopScreenShare()
        }
        btnPickMedia.setOnClickListener {
            if (activeDevice == null) { Toast.makeText(this, "Lütfen önce bir Menuboard seçin!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            pickMediaLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) })
        }

        findViewById<ImageView?>(R.id.imgFooterLogo)?.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.interfon.com.tr"))) }
        val resIdSupport = resources.getIdentifier("btnSupportCall", "id", packageName)
        if (resIdSupport != 0) findViewById<View>(resIdSupport)?.setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:03243292001"))) }

        if (deviceList.isNotEmpty() && activeDevice == null) selectDevice(deviceList.first())
        startUdpListener(); checkForUpdate()
    }

    private fun handleLockedSefimTabClick() {
        if (isSefimUnlocked) { switchTab(2); return }
        val now = System.currentTimeMillis()
        if (now - lastSefimTapMs > 3000L) sefimTapCount = 0
        sefimTapCount++; lastSefimTapMs = now
        if (sefimTapCount >= 5) { sefimTapCount = 0; showSefimPinDialog() }
        else Toast.makeText(this, "Geliştirme aşamasında", Toast.LENGTH_SHORT).show()
    }

    private fun showSefimPinDialog() {
        val edtPin = EditText(this).apply { hint = "PIN Kodu"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; gravity = Gravity.CENTER; textSize = 18f }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60, 30, 60, 10); addView(edtPin) }
        AlertDialog.Builder(this).setTitle("🔒 Geliştirici Erişimi").setMessage("Devam etmek için PIN kodunu giriniz:").setView(container)
            .setPositiveButton("Giriş") { _, _ ->
                if (edtPin.text.toString().trim() == "2228") { isSefimUnlocked = true; tabSefim?.text = "🍽️ 🔓"; switchTab(2); Toast.makeText(this, "Erişim açıldı!", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(this, "Hatalı PIN!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("İptal", null).show()
    }

    private fun injectAddUrlButton() {
        val parentLayout = btnPickMedia.parent as? LinearLayout ?: return
        val pickIdx = parentLayout.indexOfChild(btnPickMedia)

        val btnAddUrl = Button(this).apply {
            text = "🌐 URL EKLE"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#00897B"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 }
            setOnClickListener {
                if (activeDevice == null) { Toast.makeText(this@MainActivity, "Lütfen önce bir Menuboard seçin!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                showUrlConfigDialog(existingItem = null)
            }
        }
        parentLayout.addView(btnAddUrl, (pickIdx + 1).coerceAtMost(parentLayout.childCount))

        val btnAddInstagram = Button(this).apply {
            text = "📸 INSTAGRAM EKLE"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#E1306C"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 }
            setOnClickListener {
                if (activeDevice == null) { Toast.makeText(this@MainActivity, "Lütfen önce bir Menuboard seçin!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                checkInstagramStatusAndOpenDialog(existingItem = null)
            }
        }
        parentLayout.addView(btnAddInstagram, (pickIdx + 2).coerceAtMost(parentLayout.childCount))
    }

    private fun checkInstagramStatusAndOpenDialog(existingItem: MediaItemModel?) {
        val host = activeDevice?.host ?: return
        httpClient.newCall(Request.Builder().url("http://$host/api/instagram/status").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { showInstagramConfigDialog(existingItem, false) } }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"; response.close()
                val active = try { JSONObject(body).optBoolean("active", false) } catch (_: Exception) { false }
                runOnUiThread { showInstagramConfigDialog(existingItem, active) }
            }
        })
    }

    private fun showInstagramConfigDialog(existingItem: MediaItemModel?, initialSessionActive: Boolean) {
        val isEdit = existingItem != null
        val igFilterLabels = arrayOf("Tümü (Fotoğraf + Video)", "Sadece Fotoğraflar", "Sadece Videolar")
        val igFilterValues = arrayOf("all", "photo", "video")

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(45, 30, 45, 15) }
        scrollView.addView(layout)

        val txtSessionStatus = TextView(this).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, 12) }
        val edtUsername = EditText(this).apply { hint = "Instagram Kullanıcı Adı (Örn: ornekrestoran)"; setSingleLine(true); setText(existingItem?.fileName ?: "") }
        val spFilter = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, igFilterLabels)
            val idx = igFilterValues.indexOf(existingItem?.igMediaFilter ?: "all"); if (idx >= 0) setSelection(idx)
        }
        val cbUntilEnd = CheckBox(this).apply { text = "Tüm medyalar bitene kadar oynatmaya devam et"; isChecked = existingItem?.scrollUntilEnd ?: true }
        val cbAudio = CheckBox(this).apply { text = "🔊 Videoları Sesli Oynat"; isChecked = existingItem?.igAudioEnabled ?: false }
        val edtDur = EditText(this).apply { hint = "Oynatma Süresi (sn)"; inputType = InputType.TYPE_CLASS_NUMBER; setText((existingItem?.durationSec ?: 60).toString()) }
        val edtSlideSec = EditText(this).apply { hint = "Fotoğraf Başına Süre (sn)"; inputType = InputType.TYPE_CLASS_NUMBER; setText((existingItem?.igSlideSec ?: 8).toString()) }
        val spAnim = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, animOptions)
            val idx = animValues.indexOf(existingItem?.animation ?: "fade"); if (idx >= 0) setSelection(idx)
        }

        val formContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply { text = "Instagram Kullanıcı Adı:"; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 12, 0, 0) })
            addView(edtUsername)
            addView(TextView(this@MainActivity).apply { text = "Medya Filtresi:"; setPadding(0, 10, 0, 0) })
            addView(spFilter)
            addView(cbUntilEnd)
            addView(cbAudio)
            addView(TextView(this@MainActivity).apply { text = "Şu Kadar Saniye Oynat (Yukarıdaki seçili değilse):"; setPadding(0, 8, 0, 0) })
            addView(edtDur)
            addView(TextView(this@MainActivity).apply { text = "Fotoğraf Başına Bekleme Süresi (sn):"; setPadding(0, 8, 0, 0) })
            addView(edtSlideSec)
            addView(TextView(this@MainActivity).apply { text = "Giriş Animasyonu:"; setPadding(0, 8, 0, 0) })
            addView(spAnim)
        }

        fun updateSessionUi(active: Boolean) {
            if (active) {
                txtSessionStatus.text = "🟢 Menuboard Instagram Oturumu: AKTİF"
                txtSessionStatus.setTextColor(Color.parseColor("#2E7D32"))
                formContainer.visibility = View.VISIBLE
            } else {
                txtSessionStatus.text = "🔴 Menuboard Instagram Oturumu: YOK (Önce oturum açın)"
                txtSessionStatus.setTextColor(Color.parseColor("#D32F2F"))
                formContainer.visibility = View.GONE
            }
        }
        updateSessionUi(initialSessionActive)

        val btnLoginIg = Button(this).apply {
            text = if (initialSessionActive) "🔄 Instagram Oturumunu Yenile" else "🔑 Instagram Oturumu Aç"
            isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#E1306C"))
            setOnClickListener {
                openInstagramLoginAndSyncCookie { success ->
                    if (success) { text = "🔄 Instagram Oturumunu Yenile"; updateSessionUi(true) }
                }
            }
        }

        layout.addView(txtSessionStatus); layout.addView(btnLoginIg); layout.addView(formContainer)

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "📸 Instagram Ayarlarını Düzenle" else "📸 Instagram Ekle")
            .setView(scrollView)
            .setPositiveButton(if (isEdit) "Kaydet" else "Ekle ve Yayınla") { _, _ ->
                val user = edtUsername.text.toString().trim()
                if (user.isEmpty()) { Toast.makeText(this, "Lütfen kullanıcı adı girin!", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val dur = edtDur.text.toString().toIntOrNull() ?: 60
                val slideSec = edtSlideSec.text.toString().toIntOrNull() ?: 8
                val filter = igFilterValues[spFilter.selectedItemPosition]
                val untilEnd = cbUntilEnd.isChecked
                val audioOn = cbAudio.isChecked
                val anim = animValues[spAnim.selectedItemPosition]

                if (isEdit && existingItem != null) {
                    existingItem.fileName = user; existingItem.durationSec = dur; existingItem.scrollUntilEnd = untilEnd
                    existingItem.igMediaFilter = filter; existingItem.igSlideSec = slideSec; existingItem.animation = anim
                    existingItem.igAudioEnabled = audioOn
                } else {
                    playlist.add(MediaItemModel(id = System.currentTimeMillis().toString(), fileName = user, type = "instagram", durationSec = dur, waitAfterSec = 0, animation = anim, scrollUntilEnd = untilEnd, igMediaFilter = filter, igSlideSec = slideSec, igAudioEnabled = audioOn))
                }
                syncPlaylistToMenuboard()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun openInstagramLoginAndSyncCookie(onResult: (Boolean) -> Unit) {
        val host = activeDevice?.host ?: return
        var loginDialog: AlertDialog? = null
        var synced = false

        val webView = WebView(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(this, true)

            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                    if (!v.hasFocus()) v.requestFocus()
                }
                false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (synced) return
                    val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com") ?: ""
                    if (cookies.contains("sessionid=")) {
                        synced = true
                        val payload = JSONObject().apply {
                            put("cookie", cookies)
                            put("userAgent", settings.userAgentString ?: "")
                        }.toString()
                        val req = Request.Builder().url("http://$host/api/instagram/cookie").post(payload.toRequestBody("application/json".toMediaTypeOrNull())).build()
                        httpClient.newCall(req).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                runOnUiThread { Toast.makeText(this@MainActivity, "Oturum Menuboard'a aktarılamadı!", Toast.LENGTH_LONG).show(); onResult(false) }
                            }
                            override fun onResponse(call: Call, response: Response) {
                                val ok = response.isSuccessful; response.close()
                                runOnUiThread {
                                    loginDialog?.dismiss()
                                    Toast.makeText(this@MainActivity, "Instagram oturumu Menuboard'a başarıyla aktarıldı!", Toast.LENGTH_LONG).show()
                                    onResult(ok)
                                }
                            }
                        })
                    }
                }
            }
            loadUrl("https://www.instagram.com/accounts/login/")
        }

        val container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1200)
            addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1200))
        }

        loginDialog = AlertDialog.Builder(this).setTitle("🔑 Instagram Giriş").setView(container).setNegativeButton("Kapat", null).create()
        loginDialog.setOnShowListener {
            loginDialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            loginDialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            webView.requestFocus()
        }
        loginDialog.show()
    }

    private fun showUrlConfigDialog(existingItem: MediaItemModel?) {
        val isEdit = existingItem != null
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(45, 30, 45, 15) }
        scrollView.addView(layout)

        val urlRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val edtUrl = EditText(this).apply {
            hint = "Örn: https://www.ornekmenu.com"; inputType = InputType.TYPE_TEXT_VARIATION_URI; setSingleLine(true)
            setText(existingItem?.fileName ?: ""); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        activeUrlInputField = edtUrl

        val btnScanUrlQr = Button(this).apply {
            text = "📷 QR"; textSize = 11f; isAllCaps = false; setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0288D1")); setPadding(16, 10, 16, 10)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = 8 }
            setOnClickListener { urlQrLauncher.launch(ScanOptions().apply { setPrompt("URL içeren QR kodu kameraya gösterin"); setBeepEnabled(true); setOrientationLocked(false) }) }
        }
        urlRow.addView(edtUrl); urlRow.addView(btnScanUrlQr)

        val edtDur = EditText(this).apply { hint = "Ekranda Kalma Süresi (sn)"; inputType = InputType.TYPE_CLASS_NUMBER; setText((existingItem?.durationSec ?: 20).toString()) }
        val edtWait = EditText(this).apply { hint = "Sonraki Medyaya Geçmeden Bekleme (sn)"; inputType = InputType.TYPE_CLASS_NUMBER; setText((existingItem?.waitAfterSec ?: 0).toString()) }
        val spAnim = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, animOptions); val idx = animValues.indexOf(existingItem?.animation ?: "fade"); if (idx >= 0) setSelection(idx) }
        val cbAutoScroll = CheckBox(this).apply { text = "Otomatik Aşağı Kaydır"; isChecked = existingItem?.autoScroll ?: true }
        val cbScrollEnd = CheckBox(this).apply { text = "Sayfa Bitimine Kadar Aşağı Kaydır"; isChecked = existingItem?.scrollUntilEnd ?: false; setOnCheckedChangeListener { _, checked -> if (checked) cbAutoScroll.isChecked = true } }
        val spScrollSpeed = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, scrollSpeedLabels); val idx = scrollSpeedValues.indexOf(existingItem?.webScrollSpeed ?: "medium"); setSelection(if (idx >= 0) idx else 2) }
        val spZoom = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, webZoomLabels); val idx = webZoomValues.indexOf(existingItem?.webZoomPercent ?: 100); setSelection(if (idx >= 0) idx else 8) }
        val spFontFamily = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, webFontFamilyLabels); val idx = webFontFamilyValues.indexOf(existingItem?.webFontFamily ?: "default"); setSelection(if (idx >= 0) idx else 0) }
        val spFontSize = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, webFontSizeLabels); val idx = webFontSizeValues.indexOf(existingItem?.webFontSizePercent ?: 0); setSelection(if (idx >= 0) idx else 0) }
        val spImageScale = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, webImageScaleLabels); val idx = webImageScaleValues.indexOf(existingItem?.webImageScalePercent ?: 100); setSelection(if (idx >= 0) idx else 2) }
        val cbBoldHeaders = CheckBox(this).apply { text = "Sayfadaki Başlıkları Kalın (Bold) Yap"; isChecked = existingItem?.webBoldHeaders ?: false }
        val cbShowQr = CheckBox(this).apply { text = "Sayfa Linkini QR Kod Olarak Göster"; isChecked = existingItem?.showQr ?: false }
        val spQrCorner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, qrCornerLabels); val idx = qrCornerValues.indexOf(existingItem?.qrCorner ?: "bottom_right"); if (idx >= 0) setSelection(idx) }
        val spQrSize = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, qrSizeLabels); val idx = qrSizeValues.indexOf(existingItem?.qrSizeDp ?: 150); setSelection(if (idx >= 0) idx else 2) }
        val spQrBorder = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, qrBorderLabels); val idx = qrBorderValues.indexOf(existingItem?.qrBorderDp ?: 8); setSelection(if (idx >= 0) idx else 2) }

        val qrOptionsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = if (cbShowQr.isChecked) View.VISIBLE else View.GONE; setPadding(24, 6, 0, 6)
            addView(TextView(this@MainActivity).apply { text = "QR Kod Köşesi:"; textSize = 12f }); addView(spQrCorner)
            addView(TextView(this@MainActivity).apply { text = "QR Kod Boyutu:"; textSize = 12f; setPadding(0, 8, 0, 0) }); addView(spQrSize)
            addView(TextView(this@MainActivity).apply { text = "QR Beyaz Çerçeve Genişliği:"; textSize = 12f; setPadding(0, 8, 0, 0) }); addView(spQrBorder)
        }
        cbShowQr.setOnCheckedChangeListener { _, isChecked -> qrOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE }

        layout.addView(TextView(this).apply { text = "Web Sayfası URL Adresi:"; typeface = Typeface.DEFAULT_BOLD }); layout.addView(urlRow)
        layout.addView(TextView(this).apply { text = "Gösterim Süresi (saniye):"; setPadding(0, 12, 0, 0) }); layout.addView(edtDur)
        layout.addView(TextView(this).apply { text = "İki Medya Arası Bekleme (saniye):"; setPadding(0, 10, 0, 0) }); layout.addView(edtWait)
        layout.addView(TextView(this).apply { text = "Giriş Animasyonu:"; setPadding(0, 10, 0, 0) }); layout.addView(spAnim)
        layout.addView(cbAutoScroll); layout.addView(cbScrollEnd)
        layout.addView(TextView(this).apply { text = "Aşağı Kaydırma Hızı:"; setPadding(0, 8, 0, 0); typeface = Typeface.DEFAULT_BOLD }); layout.addView(spScrollSpeed)
        layout.addView(TextView(this).apply { text = "Sayfa Ölçeklendirme (Zoom):"; setPadding(0, 12, 0, 0); typeface = Typeface.DEFAULT_BOLD }); layout.addView(spZoom)
        layout.addView(TextView(this).apply { text = "Web Yazı Tipi (Türkçe Uyumlu Fontlar):"; setPadding(0, 12, 0, 0); typeface = Typeface.DEFAULT_BOLD }); layout.addView(spFontFamily)
        layout.addView(TextView(this).apply { text = "Web Yazı Boyutu:"; setPadding(0, 10, 0, 0); typeface = Typeface.DEFAULT_BOLD }); layout.addView(spFontSize)
        layout.addView(TextView(this).apply { text = "Web Görsel Boyutu:"; setPadding(0, 10, 0, 0); typeface = Typeface.DEFAULT_BOLD }); layout.addView(spImageScale)
        layout.addView(cbBoldHeaders); layout.addView(cbShowQr); layout.addView(qrOptionsContainer)

        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "🌐 URL Ayarlarını Düzenle" else "🌐 Yeni URL Ekle")
            .setView(scrollView)
            .setPositiveButton(if (isEdit) "Kaydet" else "Ekle ve Yayınla") { _, _ ->
                var rawUrl = edtUrl.text.toString().trim()
                if (rawUrl.isEmpty()) { Toast.makeText(this, "Lütfen geçerli bir URL girin!", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) rawUrl = "https://$rawUrl"

                val dur = edtDur.text.toString().toIntOrNull() ?: 20
                val wait = edtWait.text.toString().toIntOrNull() ?: 0
                val anim = animValues[spAnim.selectedItemPosition]
                val autoScroll = cbAutoScroll.isChecked || cbScrollEnd.isChecked
                val scrollEnd = cbScrollEnd.isChecked
                val scrollSpeed = scrollSpeedValues[spScrollSpeed.selectedItemPosition]
                val zoomPct = webZoomValues[spZoom.selectedItemPosition]
                val fontFam = webFontFamilyValues[spFontFamily.selectedItemPosition]
                val fontSz = webFontSizeValues[spFontSize.selectedItemPosition]
                val imgScale = webImageScaleValues[spImageScale.selectedItemPosition]
                val boldHdr = cbBoldHeaders.isChecked
                val showQr = cbShowQr.isChecked
                val qrCorner = qrCornerValues[spQrCorner.selectedItemPosition]
                val qrSize = qrSizeValues[spQrSize.selectedItemPosition]
                val qrBorder = qrBorderValues[spQrBorder.selectedItemPosition]

                if (isEdit && existingItem != null) {
                    existingItem.fileName = rawUrl; existingItem.durationSec = dur; existingItem.waitAfterSec = wait
                    existingItem.animation = anim; existingItem.autoScroll = autoScroll; existingItem.scrollUntilEnd = scrollEnd
                    existingItem.webScrollSpeed = scrollSpeed; existingItem.webZoomPercent = zoomPct
                    existingItem.webFontFamily = fontFam; existingItem.webFontSizePercent = fontSz
                    existingItem.webImageScalePercent = imgScale; existingItem.webBoldHeaders = boldHdr
                    existingItem.showQr = showQr; existingItem.qrCorner = qrCorner; existingItem.qrSizeDp = qrSize; existingItem.qrBorderDp = qrBorder
                } else {
                    playlist.add(MediaItemModel(
                        id = System.currentTimeMillis().toString(), fileName = rawUrl, type = "web",
                        durationSec = dur, waitAfterSec = wait, animation = anim, autoScroll = autoScroll,
                        scrollUntilEnd = scrollEnd, webScrollSpeed = scrollSpeed, showQr = showQr,
                        qrCorner = qrCorner, qrSizeDp = qrSize, qrBorderDp = qrBorder, webZoomPercent = zoomPct,
                        webFontFamily = fontFam, webFontSizePercent = fontSz, webBoldHeaders = boldHdr,
                        webImageScalePercent = imgScale
                    ))
                }
                activeUrlInputField = null; syncPlaylistToMenuboard()
            }
            .setNegativeButton("İptal") { _, _ -> activeUrlInputField = null }
            .setOnDismissListener { activeUrlInputField = null }
            .show()
    }

    private fun setupDynamicSefimTabAndControls() {
        val topTabsBar = tabDevices.parent as? LinearLayout
        val existingTabSefimId = resources.getIdentifier("tabSefim", "id", packageName)
        tabSefim = if (existingTabSefimId != 0) findViewById<Button>(existingTabSefimId)?.apply { text = "🍽️ 🔒" }
        else Button(this).apply {
            text = "🍽️ 🔒"; setTextColor(0xFF888888.toInt()); textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(android.R.drawable.list_selector_background)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f); topTabsBar?.addView(this)
        }

        val mainRoot = panelTabDevices.parent as? LinearLayout
        val existingPanelSefimId = resources.getIdentifier("panelTabSefim", "id", packageName)
        if (existingPanelSefimId != 0) findViewById<View>(existingPanelSefimId)?.let { mainRoot?.removeView(it) }

        val wrapper = FrameLayout(this).apply { visibility = View.GONE; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f) }
        mainRoot?.addView(wrapper, ((mainRoot.childCount) - 1).coerceAtLeast(2))
        panelTabSefimWrapper = wrapper

        sefimScrollView = NestedScrollView(this).apply { isFillViewport = true; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 30); layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        sefimScrollView.addView(container); wrapper.addView(sefimScrollView)

        val topBtnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        topBtnRow.addView(Button(this).apply { text = "⚙️ DB Ayarları"; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#455A64")); layoutParams = LinearLayout.LayoutParams(0, 115, 1f).apply { rightMargin = 10 }; setOnClickListener { showDbConfigDialog() } })
        topBtnRow.addView(Button(this).apply { text = "🔄 DB'den Çek"; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0288D1")); layoutParams = LinearLayout.LayoutParams(0, 115, 1f).apply { rightMargin = 10 }; setOnClickListener { fetchSefimProductsFromDb() } })
        topBtnRow.addView(Button(this).apply { text = "🧪 Test Menüleri"; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#7B1FA2")); layoutParams = LinearLayout.LayoutParams(0, 115, 1.1f); setOnClickListener { showDemoMenuSelectorDialog() } })
        container.addView(topBtnRow)

        val headerBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#ECEFF1")); setPadding(16, 12, 16, 12); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 12 } }
        val rowRest = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        edtRestaurantName = EditText(this).apply { hint = "Restoran Adı"; setText("BALIKÇI ŞÜKRÜ"); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        edtMenuPageDuration = EditText(this).apply { hint = "Süre(sn)"; setText("12"); textSize = 13f; inputType = InputType.TYPE_CLASS_NUMBER; gravity = Gravity.CENTER; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(170, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = 10 } }
        rowRest.addView(edtRestaurantName); rowRest.addView(edtMenuPageDuration); headerBox.addView(rowRest)

        edtHeaderNote = EditText(this).apply { hint = "Üst Bilgi / Slogan"; textSize = 13f; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 4 } }
        edtFooterNote = EditText(this).apply { hint = "Alt Bilgi"; setText("Afiyet Olsun • KDV Dahildir"); textSize = 13f; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 4 } }
        edtWebsiteLink = EditText(this).apply { hint = "🔗 QR Kod Linki"; textSize = 13f; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 4 } }
        headerBox.addView(edtHeaderNote); headerBox.addView(edtFooterNote); headerBox.addView(edtWebsiteLink); container.addView(headerBox)

        val filterRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#E0F2F1")); setPadding(12, 8, 12, 8); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 } }
        cbShowChoice1 = CheckBox(this).apply { text = "Seçenek 1"; isChecked = true; textSize = 12f }
        cbShowChoice2 = CheckBox(this).apply { text = "Seçenek 2"; isChecked = true; textSize = 12f }
        cbShowOptions = CheckBox(this).apply { text = "Opsiyon"; isChecked = true; textSize = 12f }
        val filterWatcher = CompoundButton.OnCheckedChangeListener { _, _ -> sefimAdapter.notifyDataSetChanged(); saveLocalSefimState() }
        cbShowChoice1.setOnCheckedChangeListener(filterWatcher); cbShowChoice2.setOnCheckedChangeListener(filterWatcher); cbShowOptions.setOnCheckedChangeListener(filterWatcher)
        filterRow.addView(cbShowChoice1); filterRow.addView(cbShowChoice2); filterRow.addView(cbShowOptions); container.addView(filterRow)

        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#F5F5F5")); setPadding(8, 6, 8, 6); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 } }
        spinnerSefimTheme = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, themeNames); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f) }
        spinnerSefimFontSize = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, fontSizes); setSelection(1); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        cbSefimBold = CheckBox(this).apply { text = "Kalın"; isChecked = true; textSize = 12f }
        themeRow.addView(spinnerSefimTheme); themeRow.addView(spinnerSefimFontSize); themeRow.addView(cbSefimBold); container.addView(themeRow)

        val animRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#FFF3E0")); setPadding(14, 8, 14, 8); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 } }
        animRow.addView(TextView(this).apply { text = "✨ Animasyon:"; setTextColor(Color.parseColor("#E65100")); typeface = Typeface.DEFAULT_BOLD; textSize = 13f })
        spinnerSefimBgObject = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, bgObjectNames); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 10 } }
        animRow.addView(spinnerSefimBgObject); container.addView(animRow)

        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#E8EAF6")); setPadding(14, 6, 14, 6); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 } }
        edtProductSearch = EditText(this).apply {
            hint = "🔍 Ürün Ara..."; textSize = 14f; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { currentSearchQuery = s?.toString()?.trim() ?: ""; filterSefimProductList(currentSearchQuery) }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        searchRow.addView(edtProductSearch); searchRow.addView(Button(this).apply { text = "✖"; textSize = 12f; layoutParams = LinearLayout.LayoutParams(100, 90); setOnClickListener { edtProductSearch.setText("") } })
        container.addView(searchRow)

        container.addView(Button(this).apply {
            text = "📺 Menüyü & Animasyonu Menuboard'da Yayınla"; isAllCaps = false; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; textSize = 14f
            setBackgroundColor(Color.parseColor("#E65100")); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120).apply { topMargin = 10 }
            setOnClickListener { publishSefimMenuToMenuboard() }
        })

        rvSefimProducts = RecyclerView(this).apply {
            setBackgroundColor(Color.parseColor("#141414")); setPadding(10, 10, 10, 10); isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(this@MainActivity); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 }
        }
        sefimAdapter = SefimProductAdapter(); rvSefimProducts.adapter = sefimAdapter; container.addView(rvSefimProducts)

        wrapper.addView(Button(this).apply { text = "▲"; textSize = 16f; setTextColor(Color.WHITE); setBackgroundColor(Color.argb(210, 25, 118, 210)); layoutParams = FrameLayout.LayoutParams(110, 110).apply { gravity = Gravity.TOP or Gravity.END; topMargin = 18; rightMargin = 18 }; setOnClickListener { sefimScrollView.smoothScrollBy(0, -650) }; setOnLongClickListener { sefimScrollView.fullScroll(View.FOCUS_UP); true } })
        wrapper.addView(Button(this).apply { text = "▼"; textSize = 16f; setTextColor(Color.WHITE); setBackgroundColor(Color.argb(210, 25, 118, 210)); layoutParams = FrameLayout.LayoutParams(110, 110).apply { gravity = Gravity.BOTTOM or Gravity.END; bottomMargin = 18; rightMargin = 18 }; setOnClickListener { sefimScrollView.smoothScrollBy(0, 650) }; setOnLongClickListener { sefimScrollView.fullScroll(View.FOCUS_DOWN); true } })

        val styleListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { applySefimPreviewBackground(); sefimAdapter.notifyDataSetChanged(); saveLocalSefimState() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerSefimTheme.onItemSelectedListener = styleListener; spinnerSefimFontSize.onItemSelectedListener = styleListener
        cbSefimBold.setOnCheckedChangeListener { _, _ -> sefimAdapter.notifyDataSetChanged(); saveLocalSefimState() }
        spinnerSefimBgObject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveLocalSefimState(); val host = activeDevice?.host ?: return
                httpClient.newCall(Request.Builder().url("http://$host/api/sefim/bgobject?mode=${bgObjectValues[position]}").post("".toRequestBody(null)).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) {}; override fun onResponse(call: Call, response: Response) { response.close() } })
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterSefimProductList(query: String) {
        filteredSefimProducts.clear()
        if (query.isEmpty()) filteredSefimProducts.addAll(sefimProducts)
        else {
            val q = query.lowercase()
            for (item in sefimProducts) if ((item.productName ?: "").lowercase().contains(q) || (item.recipeContent ?: "").lowercase().contains(q) || buildFilteredChoicesString(item).lowercase().contains(q)) filteredSefimProducts.add(item)
        }
        if (::sefimAdapter.isInitialized) sefimAdapter.notifyDataSetChanged()
    }

    private fun computeAllergenWarning(item: SefimProductItem?): String {
        if (item == null) return ""
        val detected = mutableListOf<String>()
        val text = (item.recipeContent ?: "").lowercase()
        val rules = listOf(
            "Gluten" to listOf("un", "buğday", "ekmek", "lavaş", "pide", "makarna", "bulgur", "irmik", "galeta", "hamur", "kadayıf", "baklava", "yufka", "bisküvi", "gluten"),
            "Süt Ürünü" to listOf("süt", "peynir", "kaşar", "tereyağ", "krema", "yoğurt", "kaymak", "dondurma", "mozzarella", "labne", "laktoz"),
            "Yumurta" to listOf("yumurta", "mayonez", "mereng"), "Kuruyemiş" to listOf("fıstık", "fındık", "ceviz", "badem", "kaju", "antep"),
            "Susam" to listOf("susam", "tahin", "helva"), "Deniz Ürünü" to listOf("balık", "levrek", "çupra", "somon", "hamsi", "karides", "kalamar", "midye", "ahtapot", "yengeç"),
            "Soya" to listOf("soya"), "Hardal" to listOf("hardal")
        )
        for ((allergenName, keywords) in rules) if (keywords.any { text.contains(it) }) detected.add(allergenName)
        val manual = (item.manualAllergen ?: "").trim(); if (manual.isNotEmpty()) detected.add(manual)
        return detected.distinct().joinToString(", ")
    }

    private fun buildFilteredChoicesString(item: SefimProductItem?): String {
        if (item == null) return ""
        return try {
            val c1 = (item.choice1Text ?: "").trim(); val c2 = (item.choice2Text ?: "").trim()
            val opt = (item.optionsText ?: "").trim(); val legacySub = (item.subDetails ?: "").trim()
            val s1 = if (::cbShowChoice1.isInitialized) cbShowChoice1.isChecked else true
            val s2 = if (::cbShowChoice2.isInitialized) cbShowChoice2.isChecked else true
            val sO = if (::cbShowOptions.isInitialized) cbShowOptions.isChecked else true
            val parts = mutableListOf<String>()
            if (s1 && c1.isNotEmpty()) parts.add("Seçenek 1: $c1")
            if (s2 && c2.isNotEmpty()) parts.add("Seçenek 2: $c2")
            if (sO && opt.isNotEmpty()) parts.add("Opsiyon: $opt")
            if (parts.isEmpty() && legacySub.isNotEmpty() && (s1 || s2 || sO)) legacySub else parts.joinToString(" | ")
        } catch (_: Exception) { "" }
    }

    private fun showDemoMenuSelectorDialog() {
        val categories = arrayOf("🐟 Örnek Balık Restoranı Menüsü", "🥙 Örnek Döner & Kebap Menüsü", "☕ Örnek Cafe & Bistro Menüsü", "🍰 Örnek Tatlıcı & Pastane Menüsü")
        AlertDialog.Builder(this).setTitle("🧪 Test Menüsü Seçin").setItems(categories) { _, which -> loadDemoCategory(which) }.setNegativeButton("İptal", null).show()
    }

    private fun loadDemoCategory(catIndex: Int) {
        val items: List<SefimProductItem> = when (catIndex) {
            0 -> {
                edtRestaurantName.setText("BALIKÇI ŞÜKRÜ"); edtHeaderNote.setText("Günlük Taze Deniz Ürünleri & Izgara"); spinnerSefimTheme.setSelection(4); spinnerSefimBgObject.setSelection(1)
                listOf(
                    SefimProductItem("1", "Izgara Levrek Porsiyon", "380 ₺", "Roka Salatası", "Köz Soğan", "Mısır Ekmeği", "", "Taze deniz levreği, zeytinyağı, limon"),
                    SefimProductItem("2", "Deniz Çuprası (450gr)", "410 ₺", "Mevsim Salata", "Limon Sos", "Fırın Patates", "", "Çupra balığı, defne yaprağı"),
                    SefimProductItem("3", "Tereyağlı Karides Güveç", "360 ₺", "Bol Kaşarlı", "Sarımsaklı", "Acı Pul Biber", "", "Çimçim karides, tereyağ, kaşar peyniri, sarımsak"),
                    SefimProductItem("4", "Kalamar Tava Spesiyal", "340 ₺", "Tarator Sos", "Akdeniz Yeşillik", "Çıtır Kaplama", "", "Ege kalamarı, un, yumurta, ceviz, yoğurt, sarımsak")
                )
            }
            1 -> {
                edtRestaurantName.setText("USTA DÖNER & KEBAP"); edtHeaderNote.setText("%100 Yaprak Et Döner"); spinnerSefimTheme.setSelection(2); spinnerSefimBgObject.setSelection(2)
                listOf(
                    SefimProductItem("1", "İskender Kebap", "390 ₺", "Ekstra Tereyağ", "Tava Yoğurdu", "Köz Biber", "", "Yaprak döner, tırnak pide, tereyağ, yoğurt"),
                    SefimProductItem("2", "Et Döner Dürüm", "270 ₺", "Çift Lavaş", "Patates", "Soğanlı", "", "Yaprak et döner, lavaş ekmeği")
                )
            }
            2 -> {
                edtRestaurantName.setText("ROAST & BREW CAFE"); edtHeaderNote.setText("3. Nesil Kahve"); spinnerSefimTheme.setSelection(5); spinnerSefimBgObject.setSelection(4)
                listOf(SefimProductItem("1", "Caramel Macchiato", "135 ₺", "Yulaf Sütü", "Ekstra Espresso", "Sıcak / Buzlu", "", "Espresso, taze süt, karamel sos"))
            }
            else -> {
                edtRestaurantName.setText("SARAY PASTANESİ"); edtHeaderNote.setText("Günlük Taze Pasta & Tatlılar"); spinnerSefimTheme.setSelection(7); spinnerSefimBgObject.setSelection(5)
                listOf(SefimProductItem("1", "San Sebastian Cheesecake", "210 ₺", "Belçika Çikolatalı", "Taze Çilekli", "Ekstra Sos", "", "Krema, labne peynir, yumurta, un"))
            }
        }
        sefimProducts.clear()
        for (prod in items) {
            productMetaCache[prod.productName ?: ""]?.let { cached ->
                if (!cached.imageBase64.isNullOrEmpty()) prod.imageBase64 = cached.imageBase64
                if (!cached.recipeContent.isNullOrEmpty()) prod.recipeContent = cached.recipeContent
                if (!cached.manualAllergen.isNullOrEmpty()) prod.manualAllergen = cached.manualAllergen
            }
            sefimProducts.add(prod)
        }
        saveLocalSefimState(); pushSefimBackupToMenuboard(); filterSefimProductList(currentSearchQuery)
    }

    private fun showEditRecipeAndAllergenDialog(item: SefimProductItem) {
        val pName = item.productName ?: ""
        val edtRecipe = EditText(this).apply { hint = "Reçete / İçerik"; setText(item.recipeContent ?: ""); minLines = 2 }
        val edtManualAllergen = EditText(this).apply { hint = "Manuel Alerjen Ekle"; setText(item.manualAllergen ?: "") }
        val txtPreview = TextView(this).apply { setPadding(0, 16, 0, 8); setTextColor(Color.parseColor("#D32F2F")); typeface = Typeface.DEFAULT_BOLD; val w = computeAllergenWarning(item); text = if (w.isNotEmpty()) "⚠️ Alerjen: $w" else "✓ Alerjen tespit edilmedi" }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { val w = computeAllergenWarning(item.copy(recipeContent = edtRecipe.text.toString(), manualAllergen = edtManualAllergen.text.toString())); txtPreview.text = if (w.isNotEmpty()) "⚠️ Alerjen: $w" else "✓ Alerjen tespit edilmedi" }
            override fun afterTextChanged(s: Editable?) {}
        }
        edtRecipe.addTextChangedListener(watcher); edtManualAllergen.addTextChangedListener(watcher)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10); addView(TextView(this@MainActivity).apply { text = "Ürün İçeriği:" }); addView(edtRecipe); addView(TextView(this@MainActivity).apply { text = "Ek Manuel Alerjen:"; setPadding(0, 14, 0, 0) }); addView(edtManualAllergen); addView(txtPreview) }
        AlertDialog.Builder(this).setTitle("$pName - İçerik & Alerjen").setView(layout).setPositiveButton("Kaydet") { _, _ ->
            item.recipeContent = edtRecipe.text.toString().trim(); item.manualAllergen = edtManualAllergen.text.toString().trim()
            val meta = productMetaCache.getOrPut(pName) { ProductMetaCache() }; meta.recipeContent = item.recipeContent; meta.manualAllergen = item.manualAllergen
            saveLocalSefimState(); pushSefimBackupToMenuboard(); filterSefimProductList(currentSearchQuery)
        }.setNegativeButton("İptal", null).show()
    }

    private fun showDeviceAdvancedMenu(device: DeviceItem) {
        val isLandscape = device.orientation == "landscape"
        val tvModeOption = if (isLandscape) "📱 Dikey Moda Çevir (9:16)" else "📺 TV Moduna Çevir (16:9 Yatay)"
        val qualStr = when (device.blurQuality ?: 1) { 0 -> "Düşük"; 2 -> "Yüksek"; else -> "Orta" }
        val blurOption = if (device.blurBg) "🌫️ Bulanık Arka Plan: AÇIK (%${device.blurDarkness ?: 30} / $qualStr)" else "🌫️ Bulanık Arka Plan: KAPALI (Ayarla)"
        val isImmersive = device.immersiveMode != false
        val immersiveOption = if (isImmersive) "🖥️ Tam Ekran (Çubuklar Gizli) → Göster" else "📱 Sistem Çubukları Görünür → Gizle"
        val options = arrayOf(
            "🖱️ Mouse & Klavye Kontrolü (Sanal Touchpad)",
            tvModeOption,
            blurOption,
            immersiveOption,
            "⬇️ Güncelleme İndir / Kontrol Et",
            "📦 Uzaktan APK Gönder & Kur",
            "🚀 Menuboard'da Kurulu Uygulamaları Gör / Başlat",
            "📺 Menuboard Uygulamasını Tekrar Öne Getir"
        )
        val verTitle = if (!device.version.isNullOrEmpty()) " (v${device.version})" else ""
        AlertDialog.Builder(this).setTitle("${device.nameTag}$verTitle Yönetimi").setItems(options) { _, which ->
            when (which) {
                0 -> verifyPermissionsAndOpenTouchpad(device)
                1 -> toggleDeviceOrientation(device)
                2 -> showBlurBackgroundDialog(device)
                3 -> toggleDeviceImmersiveMode(device)
                4 -> triggerRemoteUpdateCheck(device)
                5 -> { targetApkDevice = device; pickRemoteApkLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }) }
                6 -> fetchAndShowInstalledApps(device)
                7 -> { sendRemoteInput(device, "type=nav&act=MENUBOARD"); Toast.makeText(this, "Menuboard öne getirme komutu gönderildi.", Toast.LENGTH_SHORT).show() }
            }
        }.setNegativeButton("Kapat", null).show()
    }

    private fun showBlurBackgroundDialog(device: DeviceItem) {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 35, 50, 15)
        }
        scroll.addView(layout)

        val cbEnable = CheckBox(this).apply {
            text = "Bulanık Arka Planı (Blur Dolgu) Etkinleştir"
            isChecked = device.blurBg
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        val curDarkness = (device.blurDarkness ?: 30).coerceIn(0, 85)
        val txtDarkness = TextView(this).apply {
            text = "Koyuluk / Karartma: %$curDarkness (0: Açık — 85: Çok Koyu)"
            setPadding(0, 22, 0, 6)
            textSize = 13f
        }
        val seekDarkness = SeekBar(this).apply {
            max = 85
            progress = curDarkness
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    txtDarkness.text = "Koyuluk / Karartma: %$progress (0: Açık — 85: Çok Koyu)"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val intensityLabels = arrayOf("Az", "Orta", "Çok")
        val curIntensity = (device.blurIntensity ?: 1).coerceIn(0, 2)
        val txtIntensity = TextView(this).apply {
            text = "Bulanıklık Şiddeti: ${intensityLabels[curIntensity]}"
            setPadding(0, 22, 0, 6)
            textSize = 13f
        }
        val seekIntensity = SeekBar(this).apply {
            max = 2
            progress = curIntensity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    txtIntensity.text = "Bulanıklık Şiddeti: ${intensityLabels[progress.coerceIn(0, 2)]}"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val qualityLabels = arrayOf("Düşük (En Hafif)", "Orta (Dengeli)", "Yüksek (En Akıcı)")
        val curQuality = (device.blurQuality ?: 1).coerceIn(0, 2)
        val txtQuality = TextView(this).apply {
            text = "Arka Plan Kalitesi: ${qualityLabels[curQuality]}"
            setPadding(0, 22, 0, 6)
            textSize = 13f
        }
        val seekQuality = SeekBar(this).apply {
            max = 2
            progress = curQuality
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    txtQuality.text = "Arka Plan Kalitesi: ${qualityLabels[progress.coerceIn(0, 2)]}"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val cbApplyIg = CheckBox(this).apply {
            text = "📸 Instagram medyalarını da etkilesin"
            isChecked = device.blurApplyIg == true
            textSize = 13f
            setPadding(0, 18, 0, 4)
        }

        val slidersBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = if (cbEnable.isChecked) 1.0f else 0.45f
            addView(txtDarkness)
            addView(seekDarkness)
            addView(txtIntensity)
            addView(seekIntensity)
            addView(txtQuality)
            addView(seekQuality)
            addView(cbApplyIg)
        }
        fun setControlsEnabled(enabled: Boolean) {
            slidersBox.alpha = if (enabled) 1.0f else 0.45f
            seekDarkness.isEnabled = enabled
            seekIntensity.isEnabled = enabled
            seekQuality.isEnabled = enabled
            cbApplyIg.isEnabled = enabled
        }
        setControlsEnabled(cbEnable.isChecked)
        cbEnable.setOnCheckedChangeListener { _, checked -> setControlsEnabled(checked) }

        layout.addView(cbEnable)
        layout.addView(slidersBox)

        AlertDialog.Builder(this)
            .setTitle("🌫️ Bulanık Arka Plan Ayarları")
            .setView(scroll)
            .setPositiveButton("Uygula") { _, _ ->
                applyDeviceBlurBackground(
                    device,
                    cbEnable.isChecked,
                    seekDarkness.progress,
                    seekIntensity.progress,
                    seekQuality.progress,
                    cbApplyIg.isChecked
                )
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun applyDeviceBlurBackground(device: DeviceItem, enabled: Boolean, darkness: Int, intensity: Int, quality: Int, applyIg: Boolean) {
        val url = "http://${device.host}/api/blur_bg?enabled=$enabled&darkness=$darkness&intensity=$intensity&quality=$quality&ig=$applyIg"
        httpClient.newCall(Request.Builder().url(url).post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Bulanık arka plan ayarları gönderilemedi!", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful; response.close()
                if (ok) {
                    device.blurBg = enabled
                    device.blurDarkness = darkness
                    device.blurIntensity = intensity
                    device.blurQuality = quality
                    device.blurApplyIg = applyIg
                    saveDevices()
                    val qualStr = when (quality) { 0 -> "Düşük"; 2 -> "Yüksek"; else -> "Orta" }
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            if (enabled) "🌫️ Bulanık arka plan güncellendi (%$darkness / $qualStr)" else "⬛ Bulanık arka plan kapatıldı!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun toggleDeviceImmersiveMode(device: DeviceItem) {
        val newState = !(device.immersiveMode != false)
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/immersive?enabled=$newState").post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Tam ekran komutu gönderilemedi!", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful; response.close()
                if (ok) {
                    device.immersiveMode = newState
                    saveDevices()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, if (newState) "🖥️ Tam ekran modu açıldı (Çubuklar gizli)" else "📱 Sistem çubukları görünür yapıldı", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun toggleDeviceOrientation(device: DeviceItem) {
        val newMode = if (device.orientation == "landscape") "portrait" else "landscape"
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/orientation?mode=$newMode").post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Yön değiştirme komutu gönderilemedi!", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful; response.close()
                if (ok) {
                    device.orientation = newMode
                    saveDevices()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, if (newMode == "landscape") "📺 TV Modu (16:9 Yatay) aktif edildi!" else "📱 Dikey Mod (9:16) aktif edildi!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun triggerRemoteUpdateCheck(device: DeviceItem) {
        Toast.makeText(this, "${device.nameTag} için güncelleme kontrolü başlatılıyor...", Toast.LENGTH_SHORT).show()
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/update/check").post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Cihaza ulaşılamadı!", Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful; response.close()
                runOnUiThread {
                    if (ok) Toast.makeText(this@MainActivity, "Güncelleme komutu gönderildi! Yeni sürüm varsa Menuboard indirip kurulumu açacaktır.", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this@MainActivity, "Bu cihaz eski sürümde; önce APK göndererek güncelleyin.", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun verifyPermissionsAndOpenTouchpad(device: DeviceItem) {
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/permissions/check").post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { Toast.makeText(this@MainActivity, "Menuboard cihazına bağlanılamadı!", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "{}"; response.close()
                try {
                    val allGranted = JSONObject(body).optBoolean("allGranted", false)
                    runOnUiThread {
                        if (!allGranted) {
                            AlertDialog.Builder(this@MainActivity).setTitle("⚠️ İzin Gerekli")
                                .setMessage("Gösterim izni yok, menuboardda ekrana gelen izinleri sağlayınız.")
                                .setPositiveButton("Yine de Touchpad'i Aç") { _, _ -> showVirtualTouchpadAndKeyboardDialog(device) }
                                .setNegativeButton("Tamam", null).show()
                        } else showVirtualTouchpadAndKeyboardDialog(device)
                    }
                } catch (_: Exception) { runOnUiThread { showVirtualTouchpadAndKeyboardDialog(device) } }
            }
        })
    }

    private fun showVirtualTouchpadAndKeyboardDialog(device: DeviceItem) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 20, 28, 14) }
        val touchpad = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400); setBackgroundColor(Color.parseColor("#263238")) }
        touchpad.addView(TextView(this).apply { text = "🖱️ SANAL TOUCHPAD\n(Parmağınızı Gezdirin: İmleç | Dokunun: Sol Tık)"; setTextColor(Color.parseColor("#80CBC4")); gravity = Gravity.CENTER; textSize = 13f }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var lastX = 0f; var lastY = 0f; var downX = 0f; var downY = 0f; var downTime = 0L; var lastSendTime = 0L
        touchpad.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastX = ev.x; lastY = ev.y; downX = ev.x; downY = ev.y; downTime = System.currentTimeMillis() }
                MotionEvent.ACTION_MOVE -> {
                    val now = System.currentTimeMillis(); val dx = (ev.x - lastX) * 1.9f; val dy = (ev.y - lastY) * 1.9f
                    if (now - lastSendTime > 28L && (kotlin.math.abs(dx) > 1.5f || kotlin.math.abs(dy) > 1.5f)) { lastX = ev.x; lastY = ev.y; lastSendTime = now; sendRemoteInput(device, "type=move&dx=${dx.toInt()}&dy=${dy.toInt()}") }
                }
                MotionEvent.ACTION_UP -> { if (System.currentTimeMillis() - downTime < 260L && hypot((ev.x - downX).toDouble(), (ev.y - downY).toDouble()) < 18.0) sendRemoteInput(device, "type=click") }
            }
            true
        }
        root.addView(touchpad)

        val navRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 4) }
        navRow1.addView(Button(this).apply { text = "◀ Geri"; textSize = 12f; isAllCaps = false; setPadding(4, 16, 4, 16); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 6 }; setOnClickListener { sendRemoteInput(device, "type=nav&act=BACK") } })
        navRow1.addView(Button(this).apply { text = "⏺ Ana Ekran"; textSize = 12f; isAllCaps = false; setPadding(4, 16, 4, 16); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f).apply { rightMargin = 6 }; setOnClickListener { sendRemoteInput(device, "type=nav&act=HOME") } })
        navRow1.addView(Button(this).apply { text = "📺 Öne Getir"; textSize = 12f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#E65100")); setPadding(4, 16, 4, 16); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f); setOnClickListener { sendRemoteInput(device, "type=nav&act=MENUBOARD") } })
        root.addView(navRow1)

        val navRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 8) }
        navRow2.addView(Button(this).apply { text = "▲ Yukarı Kaydır"; textSize = 12f; isAllCaps = false; setPadding(4, 14, 4, 14); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 6 }; setOnClickListener { sendRemoteInput(device, "type=scroll&dy=-350") } })
        navRow2.addView(Button(this).apply { text = "▼ Aşağı Kaydır"; textSize = 12f; isAllCaps = false; setPadding(4, 14, 4, 14); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { sendRemoteInput(device, "type=scroll&dy=350") } })
        root.addView(navRow2)

        val kbRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val edtRemoteKb = EditText(this).apply { hint = "Menuboard'a yazılacak metin..."; textSize = 13f; setSingleLine(true); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        kbRow.addView(edtRemoteKb)
        kbRow.addView(Button(this).apply {
            text = "⌨️ Gönder"; textSize = 12f; isAllCaps = false; setPadding(16, 14, 16, 14); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = 6 }
            setOnClickListener {
                val txt = edtRemoteKb.text.toString()
                if (txt.isNotEmpty()) { sendRemoteInput(device, "type=text&val=${Uri.encode(txt)}"); edtRemoteKb.setText("") }
                else { edtRemoteKb.requestFocus(); (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(edtRemoteKb, InputMethodManager.SHOW_IMPLICIT) }
            }
        })
        kbRow.addView(Button(this).apply { text = "⌫"; textSize = 14f; setPadding(12, 14, 12, 14); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = 6 }; setOnClickListener { sendRemoteInput(device, "type=key&code=BACKSPACE") } })
        root.addView(kbRow)

        AlertDialog.Builder(this).setTitle("🖱️ ${device.nameTag} - Mouse & Klavye").setView(root).setPositiveButton("Kapat", null).show()
    }

    private fun sendRemoteInput(device: DeviceItem, queryParams: String) {
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/input?$queryParams").post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    private fun uploadAndInstallApkOnMenuboard(device: DeviceItem, uri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val totalLength = try { contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
        Toast.makeText(this, "APK ${device.nameTag} cihazına gönderiliyor...", Toast.LENGTH_LONG).show()
        uploadProgressBar?.visibility = View.VISIBLE; uploadProgressBar?.progress = 0

        val customBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
            override fun contentLength(): Long = totalLength
            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(65536); var read: Int; var uploaded = 0L; var lastPct = -1
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        if (totalLength > 0) { uploaded += read; val p = ((uploaded * 100) / totalLength).toInt(); if (p != lastPct) { lastPct = p; runOnUiThread { uploadProgressBar?.progress = p } } }
                    }
                }
            }
        }
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/upload_raw?filename=remote.apk&apk=true").post(customBody).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { uploadProgressBar?.visibility = View.GONE; Toast.makeText(this@MainActivity, "APK gönderimi başarısız!", Toast.LENGTH_LONG).show() } }
            override fun onResponse(call: Call, response: Response) { response.close(); runOnUiThread { uploadProgressBar?.visibility = View.GONE; Toast.makeText(this@MainActivity, "APK gönderildi ve Menuboard'da kurulum başlatıldı!", Toast.LENGTH_LONG).show() } }
        })
    }

    private fun fetchAndShowInstalledApps(device: DeviceItem) {
        Toast.makeText(this, "${device.nameTag} üzerindeki uygulamalar getiriliyor...", Toast.LENGTH_SHORT).show()
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/apps").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { Toast.makeText(this@MainActivity, "Cihaza ulaşılamadı!", Toast.LENGTH_LONG).show() } }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "[]"; response.close()
                try {
                    val apps: List<RemoteAppInfo> = gson.fromJson(body, object : TypeToken<List<RemoteAppInfo>>() {}.type)
                    if (apps.isEmpty()) { runOnUiThread { Toast.makeText(this@MainActivity, "Uygulama listesi alınamadı.", Toast.LENGTH_LONG).show() }; return }
                    val labels = apps.map { "${it.appName}\n(${it.packageName})" }.toTypedArray()
                    runOnUiThread { AlertDialog.Builder(this@MainActivity).setTitle("${device.nameTag} - Uygulama Başlat").setItems(labels) { _, idx -> launchRemoteApp(device, apps[idx]) }.setNegativeButton("İptal", null).show() }
                } catch (_: Exception) {}
            }
        })
    }

    private fun launchRemoteApp(device: DeviceItem, app: RemoteAppInfo) {
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/apps/launch?pkg=${Uri.encode(app.packageName)}").post("".toRequestBody(null)).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { Toast.makeText(this@MainActivity, "Başlatma komutu gönderilemedi!", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.close(); runOnUiThread { Toast.makeText(this@MainActivity, "${app.appName} Menuboard'da başlatıldı!", Toast.LENGTH_SHORT).show() } }
        })
    }

    private fun saveLocalSefimState() {
        val dbPrefs = getSharedPreferences("sefim_v118_prefs", Context.MODE_PRIVATE)
        val payload = SefimBackupPayload(
            dbServer = dbPrefs.getString("db_server", "192.168.2.167\\YIRMIBES") ?: "", dbName = dbPrefs.getString("db_name", "BALIKCISUKRU") ?: "",
            dbUser = dbPrefs.getString("db_user", "sa") ?: "", dbPass = dbPrefs.getString("db_pass", "") ?: "",
            restaurantName = edtRestaurantName.text.toString().trim(), headerNote = edtHeaderNote.text.toString().trim(),
            footerNote = edtFooterNote.text.toString().trim(), websiteUrl = edtWebsiteLink.text.toString().trim(),
            pageDurationSec = edtMenuPageDuration.text.toString().toIntOrNull() ?: 12,
            showChoice1 = cbShowChoice1.isChecked, showChoice2 = cbShowChoice2.isChecked, showOptions = cbShowOptions.isChecked,
            themeIndex = spinnerSefimTheme.selectedItemPosition, fontSizeIndex = spinnerSefimFontSize.selectedItemPosition,
            isBold = cbSefimBold.isChecked, bgObjectMode = bgObjectValues.getOrElse(spinnerSefimBgObject.selectedItemPosition) { "none" },
            products = sefimProducts, metaCacheByName = productMetaCache
        )
        dbPrefs.edit().putString("sefim_v118_json", gson.toJson(payload)).apply()
    }

    private fun loadLocalSefimState() {
        val json = getSharedPreferences("sefim_v118_prefs", Context.MODE_PRIVATE).getString("sefim_v118_json", null) ?: return
        try {
            val payload = gson.fromJson(json, SefimBackupPayload::class.java) ?: return
            edtRestaurantName.setText((payload.restaurantName ?: "").ifEmpty { "BALIKÇI ŞÜKRÜ" })
            edtHeaderNote.setText(payload.headerNote ?: ""); edtFooterNote.setText(payload.footerNote ?: ""); edtWebsiteLink.setText(payload.websiteUrl ?: "")
            edtMenuPageDuration.setText((if (payload.pageDurationSec > 0) payload.pageDurationSec else 12).toString())
            cbShowChoice1.isChecked = payload.showChoice1; cbShowChoice2.isChecked = payload.showChoice2; cbShowOptions.isChecked = payload.showOptions
            productMetaCache.clear(); payload.metaCacheByName?.let { productMetaCache.putAll(it) }
            sefimProducts.clear(); payload.products?.let { sefimProducts.addAll(it) }
            sefimProducts.forEach { prod ->
                productMetaCache[prod.productName ?: ""]?.let { meta ->
                    if (prod.imageBase64.isNullOrEmpty()) prod.imageBase64 = meta.imageBase64
                    if (prod.recipeContent.isNullOrEmpty()) prod.recipeContent = meta.recipeContent
                    if (prod.manualAllergen.isNullOrEmpty()) prod.manualAllergen = meta.manualAllergen
                }
            }
            filterSefimProductList(currentSearchQuery)
        } catch (_: Exception) {}
    }

    private fun pushSefimBackupToMenuboard() {
        val host = activeDevice?.host ?: return
        val json = getSharedPreferences("sefim_v118_prefs", Context.MODE_PRIVATE).getString("sefim_v118_json", null) ?: return
        httpClient.newCall(Request.Builder().url("http://$host/api/sefim/backup").post(json.toRequestBody("application/json".toMediaTypeOrNull())).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) {}; override fun onResponse(call: Call, response: Response) { response.close() } })
    }

    private fun syncSefimBackupWithMenuboard() {
        val host = activeDevice?.host ?: return
        httpClient.newCall(Request.Builder().url("http://$host/api/sefim/backup").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val remoteJson = response.body?.string()?.trim() ?: ""; response.close()
                if (remoteJson.isNotEmpty() && sefimProducts.isEmpty()) {
                    try {
                        val payload = gson.fromJson(remoteJson, SefimBackupPayload::class.java) ?: return
                        getSharedPreferences("sefim_v118_prefs", Context.MODE_PRIVATE).edit().putString("db_server", payload.dbServer ?: "").putString("db_name", payload.dbName ?: "").putString("db_user", payload.dbUser ?: "").putString("db_pass", payload.dbPass ?: "").putString("sefim_v118_json", remoteJson).apply()
                        payload.metaCacheByName?.let { productMetaCache.putAll(it) }; sefimProducts.clear(); payload.products?.let { sefimProducts.addAll(it) }
                        runOnUiThread {
                            edtRestaurantName.setText(payload.restaurantName ?: ""); edtHeaderNote.setText(payload.headerNote ?: ""); edtFooterNote.setText(payload.footerNote ?: ""); edtWebsiteLink.setText(payload.websiteUrl ?: "")
                            edtMenuPageDuration.setText((if (payload.pageDurationSec > 0) payload.pageDurationSec else 12).toString()); filterSefimProductList(currentSearchQuery)
                        }
                    } catch (_: Exception) {}
                } else if (remoteJson.isEmpty() && sefimProducts.isNotEmpty()) pushSefimBackupToMenuboard()
            }
        })
    }

    private fun showDbConfigDialog() {
        val prefs = getSharedPreferences("sefim_v118_prefs", Context.MODE_PRIVATE)
        val edtServer = EditText(this).apply { hint = "192.168.2.167\\YIRMIBES"; setText(prefs.getString("db_server", "192.168.2.167\\YIRMIBES")) }
        val edtDbName = EditText(this).apply { hint = "BALIKCISUKRU"; setText(prefs.getString("db_name", "BALIKCISUKRU")) }
        val edtUser = EditText(this).apply { hint = "sa"; setText(prefs.getString("db_user", "sa")) }
        val edtPass = EditText(this).apply { hint = "Şifre"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setText(prefs.getString("db_pass", "")) }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10); addView(TextView(this@MainActivity).apply { text = "SQL Server IP:" }); addView(edtServer); addView(TextView(this@MainActivity).apply { text = "Database Adı:" }); addView(edtDbName); addView(TextView(this@MainActivity).apply { text = "Kullanıcı Adı:" }); addView(edtUser); addView(TextView(this@MainActivity).apply { text = "Şifre:" }); addView(edtPass) }
        AlertDialog.Builder(this).setTitle("Şefim MS SQL Bağlantı Ayarları").setView(layout).setPositiveButton("Kaydet") { _, _ ->
            prefs.edit().putString("db_server", edtServer.text.toString().trim()).putString("db_name", edtDbName.text.toString().trim()).putString("db_user", edtUser.text.toString().trim()).putString("db_pass", edtPass.text.toString()).apply()
            saveLocalSefimState(); pushSefimBackupToMenuboard(); Toast.makeText(this, "Kaydedildi.", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("İptal", null).show()
    }

    private fun buildJdbcConnectionUrl(rawServerInput: String, dbName: String): String {
        val normalized = rawServerInput.trim().replace("/", "\\")
        var hostPart = normalized; var instancePart = ""
        if (normalized.contains("\\")) { val split = normalized.split("\\", limit = 2); hostPart = split[0].trim(); instancePart = split[1].trim() }
        return if (instancePart.isNotEmpty()) "jdbc:jtds:sqlserver://$hostPart/$dbName;instance=$instancePart"
        else "jdbc:jtds:sqlserver://${if (hostPart.contains(":")) hostPart else "$hostPart:1433"}/$dbName"
    }

    private fun fetchSefimProductsFromDb() {
        val prefs = getSharedPreferences("sefim_v118_prefs", Context.MODE_PRIVATE)
        val server = prefs.getString("db_server", "") ?: ""; val dbName = prefs.getString("db_name", "") ?: ""
        val user = prefs.getString("db_user", "") ?: ""; val pass = prefs.getString("db_pass", "") ?: ""
        if (server.isEmpty() || dbName.isEmpty()) { showDbConfigDialog(); return }
        Toast.makeText(this, "Şefim veritabanına bağlanılıyor...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                Class.forName("net.sourceforge.jtds.jdbc.Driver"); DriverManager.setLoginTimeout(8)
                val conn = try { DriverManager.getConnection(buildJdbcConnectionUrl(server, dbName), user, pass) }
                catch (firstEx: Exception) { if (server.contains("\\") && !server.contains(":")) DriverManager.getConnection("jdbc:jtds:sqlserver://${server.split("\\")[0].trim()}:1433/$dbName", user, pass) else throw firstEx }
                val rs = conn.createStatement().executeQuery("SELECT * FROM Product"); val meta = rs.metaData
                val fetchedList = mutableListOf<SefimProductItem>()
                val prevSel = sefimProducts.associate { (it.productName ?: "") to it.isSelected }
                while (rs.next()) {
                    val id = getColString(rs, meta, listOf("Id", "ProductId", "RECNO", "Kod"))
                    val name = getColString(rs, meta, listOf("ProductName", "Name", "UrunAdi", "Header", "Description"))
                    val rawPrice = getColString(rs, meta, listOf("Price", "Price1", "Fiyat", "SatisFiyati", "Amount"))
                    if (name.isNotEmpty()) {
                        val cachedMeta = productMetaCache[name]
                        fetchedList.add(SefimProductItem(id, name, if (rawPrice.isNotEmpty()) "$rawPrice ₺" else "", "", "", "", "", cachedMeta?.recipeContent ?: "", cachedMeta?.manualAllergen ?: "", prevSel[name] ?: true, cachedMeta?.imageBase64))
                    }
                }
                rs.close(); conn.close()
                runOnUiThread { sefimProducts.clear(); sefimProducts.addAll(fetchedList); saveLocalSefimState(); pushSefimBackupToMenuboard(); filterSefimProductList(currentSearchQuery); Toast.makeText(this, "${fetchedList.size} ürün getirildi!", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) { runOnUiThread { AlertDialog.Builder(this).setTitle("Bağlantı Hatası").setMessage("Hata: ${e.localizedMessage}").setPositiveButton("Tamam", null).show() } }
        }.start()
    }

    private fun getColString(rs: java.sql.ResultSet, meta: java.sql.ResultSetMetaData, candidates: List<String>): String {
        for (colName in candidates) for (i in 1..meta.columnCount) if (meta.getColumnName(i).equals(colName, ignoreCase = true)) {
            val v = rs.getString(i)?.trim() ?: ""; if (v.isNotEmpty() && v != "null" && v != "0" && v != "0.00") return v
        }
        return ""
    }

    private fun getSelectedThemeColors(): Triple<Int, Int, Int> = when (spinnerSefimTheme.selectedItemPosition) {
        1 -> Triple(Color.parseColor("#FAFAFA"), Color.parseColor("#212121"), Color.parseColor("#D32F2F"))
        2 -> Triple(Color.parseColor("#1B0505"), Color.parseColor("#FFF8E1"), Color.parseColor("#FF5252"))
        3 -> Triple(Color.parseColor("#0A0E27"), Color.parseColor("#E0F7FA"), Color.parseColor("#00E5FF"))
        4 -> Triple(Color.parseColor("#041E3A"), Color.parseColor("#E1F5FE"), Color.parseColor("#29B6F6"))
        5 -> Triple(Color.parseColor("#0D2618"), Color.parseColor("#E8F5E9"), Color.parseColor("#69F0AE"))
        6 -> Triple(Color.parseColor("#2B0912"), Color.parseColor("#FFF3E0"), Color.parseColor("#FFC107"))
        7 -> Triple(Color.parseColor("#FFF0F5"), Color.parseColor("#4A1525"), Color.parseColor("#D81B60"))
        8 -> Triple(Color.parseColor("#26140C"), Color.parseColor("#FFF8F0"), Color.parseColor("#FF8A65"))
        9 -> Triple(Color.parseColor("#1E272C"), Color.parseColor("#ECEFF1"), Color.parseColor("#80CBC4"))
        else -> Triple(Color.parseColor("#121212"), Color.parseColor("#FFFFFF"), Color.parseColor("#FFD700"))
    }

    private fun applySefimPreviewBackground() { rvSefimProducts.setBackgroundColor(getSelectedThemeColors().first) }

    private fun publishSefimMenuToMenuboard() {
        val host = activeDevice?.host ?: return
        val selectedItems = sefimProducts.filter { it.isSelected }
        if (selectedItems.isEmpty()) { Toast.makeText(this, "Liste boş!", Toast.LENGTH_LONG).show(); return }
        val restName = edtRestaurantName.text.toString().trim().ifEmpty { "MENÜ" }
        val topInfo = edtHeaderNote.text.toString().trim()
        val bottomInfo = edtFooterNote.text.toString().trim()
        val rawWebLink = edtWebsiteLink.text.toString().trim()
        val pageDurSec = (edtMenuPageDuration.text.toString().toIntOrNull() ?: 12).coerceAtLeast(3)

        saveLocalSefimState(); pushSefimBackupToMenuboard()
        Toast.makeText(this, "Menü sayfaları hazırlanıyor...", Toast.LENGTH_SHORT).show()

        Thread {
            val width = 1080; val height = 1920
            val (bgColor, textColor, accentColor) = getSelectedThemeColors()
            val isBold = cbSefimBold.isChecked
            val baseFontPt = fontSizeValues[spinnerSefimFontSize.selectedItemPosition]
            val titleSizePx = baseFontPt * 2.0f; val subSizePx = baseFontPt * 1.25f

            var linkQrBitmap: Bitmap? = null
            if (rawWebLink.isNotEmpty()) {
                val url = if (rawWebLink.startsWith("http://") || rawWebLink.startsWith("https://")) rawWebLink else "https://$rawWebLink"
                try { linkQrBitmap = BarcodeEncoder().encodeBitmap(url, BarcodeFormat.QR_CODE, 165, 165) } catch (_: Exception) {}
            }

            val startTopY = if (topInfo.isNotEmpty()) 245f else 200f
            val maxBottomY = if (linkQrBitmap != null) height - 220f else if (bottomInfo.isNotEmpty()) height - 150f else height - 90f

            val pages = mutableListOf<List<SefimProductItem>>()
            var currentPageItems = mutableListOf<SefimProductItem>()
            var testY = startTopY
            for (item in selectedItems) {
                val hasImg = !(item.imageBase64 ?: productMetaCache[item.productName ?: ""]?.imageBase64).isNullOrEmpty()
                val rowH = maxOf(if (hasImg) 145f else 0f, titleSizePx + subSizePx + 60f)
                if (testY + rowH > maxBottomY && currentPageItems.isNotEmpty()) { pages.add(currentPageItems); currentPageItems = mutableListOf(); testY = startTopY }
                currentPageItems.add(item); testY += rowH
            }
            if (currentPageItems.isNotEmpty()) pages.add(currentPageItems)

            playlist.removeAll { it.fileName.startsWith("sefim_menu_") }
            val batchTime = System.currentTimeMillis(); val totalPages = pages.size

            for ((pageIdx, pageItems) in pages.withIndex()) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap); canvas.drawColor(bgColor)
                val paintRest = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; textSize = 58f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textColor; textSize = titleSizePx; typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL) }
                val paintPrice = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; textSize = titleSizePx; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
                val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; alpha = 75; strokeWidth = 2f }

                canvas.drawText(restName, width / 2f, 92f, paintRest)
                var currentY = startTopY
                for (item in pageItems) {
                    canvas.drawText(item.productName ?: "", 65f, currentY + 12f, paintTitle)
                    canvas.drawText(item.price ?: "", width - 65f, currentY + 12f, paintPrice)
                    currentY += titleSizePx + 40f
                    canvas.drawLine(65f, currentY, width - 65f, currentY, paintLine); currentY += 32f
                }

                val stream = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                val fileName = "sefim_menu_${batchTime}_p${pageIdx + 1}.jpg"
                try {
                    httpClient.newCall(Request.Builder().url("http://$host/api/upload_raw?filename=$fileName").post(stream.toByteArray().toRequestBody("application/octet-stream".toMediaTypeOrNull())).build()).execute().use { resp ->
                        if (resp.isSuccessful) playlist.add(MediaItemModel(id = "${batchTime}_${pageIdx + 1}", fileName = fileName, type = "image", durationSec = pageDurSec, waitAfterSec = 0, animation = "fade"))
                    }
                } catch (_: Exception) {}
            }
            runOnUiThread { Toast.makeText(this, "Toplam $totalPages sayfa menü yayınlandı!", Toast.LENGTH_LONG).show(); syncPlaylistToMenuboard() }
        }.start()
    }

    private fun checkAllDevicesStatus() {
        val now = System.currentTimeMillis()
        if (deviceList.isEmpty()) return
        for (dev in deviceList) {
            pingClient.newCall(Request.Builder().url("http://${dev.host}/api/ping").build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (now - dev.lastSeenMs > 10000L) dev.isOnline = false
                    if (activeDevice?.id == dev.id) updateConnectionUi(dev.isOnline)
                    runOnUiThread { deviceAdapter.notifyDataSetChanged() }
                }
                override fun onResponse(call: Call, response: Response) {
                    val ok = response.isSuccessful
                    val bodyStr = response.body?.string() ?: ""
                    response.close()
                    dev.isOnline = ok
                    if (ok) {
                        dev.lastSeenMs = System.currentTimeMillis()
                        try {
                            val json = JSONObject(bodyStr)
                            val ver = json.optString("version", "")
                            if (ver.isNotEmpty()) dev.version = ver
                            val ori = json.optString("orientation", "")
                            if (ori.isNotEmpty()) dev.orientation = ori
                            val blurStr = json.optString("blurBg", "")
                            if (blurStr.isNotEmpty()) dev.blurBg = (blurStr == "true")
                            val darkStr = json.optString("blurDarkness", "")
                            if (darkStr.isNotEmpty()) dev.blurDarkness = darkStr.toIntOrNull() ?: 30
                            val intenStr = json.optString("blurIntensity", "")
                            if (intenStr.isNotEmpty()) dev.blurIntensity = intenStr.toIntOrNull() ?: 1
                            val qualStr = json.optString("blurQuality", "")
                            if (qualStr.isNotEmpty()) dev.blurQuality = qualStr.toIntOrNull() ?: 1
                            val igStr = json.optString("blurIg", "")
                            if (igStr.isNotEmpty()) dev.blurApplyIg = (igStr == "true")
                            val immStr = json.optString("immersive", "")
                            if (immStr.isNotEmpty()) dev.immersiveMode = (immStr == "true")
                        } catch (_: Exception) {}
                    }
                    if (activeDevice?.id == dev.id) {
                        val wasDisc = !isConnected; updateConnectionUi(ok)
                        if (ok && (wasDisc || playlist.isEmpty())) { fetchPlaylist(); syncSefimBackupWithMenuboard() }
                    }
                    runOnUiThread { deviceAdapter.notifyDataSetChanged() }
                }
            })
        }
    }

    private fun checkForUpdate() {
        if (GITHUB_REPO.startsWith("KULLANICI_ADINIZ")) return
        httpClient.newCall(Request.Builder().url("https://api.github.com/repos/$GITHUB_REPO/releases/latest").header("User-Agent", "Anypay-Remote-App").header("Accept", "application/vnd.github+json").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val rawTag = json.optString("tag_name", "v1.00")
                    val remoteCode = rawTag.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 100
                    val curCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt() else @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0).versionCode
                    if (remoteCode > curCode) {
                        val assets = json.optJSONArray("assets") ?: return
                        var dlUrl = ""
                        for (i in 0 until assets.length()) { val a = assets.getJSONObject(i); val n = a.getString("name").lowercase(); if (n.contains("remote") && n.endsWith(".apk")) { dlUrl = a.getString("browser_download_url"); break } }
                        if (dlUrl.isNotEmpty()) runOnUiThread { showUpdateDialog(rawTag, dlUrl) }
                    }
                } catch (_: Exception) {}
            }
        })
    }

    private fun showUpdateDialog(versionTitle: String, downloadUrl: String) {
        AlertDialog.Builder(this).setTitle("Yeni Güncelleme Bulundu").setMessage("Kumanda için yeni sürüm mevcut ($versionTitle).\nŞimdi yüklemek ister misiniz?")
            .setPositiveButton("Güncelle") { _, _ -> downloadAndInstallApk(downloadUrl) }.setNegativeButton("Daha Sonra", null).show()
    }

    private fun downloadAndInstallApk(url: String) {
        Toast.makeText(this, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()
        val destination = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "remote_update_${System.currentTimeMillis()}.apk")
        if (destination.exists()) destination.delete()
        val req = DownloadManager.Request(Uri.parse(url)).setTitle("Kumanda Güncellemesi").setDescription("İndiriliyor...").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationUri(Uri.fromFile(destination))
        val downloadId = (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    unregisterReceiver(this)
                    val apkUri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", destination)
                    startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(apkUri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) })
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        else registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun stopScreenShare() {
        startService(Intent(this, ScreenCaptureService::class.java).apply { action = "STOP" })
        isSharingScreen = false; btnToggleScreenShare.text = "📱 Ekranı Paylaş"; btnToggleScreenShare.setBackgroundColor(0xFF7B1FA2.toInt())
    }

    private fun startUdpListener() {
        Thread {
            try {
                val socket = DatagramSocket(8888); val buffer = ByteArray(1024)
                while (isUdpListening) {
                    val packet = DatagramPacket(buffer, buffer.size); socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length)
                    if (msg.startsWith("ANYPAY_DISCOVERY:")) {
                        val p = msg.split(":"); if (p.size >= 5) runOnUiThread { updateOrAddDevice(p[1], "${p[3]}:${p[4]}", p[2], true) }
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun updateOrAddDevice(id: String, host: String, tag: String, online: Boolean): DeviceItem {
        val now = System.currentTimeMillis()
        var existing = deviceList.find { it.id == id }
        if (existing != null) { existing.host = host; existing.isOnline = online; if (online) existing.lastSeenMs = now }
        else { existing = DeviceItem(id, host, tag, isOnline = online, lastSeenMs = if (online) now else 0L); deviceList.add(existing) }
        saveDevices(); deviceAdapter.notifyDataSetChanged()
        if (activeDevice?.id == id) { activeDevice?.host = host; activeDevice?.isOnline = online; txtActiveDeviceName.text = "Aktif: ${activeDevice?.nameTag} ($host)"; updateConnectionUi(online) }
        return existing
    }

    private fun switchTab(tabIndex: Int) {
        panelTabDevices.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        panelTabMedia.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        panelTabSefimWrapper?.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        tabDevices.setTextColor(if (tabIndex == 0) 0xFF00E676.toInt() else 0xFF888888.toInt())
        tabMedia.setTextColor(if (tabIndex == 1) 0xFF00E676.toInt() else 0xFF888888.toInt())
        tabSefim?.setTextColor(if (tabIndex == 2) 0xFF00E676.toInt() else 0xFF888888.toInt())
    }

    private fun selectDevice(device: DeviceItem) {
        if (isSharingScreen) stopScreenShare()
        activeDevice = device; txtActiveDeviceName.text = "Aktif: ${device.nameTag} (${device.host})"
        playlist.clear(); mediaAdapter.notifyDataSetChanged()
        saveDevices(); deviceAdapter.notifyDataSetChanged(); checkAllDevicesStatus(); syncSefimBackupWithMenuboard()
    }

    private fun loadSavedDevices() {
        val json = getSharedPreferences("remote_prefs", Context.MODE_PRIVATE).getString("device_list_json", null)
        if (!json.isNullOrEmpty()) { deviceList = gson.fromJson(json, object : TypeToken<MutableList<DeviceItem>>() {}.type); deviceList.forEach { it.isOnline = false } }
    }

    private fun saveDevices() { getSharedPreferences("remote_prefs", Context.MODE_PRIVATE).edit().putString("device_list_json", gson.toJson(deviceList)).apply() }
    override fun onResume() { super.onResume(); handler.post(heartbeatRunnable) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(heartbeatRunnable) }

    private fun updateConnectionUi(connected: Boolean) {
        isConnected = connected
        runOnUiThread { txtConnectionStatus.text = if (connected) "Bağlı" else "Bağlı Değil"; txtConnectionStatus.setTextColor(if (connected) 0xFF2E7D32.toInt() else 0xFFD32F2F.toInt()) }
    }

    private fun fetchPlaylist() {
        val host = activeDevice?.host ?: return
        httpClient.newCall(Request.Builder().url("http://$host/api/playlist").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    playlist = gson.fromJson(response.body?.string() ?: "[]", object : TypeToken<MutableList<MediaItemModel>>() {}.type)
                    runOnUiThread { mediaAdapter.notifyDataSetChanged() }
                }
            }
        })
    }

    private fun uploadMediaFile(uri: Uri) {
        val host = activeDevice?.host ?: return
        val contentResolver = applicationContext.contentResolver
        val isVideo = (contentResolver.getType(uri) ?: "").startsWith("video")
        var fileName = "media_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { c -> val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && idx >= 0) fileName = c.getString(idx).replace(Regex("[^a-zA-Z0-9._-]"), "_") }
        var fileDurSec = 0
        if (isVideo) {
            val ret = MediaMetadataRetriever()
            try { ret.setDataSource(this, uri); fileDurSec = ((ret.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L) / 1000).toInt() } catch (_: Exception) {} finally { try { ret.release() } catch (_: Exception) {} }
        }
        val totalLen = try { contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
        Toast.makeText(this, "Yükleniyor ($fileName)...", Toast.LENGTH_SHORT).show()
        uploadProgressBar?.visibility = View.VISIBLE; uploadProgressBar?.progress = 0

        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
            override fun contentLength(): Long = totalLen
            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { inp ->
                    val buf = ByteArray(65536); var r: Int; var up = 0L; var lastP = -1
                    while (inp.read(buf).also { r = it } != -1) {
                        sink.write(buf, 0, r)
                        if (totalLen > 0) { up += r; val p = ((up * 100) / totalLen).toInt(); if (p != lastP) { lastP = p; runOnUiThread { uploadProgressBar?.progress = p } } }
                    }
                }
            }
        }
        httpClient.newCall(Request.Builder().url("http://$host/api/upload_raw?filename=${Uri.encode(fileName)}").post(body).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { uploadProgressBar?.visibility = View.GONE; Toast.makeText(this@MainActivity, "Yükleme Başarısız!", Toast.LENGTH_LONG).show() } }
            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful; response.close()
                runOnUiThread { uploadProgressBar?.visibility = View.GONE }
                if (ok) {
                    playlist.add(MediaItemModel(id = System.currentTimeMillis().toString(), fileName = fileName, type = if (isVideo) "video" else "image", durationSec = edtDuration.text.toString().toIntOrNull() ?: 0, waitAfterSec = edtWaitAfter.text.toString().toIntOrNull() ?: 0, animation = animValues[spinnerAnim.selectedItemPosition], fileDurationSec = fileDurSec))
                    syncPlaylistToMenuboard()
                } else runOnUiThread { Toast.makeText(this@MainActivity, "Sunucu dosyayı kaydedemedi!", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun syncPlaylistToMenuboard() {
        val host = activeDevice?.host ?: return
        httpClient.newCall(Request.Builder().url("http://$host/api/playlist").post(gson.toJson(playlist).toRequestBody("application/json".toMediaTypeOrNull())).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { Toast.makeText(this@MainActivity, "Senkronizasyon Başarısız!", Toast.LENGTH_SHORT).show() } }
            override fun onResponse(call: Call, response: Response) { response.close(); runOnUiThread { Toast.makeText(this@MainActivity, "Menuboard Güncellendi!", Toast.LENGTH_SHORT).show(); fetchPlaylist() } }
        })
    }

    private fun showEditTagDialog(device: DeviceItem) {
        val input = EditText(this).apply { hint = "Örn: 1, 2 veya Kasa"; setText(device.nameTag) }
        AlertDialog.Builder(this).setTitle("Nametag Belirle").setView(input).setPositiveButton("Kaydet") { _, _ ->
            val newTag = input.text.toString().trim()
            if (newTag.isNotEmpty()) {
                device.nameTag = newTag; saveDevices(); deviceAdapter.notifyDataSetChanged()
                if (activeDevice?.id == device.id) txtActiveDeviceName.text = "Aktif: ${device.nameTag} (${device.host})"
                httpClient.newCall(Request.Builder().url("http://${device.host}/api/tag?tag=${Uri.encode(newTag)}").post("".toRequestBody(null)).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) {}; override fun onResponse(call: Call, response: Response) { response.close() } })
            }
        }.setNegativeButton("İptal", null).show()
    }

    private fun toggleFindDevice(device: DeviceItem) {
        device.isFinding = !device.isFinding; deviceAdapter.notifyDataSetChanged()
        httpClient.newCall(Request.Builder().url("http://${device.host}/api/identify?show=${device.isFinding}&tag=${Uri.encode(device.nameTag)}").post("".toRequestBody(null)).build()).enqueue(object : Callback { override fun onFailure(call: Call, e: IOException) {}; override fun onResponse(call: Call, response: Response) { response.close() } })
    }

    override fun onDestroy() { super.onDestroy(); isUdpListening = false }

    inner class SefimProductAdapter : RecyclerView.Adapter<SefimProductAdapter.SefimViewHolder>() {
        inner class SefimViewHolder(v: View, val cbInclude: CheckBox, val imgPhoto: ImageView, val btnPickPhoto: ImageButton, val txtName: TextView, val txtChoices: TextView, val txtAllergen: TextView, val txtPrice: TextView, val btnEditRecipe: Button) : RecyclerView.ViewHolder(v)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SefimViewHolder {
            val row = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(12, 12, 12, 12); layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 } }
            val cb = CheckBox(parent.context)
            val pf = FrameLayout(parent.context).apply { layoutParams = LinearLayout.LayoutParams(125, 125).apply { leftMargin = 6 }; setBackgroundColor(Color.parseColor("#33888888")) }
            val img = ImageView(parent.context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
            val cam = ImageButton(parent.context).apply { setImageResource(android.R.drawable.ic_menu_camera); setBackgroundResource(android.R.drawable.list_selector_background); layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
            pf.addView(img); pf.addView(cam)
            val mid = LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 16; rightMargin = 10 } }
            val tN = TextView(parent.context).apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
            val tC = TextView(parent.context).apply { textSize = 11f }
            val tA = TextView(parent.context).apply { textSize = 11f; setTextColor(Color.parseColor("#FF5252")); typeface = Typeface.DEFAULT_BOLD; visibility = View.GONE }
            mid.addView(tN); mid.addView(tC); mid.addView(tA)
            val rCol = LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL }
            val tP = TextView(parent.context).apply { textSize = 15f; typeface = Typeface.DEFAULT_BOLD }
            val bR = Button(parent.context).apply { text = "📝 İçerik"; textSize = 10f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#37474F")); layoutParams = LinearLayout.LayoutParams(170, 82).apply { topMargin = 6 } }
            rCol.addView(tP); rCol.addView(bR); row.addView(cb); row.addView(pf); row.addView(mid); row.addView(rCol)
            return SefimViewHolder(row, cb, img, cam, tN, tC, tA, tP, bR)
        }
        override fun onBindViewHolder(holder: SefimViewHolder, position: Int) {
            val item = filteredSefimProducts.getOrNull(position) ?: return
            val pName = item.productName ?: ""
            val (_, textColor, accentColor) = getSelectedThemeColors()
            val fontSize = fontSizeValues.getOrElse(spinnerSefimFontSize.selectedItemPosition) { 18f }
            holder.cbInclude.setOnCheckedChangeListener(null); holder.cbInclude.isChecked = item.isSelected
            holder.cbInclude.setOnCheckedChangeListener { _, isChecked -> item.isSelected = isChecked; saveLocalSefimState() }
            val cachedB64 = item.imageBase64 ?: productMetaCache[pName]?.imageBase64
            if (!cachedB64.isNullOrEmpty()) {
                try { val b = Base64.decode(cachedB64, Base64.DEFAULT); holder.imgPhoto.setImageBitmap(BitmapFactory.decodeByteArray(b, 0, b.size)); holder.btnPickPhoto.alpha = 0.15f }
                catch (_: Exception) { holder.imgPhoto.setImageDrawable(null); holder.btnPickPhoto.alpha = 0.85f }
            } else { holder.imgPhoto.setImageDrawable(null); holder.btnPickPhoto.alpha = 0.85f }
            holder.btnPickPhoto.setOnClickListener {
                if (!cachedB64.isNullOrEmpty()) {
                    AlertDialog.Builder(this@MainActivity).setTitle(pName).setItems(arrayOf("📷 Yeni Görsel Seç", "🗑️ Mevcut Görseli Kaldır")) { _, w ->
                        if (w == 0) { pendingPhotoProductName = pName; pickProductPhotoLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*" }) }
                        else { productMetaCache[pName]?.imageBase64 = null; item.imageBase64 = null; saveLocalSefimState(); pushSefimBackupToMenuboard(); notifyDataSetChanged() }
                    }.show()
                } else { pendingPhotoProductName = pName; pickProductPhotoLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*" }) }
            }
            holder.txtName.text = pName; holder.txtName.setTextColor(textColor); holder.txtName.textSize = fontSize; holder.txtName.setTypeface(null, if (cbSefimBold.isChecked) Typeface.BOLD else Typeface.NORMAL)
            holder.txtPrice.text = item.price ?: ""; holder.txtPrice.setTextColor(accentColor); holder.txtPrice.textSize = fontSize; holder.txtPrice.setTypeface(null, Typeface.BOLD)
            val sub = buildString { val f = buildFilteredChoicesString(item); if (f.isNotEmpty()) append("↳ $f"); val r = item.recipeContent ?: ""; if (r.isNotEmpty()) { if (isNotEmpty()) append("\n"); append("🍽️ İçerik: $r") } }
            if (sub.isNotEmpty()) { holder.txtChoices.visibility = View.VISIBLE; holder.txtChoices.text = sub; holder.txtChoices.setTextColor(accentColor); holder.txtChoices.textSize = (fontSize - 4f).coerceAtLeast(10f) } else holder.txtChoices.visibility = View.GONE
            val alg = computeAllergenWarning(item)
            if (alg.isNotEmpty()) { holder.txtAllergen.visibility = View.VISIBLE; holder.txtAllergen.text = "⚠️ Alerjen: $alg" } else holder.txtAllergen.visibility = View.GONE
            holder.btnEditRecipe.setOnClickListener { showEditRecipeAndAllergenDialog(item) }
        }
        override fun getItemCount(): Int = filteredSefimProducts.size
    }

    inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
        inner class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val viewStatusDot: View = v.findViewById(R.id.viewStatusDot)
            val txtDeviceTag: TextView = v.findViewById(R.id.txtDeviceTag)
            val txtDeviceIp: TextView = v.findViewById(R.id.txtDeviceIp)
            val btnEditTag: ImageButton = v.findViewById(R.id.btnEditTag)
            val btnFindDevice: Button = v.findViewById(R.id.btnFindDevice)
            val btnDeleteDevice: ImageButton = v.findViewById(R.id.btnDeleteDevice)
            var btnDynamicSettings: Button? = null
            init {
                val rowLayout = btnFindDevice.parent as? LinearLayout
                val existingId = resources.getIdentifier("btnDeviceSettings", "id", packageName)
                val existingBtn = if (existingId != 0) v.findViewById<View>(existingId) else null
                if (existingBtn is Button) btnDynamicSettings = existingBtn
                else if (existingBtn != null) existingBtn.setOnClickListener { val p = adapterPosition; if (p != RecyclerView.NO_POSITION) showDeviceAdvancedMenu(deviceList[p]) }
                else if (rowLayout != null) {
                    val b = Button(v.context).apply { text = "⚙️ Ayar"; textSize = 11f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1976D2")); layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 92).apply { leftMargin = 8; rightMargin = 4 } }
                    rowLayout.addView(b, (rowLayout.indexOfChild(btnFindDevice) + 1).coerceAtMost(rowLayout.childCount)); btnDynamicSettings = b
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DeviceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))
        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            val dev = deviceList[position]
            val verSuffix = if (dev.isOnline && !dev.version.isNullOrEmpty()) " (v${dev.version})" else ""
            holder.txtDeviceTag.text = "${if (activeDevice?.id == dev.id) "✓ " else ""}${dev.nameTag}$verSuffix"
            holder.txtDeviceIp.text = dev.host
            holder.viewStatusDot.setBackgroundColor(if (dev.isOnline) 0xFF00C853.toInt() else 0xFFD32F2F.toInt())
            holder.btnFindDevice.text = if (dev.isFinding) "Gizle" else "Bul"; holder.btnFindDevice.setBackgroundColor(if (dev.isFinding) 0xFFD32F2F.toInt() else 0xFFF57C00.toInt())
            holder.itemView.setOnClickListener { selectDevice(dev); switchTab(1) }
            holder.btnEditTag.setOnClickListener { showEditTagDialog(dev) }
            holder.btnFindDevice.setOnClickListener { toggleFindDevice(dev) }
            holder.btnDynamicSettings?.setOnClickListener { showDeviceAdvancedMenu(dev) }
            holder.btnDeleteDevice.setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle("Cihazı Kaldır").setMessage("${dev.nameTag} silinsin mi?").setPositiveButton("Kaldır") { _, _ ->
                    deviceList.removeAt(holder.adapterPosition)
                    if (activeDevice?.id == dev.id) {
                        activeDevice = deviceList.firstOrNull()
                        if (activeDevice != null) selectDevice(activeDevice!!) else { txtActiveDeviceName.text = "Seçili Cihaz Yok"; playlist.clear(); mediaAdapter.notifyDataSetChanged() }
                    }
                    saveDevices(); notifyDataSetChanged()
                }.setNegativeButton("İptal", null).show()
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
            var btnToggleVisibility: ImageButton? = null

            init {
                val actionParent = btnEdit.parent as? LinearLayout
                if (actionParent != null) {
                    val density = v.resources.displayMetrics.density
                    val sizePx = (40 * density).toInt()
                    val eyeBtn = ImageButton(v.context).apply {
                        setImageResource(android.R.drawable.ic_menu_view)
                        setBackgroundResource(android.R.drawable.list_selector_background)
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        contentDescription = "Göster / Gizle"
                        layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                            marginEnd = (4 * density).toInt()
                        }
                    }
                    val editIdx = actionParent.indexOfChild(btnEdit).coerceAtLeast(0)
                    actionParent.addView(eyeBtn, editIdx)
                    btnToggleVisibility = eyeBtn
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false))

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val item = playlist[position]
            val host = activeDevice?.host ?: ""
            holder.txtFileName.text = if (item.isHidden) "${item.fileName} (Gizli)" else item.fileName
            holder.txtAnimBadge.text = "Animasyon: ${animOptions.getOrNull(animValues.indexOf(item.animation)) ?: "Solma"}"

            val contentAlpha = if (item.isHidden) 0.40f else 1.0f
            holder.imgThumb.alpha = contentAlpha
            holder.txtFileName.alpha = contentAlpha
            holder.txtDetails.alpha = contentAlpha
            holder.btnToggleVisibility?.alpha = if (item.isHidden) 0.25f else 1.0f
            holder.btnToggleVisibility?.setOnClickListener {
                item.isHidden = !item.isHidden
                notifyItemChanged(holder.adapterPosition)
                syncPlaylistToMenuboard()
            }

            if (item.type == "web") {
                holder.txtTypeBadge.text = "URL"; holder.txtTypeBadge.setBackgroundColor(0xCC00897B.toInt())
                val spd = scrollSpeedLabels.getOrElse(scrollSpeedValues.indexOf(item.webScrollSpeed ?: "medium")) { "Orta" }
                val scr = when { item.scrollUntilEnd -> "Sonuna Kadar ($spd)"; item.autoScroll -> "${item.durationSec}sn ($spd)"; else -> "${item.durationSec}sn Sabit" }
                holder.txtDetails.text = "$scr | Zoom:%${item.webZoomPercent}${if (item.showQr) " | QR" else ""}"
                holder.imgThumb.setImageResource(android.R.drawable.ic_menu_compass); holder.imgThumb.setBackgroundColor(Color.parseColor("#E0F2F1"))
                holder.btnEdit.setOnClickListener { showUrlConfigDialog(existingItem = item) }
            } else if (item.type == "instagram") {
                holder.txtTypeBadge.text = "IG"; holder.txtTypeBadge.setBackgroundColor(0xCCE1306C.toInt())
                val modeStr = if (item.scrollUntilEnd) "Medyalar Bitene Kadar" else "${item.durationSec}sn"
                val fltStr = when (item.igMediaFilter) { "photo" -> "Fotoğraf"; "video" -> "Video"; else -> "Tümü" }
                val audioStr = if (item.igAudioEnabled) " | Sesli" else ""
                holder.txtDetails.text = "IG: $modeStr | $fltStr$audioStr"
                holder.imgThumb.setImageResource(android.R.drawable.ic_menu_camera); holder.imgThumb.setBackgroundColor(Color.parseColor("#FCE4EC"))
                holder.btnEdit.setOnClickListener { checkInstagramStatusAndOpenDialog(existingItem = item) }
            } else {
                holder.txtTypeBadge.text = if (item.type == "video") "VIDEO" else "RESIM"
                holder.txtTypeBadge.setBackgroundColor(if (item.type == "video") 0xCC1976D2.toInt() else 0xCC388E3C.toInt())
                holder.txtDetails.text = if (item.type == "video") "Video: ${if (item.durationSec > 0) "${item.durationSec}sn kesit" else "Tamamı"} | +${item.waitAfterSec}s ara"
                else "Resim: ${if (item.durationSec > 0) item.durationSec else 10}s süre | +${item.waitAfterSec}s ara"
                holder.imgThumb.setBackgroundColor(Color.TRANSPARENT)
                Glide.with(holder.itemView.context).load("http://$host/api/thumbnail?filename=${item.fileName}").diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(holder.imgThumb)
                holder.btnEdit.setOnClickListener {
                    val edtDur = EditText(this@MainActivity).apply { hint = "Süre (0=tamamı)"; inputType = InputType.TYPE_CLASS_NUMBER; setText(item.durationSec.toString()) }
                    val edtWait = EditText(this@MainActivity).apply { hint = "Ara Bekleme (sn)"; inputType = InputType.TYPE_CLASS_NUMBER; setText(item.waitAfterSec.toString()) }
                    val spAnim = Spinner(this@MainActivity).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, animOptions); val idx = animValues.indexOf(item.animation); if (idx >= 0) setSelection(idx) }
                    val layout = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 40, 50, 10); addView(TextView(this@MainActivity).apply { text = "Oynatma Süresi (sn):" }); addView(edtDur); addView(TextView(this@MainActivity).apply { text = "Ara Bekleme (sn):" }); addView(edtWait); addView(TextView(this@MainActivity).apply { text = "Giriş Animasyonu:" }); addView(spAnim) }
                    AlertDialog.Builder(this@MainActivity).setTitle("Medya Ayarları").setView(layout).setPositiveButton("Kaydet") { _, _ ->
                        item.durationSec = edtDur.text.toString().toIntOrNull() ?: item.durationSec
                        item.waitAfterSec = edtWait.text.toString().toIntOrNull() ?: item.waitAfterSec
                        item.animation = animValues[spAnim.selectedItemPosition]
                        syncPlaylistToMenuboard()
                    }.setNegativeButton("İptal", null).show()
                }
            }

            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle("Öğeyi Sil").setMessage("${item.fileName} silinsin mi?").setPositiveButton("Sil") { _, _ ->
                    playlist.removeAt(holder.adapterPosition); syncPlaylistToMenuboard()
                }.setNegativeButton("İptal", null).show()
            }
        }
        override fun getItemCount(): Int = playlist.size
    }
}
