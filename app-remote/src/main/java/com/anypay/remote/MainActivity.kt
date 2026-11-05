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
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import kotlin.math.hypot

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
    val choice1Text: String = "",
    val choice2Text: String = "",
    val optionsText: String = "",
    var recipeContent: String = "",
    var manualAllergen: String = "",
    var isSelected: Boolean = true,
    var imageBase64: String? = null
)

data class ProductMetaCache(
    var imageBase64: String? = null,
    var recipeContent: String = "",
    var manualAllergen: String = ""
)

data class SefimBackupPayload(
    val dbServer: String = "",
    val dbName: String = "",
    val dbUser: String = "",
    val dbPass: String = "",
    val restaurantName: String = "BALIKÇI ŞÜKRÜ",
    val headerNote: String = "",
    val footerNote: String = "Afiyet Olsun • KDV Dahildir",
    val pageDurationSec: Int = 12,
    val showChoice1: Boolean = true,
    val showChoice2: Boolean = true,
    val showOptions: Boolean = true,
    val themeIndex: Int = 0,
    val fontSizeIndex: Int = 1,
    val isBold: Boolean = true,
    val bgObjectMode: String = "none",
    val products: List<SefimProductItem> = emptyList(),
    val metaCacheByName: Map<String, ProductMetaCache> = emptyMap()
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
    private lateinit var btnLoadDemoProducts: Button
    private lateinit var edtRestaurantName: EditText
    private lateinit var edtMenuPageDuration: EditText
    private lateinit var edtHeaderNote: EditText
    private lateinit var edtFooterNote: EditText
    private lateinit var cbShowChoice1: CheckBox
    private lateinit var cbShowChoice2: CheckBox
    private lateinit var cbShowOptions: CheckBox
    private lateinit var spinnerSefimTheme: Spinner
    private lateinit var spinnerSefimFontSize: Spinner
    private lateinit var spinnerSefimBgObject: Spinner
    private lateinit var cbSefimBold: CheckBox
    private lateinit var rvSefimProducts: RecyclerView
    private lateinit var btnPublishSefimMenu: Button
    private lateinit var sefimAdapter: SefimProductAdapter
    private var sefimProducts = mutableListOf<SefimProductItem>()

    // Ürün İsmine Göre Görsel + Reçete + Alerjen Önbelleği
    private var productMetaCache = mutableMapOf<String, ProductMetaCache>()
    private var pendingPhotoProductName: String? = null
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

    // 10 Göz Alıcı Modern Tema Şablonu
    private val themeNames = arrayOf(
        "🖤 Koyu Lüks (Gold & Siyah)",
        "🤍 Modern Beyaz (Ferah)",
        "🔥 Restoran Ateşi (Kırmızı & Kömür)",
        "🌌 Neon Cyberpunk (Gece & Cyan)",
        "🌊 Okyanus Derinliği (Balık & Deniz)",
        "🌿 Zümrüt Bahçesi (Doğal & Vegan)",
        "👑 Osmanlı Kehribar (Bordo & Altın)",
        "🧁 Paris Pastanesi (Tatlı & Cafe)",
        "🍕 İtalyan Trattoria (Sıcak Terracotta)",
        "💎 Minimalist Slate (Modern Gri)"
    )

    private val fontSizes = arrayOf("Küçük (14pt)", "Orta (18pt)", "Büyük (22pt)", "Dev (26pt)")
    private val fontSizeValues = arrayOf(14f, 18f, 22f, 26f)

    private val bgObjectNames = arrayOf(
        "Yok (Sade Arka Plan)",
        "🐟 Deniz Akvaryumu (Balıklar, Ahtapot & Yengeç)",
        "🥙 Gerçekçi Dönen Döner & Kor Ateş Buharı",
        "💨 Gerçekçi Nargile & Yoğun Duman",
        "☕ Sıcak Kahve Buharı (Cafe)",
        "✨ Altın Işıltı (Luxury Bokeh)",
        "❄️ Kış Kar Yağışı"
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

                        val meta = productMetaCache.getOrPut(prodName) { ProductMetaCache() }
                        meta.imageBase64 = b64

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
        btnLoadDemoProducts = findViewById(R.id.btnLoadDemoProducts)
        edtRestaurantName = findViewById(R.id.edtRestaurantName)
        edtMenuPageDuration = findViewById(R.id.edtMenuPageDuration)
        edtHeaderNote = findViewById(R.id.edtHeaderNote)
        edtFooterNote = findViewById(R.id.edtFooterNote)
        cbShowChoice1 = findViewById(R.id.cbShowChoice1)
        cbShowChoice2 = findViewById(R.id.cbShowChoice2)
        cbShowOptions = findViewById(R.id.cbShowOptions)
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

        val filterChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            sefimAdapter.notifyDataSetChanged()
            saveLocalSefimState()
        }
        cbShowChoice1.setOnCheckedChangeListener(filterChangeListener)
        cbShowChoice2.setOnCheckedChangeListener(filterChangeListener)
        cbShowOptions.setOnCheckedChangeListener(filterChangeListener)

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
        btnLoadDemoProducts.setOnClickListener { showDemoMenuSelectorDialog() }
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

    // --- OTOMATİK VE MANUEL ALERJEN ALGILAMA MOTORU ---

    private fun computeAllergenWarning(item: SefimProductItem): String {
        val detected = mutableListOf<String>()
        val text = item.recipeContent.lowercase()

        val rules = listOf(
            "Gluten" to listOf("un", "buğday", "ekmek", "lavaş", "pide", "makarna", "bulgur", "irmik", "galeta", "hamur", "kadayıf", "baklava", "yufka", "bisküvi", "gluten"),
            "Süt Ürünü" to listOf("süt", "peynir", "kaşar", "tereyağ", "krema", "yoğurt", "kaymak", "dondurma", "mozzarella", "labne", "mascarpone", "laktoz"),
            "Yumurta" to listOf("yumurta", "mayonez", "mereng"),
            "Kuruyemiş" to listOf("fıstık", "fındık", "ceviz", "badem", "kaju", "antep"),
            "Susam" to listOf("susam", "tahin", "helva"),
            "Deniz Ürünü" to listOf("balık", "levrek", "çupra", "somon", "hamsi", "karides", "kalamar", "midye", "ahtapot", "yengeç"),
            "Soya" to listOf("soya"),
            "Hardal" to listOf("hardal")
        )

        for ((allergenName, keywords) in rules) {
            if (keywords.any { text.contains(it) }) {
                detected.add(allergenName)
            }
        }

        if (item.manualAllergen.isNotBlank()) {
            detected.add(item.manualAllergen.trim())
        }

        return detected.distinct().joinToString(", ")
    }

    private fun buildFilteredChoicesString(item: SefimProductItem): String {
        val parts = mutableListOf<String>()
        if (cbShowChoice1.isChecked && item.choice1Text.isNotEmpty()) {
            parts.add("Seçenek 1: ${item.choice1Text}")
        }
        if (cbShowChoice2.isChecked && item.choice2Text.isNotEmpty()) {
            parts.add("Seçenek 2: ${item.choice2Text}")
        }
        if (cbShowOptions.isChecked && item.optionsText.isNotEmpty()) {
            parts.add("Opsiyon: ${item.optionsText}")
        }
        return parts.joinToString(" | ")
    }

    // --- 4 SEKTÖREL TEST MENÜSÜ SEÇİCİ (BALIK, DÖNER, CAFE, TATLICI) ---

    private fun showDemoMenuSelectorDialog() {
        val categories = arrayOf(
            "🐟 Örnek Balık Restoranı Menüsü",
            "🥙 Örnek Döner & Kebap Menüsü",
            "☕ Örnek Cafe & Bistro Menüsü",
            "🍰 Örnek Tatlıcı & Pastane Menüsü"
        )

        AlertDialog.Builder(this)
            .setTitle("🧪 Test Menüsü Seçin")
            .setItems(categories) { _, which ->
                loadDemoCategory(which)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun loadDemoCategory(catIndex: Int) {
        val items: List<SefimProductItem> = when (catIndex) {
            0 -> {
                edtRestaurantName.setText("BALIKÇI ŞÜKRÜ")
                edtHeaderNote.setText("Günlük Taze Deniz Ürünleri & Izgara")
                spinnerSefimTheme.setSelection(4) // Okyanus Derinliği
                spinnerSefimBgObject.setSelection(1) // Deniz Akvaryumu
                listOf(
                    SefimProductItem("1", "Izgara Levrek Porsiyon", "380 ₺", "Roka Salatası", "Köz Soğan", "Mısır Ekmeği", "Taze deniz levreği, zeytinyağı, limon"),
                    SefimProductItem("2", "Deniz Çuprası (450gr)", "410 ₺", "Mevsim Salata", "Limon Sos", "Fırın Patates", "Çupra balığı, defne yaprağı"),
                    SefimProductItem("3", "Tereyağlı Karides Güveç", "360 ₺", "Bol Kaşarlı", "Sarımsaklı", "Acı Pul Biber", "Çimçim karides, tereyağ, kaşar peyniri, sarımsak"),
                    SefimProductItem("4", "Kalamar Tava Spesiyal", "340 ₺", "Tarator Sos", "Akdeniz Yeşillik", "Çıtır Kaplama", "Ege kalamarı, un, yumurta, ceviz, yoğurt, sarımsak"),
                    SefimProductItem("5", "Norveç Somon Izgara", "460 ₺", "Buharda Sebze", "Kremalı Sos", "Kinoa Salatası", "Somon balığı, krema, karabiber"),
                    SefimProductItem("6", "Ahtapot Izgara", "520 ₺", "Kırmızı Soğan", "Zeytinyağlı", "Sıcak Servis", "Ahtapot kolu, zeytinyağı, kekik"),
                    SefimProductItem("7", "Hamsi Tava Porsiyon", "260 ₺", "Çoban Salata", "Turp & Roka", "Mısır Unlu", "Karadeniz hamsi balığı, mısır unu"),
                    SefimProductItem("8", "Fırında Tahinli Helva", "150 ₺", "Bol Fındıklı", "Dondurmalı", "Sıcak Güveç", "Tahin, susam, süt, fındık")
                )
            }
            1 -> {
                edtRestaurantName.setText("USTA DÖNER & KEBAP")
                edtHeaderNote.setText("%100 Yaprak Et Döner & Odun Ateşi")
                spinnerSefimTheme.setSelection(2) // Restoran Ateşi
                spinnerSefimBgObject.setSelection(2) // Gerçekçi Döner & Ateş
                listOf(
                    SefimProductItem("1", "İskender Kebap (Yaprak)", "390 ₺", "Ekstra Tereyağ", "Tava Yoğurdu", "Köz Biber & Domates", "Yaprak döner, tırnak pide, tereyağ, yoğurt, domates sos"),
                    SefimProductItem("2", "Et Döner Dürüm (150gr)", "270 ₺", "Çift Lavaş", "Patates Kızartması", "Soğanlı / Soğansız", "Yaprak et döner, lavaş ekmeği, turşu"),
                    SefimProductItem("3", "Pilav Üstü Et Döner", "340 ₺", "Tereyağlı Pirinç", "Acı Biber Turşusu", "Lavaş İle", "Et döner, pirinç pilavı, tereyağ"),
                    SefimProductItem("4", "Beyti Sarma Kebap", "380 ₺", "Sarımsaklı Yoğurt", "Özel Domates Sos", "Antep Fıstıklı", "Kuzu kıyma, lavaş, tereyağ, yoğurt, fıstık"),
                    SefimProductItem("5", "Kaşarlı Pide Döner", "310 ₺", "Kapalı Pide", "Yumurta İlaveli", "Bol Tereyağ", "Buğday unu, kaşar peyniri, et döner, yumurta, tereyağ"),
                    SefimProductItem("6", "Künefe (Hatay Peynirli)", "190 ₺", "Kaymaklı", "Bol Antep Fıstıklı", "Sıcak Şerbetli", "Tel kadayıf, buğday unu, peynir, tereyağ, antep fıstık")
                )
            }
            2 -> {
                edtRestaurantName.setText("ROAST & BREW CAFE")
                edtHeaderNote.setText("3. Nesil Kahve & Artizan Lezzetler")
                spinnerSefimTheme.setSelection(5) // Zümrüt Bahçesi
                spinnerSefimBgObject.setSelection(4) // Kahve Buharı
                listOf(
                    SefimProductItem("1", "Caramel Macchiato", "135 ₺", "Yulaf Sütü", "Ekstra Espresso", "Sıcak / Buzlu", "Espresso, taze süt, karamel sos"),
                    SefimProductItem("2", "Avokado Poşe Yumurta Tost", "240 ₺", "Ekşi Maya Ekmek", "Akdeniz Yeşillik", "Çeçil Peynirli", "Ekşi maya ekmek, yumurta, avokado, peynir"),
                    SefimProductItem("3", "Fettuccine Alfredo", "280 ₺", "Izgara Tavuklu", "Mantarlı", "Parmesan Peynirli", "Makarna, krema, tereyağ, parmesan peynir"),
                    SefimProductItem("4", "Club Sandviç Menü", "260 ₺", "Baharatlı Patates", "Hardal Sos", "Kepekli Ekmek", "Tost ekmeği, tavuk, yumurta, mayonez, hardal, peynir"),
                    SefimProductItem("5", "Lotus Biscoff Latte", "145 ₺", "Tam Yağlı Süt", "Krema dolgulu", "Bisküvi Parçalı", "Espresso, süt, krema, karamelize bisküvi")
                )
            }
            else -> {
                edtRestaurantName.setText("SARAY PASTANESİ")
                edtHeaderNote.setText("Günlük Taze Pasta, Baklava & Sütlü Tatlılar")
                spinnerSefimTheme.setSelection(7) // Paris Pastanesi
                spinnerSefimBgObject.setSelection(5) // Altın Işıltı
                listOf(
                    SefimProductItem("1", "San Sebastian Cheesecake", "210 ₺", "Belçika Çikolatalı", "Taze Çilekli", "Ekstra Sos", "Krema, labne peynir, yumurta, un"),
                    SefimProductItem("2", "Fıstıklı Soğuk Baklava", "250 ₺", "Gaziantep Boz Fıstık", "Süt Şerbetli", "4 Dilim", "Baklava yufkası, un, süt, tereyağ, antep fıstık"),
                    SefimProductItem("3", "Magnolia (Çilek & Muz)", "165 ₺", "Bebe Bisküvili", "Fındık Parçalı", "Özel Krema", "Süt, krema, yumurta, bisküvi, fındık"),
                    SefimProductItem("4", "Profiterol Kulesi", "180 ₺", "Bitter Çikolata", "Krem Şanti", "Fıstık Tozlu", "Un, yumurta, tereyağ, süt, fıstık"),
                    SefimProductItem("5", "Maraş Kesme Dondurma", "140 ₺", "Keçi Sütlü", "Badem & Fıstıklı", "Külah İle", "Keçi sütü, salep, badem, fıstık")
                )
            }
        }

        sefimProducts.clear()
        for (prod in items) {
            val cached = productMetaCache[prod.productName]
            if (cached != null) {
                if (!cached.imageBase64.isNullOrEmpty()) prod.imageBase64 = cached.imageBase64
                if (cached.recipeContent.isNotEmpty()) prod.recipeContent = cached.recipeContent
                if (cached.manualAllergen.isNotEmpty()) prod.manualAllergen = cached.manualAllergen
            }
            sefimProducts.add(prod)
        }

        saveLocalSefimState()
        pushSefimBackupToMenuboard()
        sefimAdapter.notifyDataSetChanged()
        Toast.makeText(this, "${items.size} örnek ürün, reçete ve alerjenleriyle yüklendi!", Toast.LENGTH_LONG).show()
    }

    // --- Ürün İçeriği (Reçete) ve Manuel Alerjen Düzenleme Penceresi ---

    private fun showEditRecipeAndAllergenDialog(item: SefimProductItem) {
        val edtRecipe = EditText(this).apply {
            hint = "Reçete / İçerik (Örn: Un, tereyağ, süt, karides, ceviz...)"
            setText(item.recipeContent)
            minLines = 2
        }
        val edtManualAllergen = EditText(this).apply {
            hint = "Manuel Alerjen Ekle (Opsiyonel)"
            setText(item.manualAllergen)
        }
        val txtPreview = TextView(this).apply {
            setPadding(0, 16, 0, 8)
            setTextColor(Color.parseColor("#D32F2F"))
            typeface = Typeface.DEFAULT_BOLD
            val currentWarn = computeAllergenWarning(item)
            text = if (currentWarn.isNotEmpty()) "⚠️ Tespit Edilen Alerjen: $currentWarn" else "✓ Alerjen tespit edilmedi (Uyarı yazılmaz)"
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val temp = item.copy(
                    recipeContent = edtRecipe.text.toString(),
                    manualAllergen = edtManualAllergen.text.toString()
                )
                val warn = computeAllergenWarning(temp)
                txtPreview.text = if (warn.isNotEmpty()) "⚠️ Tespit Edilen Alerjen: $warn" else "✓ Alerjen tespit edilmedi (Uyarı yazılmaz)"
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        edtRecipe.addTextChangedListener(watcher)
        edtManualAllergen.addTextChangedListener(watcher)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
            addView(TextView(this@MainActivity).apply { text = "Ürün İçeriği / Reçete Malzemeleri:" })
            addView(edtRecipe)
            addView(TextView(this@MainActivity).apply {
                text = "Ek Manuel Alerjen Uyarısı (İsteğe Bağlı):"
                setPadding(0, 14, 0, 0)
            })
            addView(edtManualAllergen)
            addView(txtPreview)
        }

        AlertDialog.Builder(this)
            .setTitle("${item.productName} - İçerik & Alerjen")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                item.recipeContent = edtRecipe.text.toString().trim()
                item.manualAllergen = edtManualAllergen.text.toString().trim()

                val meta = productMetaCache.getOrPut(item.productName) { ProductMetaCache() }
                meta.recipeContent = item.recipeContent
                meta.manualAllergen = item.manualAllergen

                saveLocalSefimState()
                pushSefimBackupToMenuboard()
                sefimAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // --- ⚙️ AYAR BUTONU MENÜSÜ: MOUSE & KLAVYE KONTROLÜ, UZAKTAN APK VE UYGULAMA BAŞLATMA ---

    private fun showDeviceAdvancedMenu(device: DeviceItem) {
        val options = arrayOf(
            "🖱️ Mouse & Klavye Kontrolü (Sanal Touchpad)",
            "📦 Uzaktan APK Gönder & Kur",
            "🚀 Menuboard'da Kurulu Uygulamaları Gör / Başlat",
            "📺 Menuboard Uygulamasını Tekrar Öne Getir"
        )
        AlertDialog.Builder(this)
            .setTitle("${device.nameTag} (${device.host}) Yönetimi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showVirtualTouchpadAndKeyboardDialog(device)
                    1 -> {
                        targetApkDevice = device
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        pickRemoteApkLauncher.launch(intent)
                    }
                    2 -> fetchAndShowInstalledApps(device)
                    3 -> sendRemoteInput(device, "type=nav&act=MENUBOARD")
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    // Sanal Touchpad (Mouse) ve Uzaktan Klavye Kontrol Penceresi
    private fun showVirtualTouchpadAndKeyboardDialog(device: DeviceItem) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 24, 30, 16)
        }

        val infoTxt = TextView(this).apply {
            text = "Parmağınızı gri alanda gezdirerek Mouse'u hareket ettirin, tıklamak için dokunun."
            textSize = 11f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 12)
        }
        root.addView(infoTxt)

        // Touchpad Dokunmatik Alanı
        val touchpad = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 460)
            setBackgroundColor(Color.parseColor("#263238"))
        }
        val padHint = TextView(this).apply {
            text = "🖱️ TOUCHPAD ALANI\n(Kaydır: İmleç | Dokun: Sol Tık)"
            setTextColor(Color.parseColor("#80CBC4"))
            gravity = Gravity.CENTER
            textSize = 13f
        }
        touchpad.addView(padHint, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var lastX = 0f
        var lastY = 0f
        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var lastSendTime = 0L

        touchpad.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.x
                    lastY = ev.y
                    downX = ev.x
                    downY = ev.y
                    downTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_MOVE -> {
                    val now = System.currentTimeMillis()
                    val dx = (ev.x - lastX) * 1.9f
                    val dy = (ev.y - lastY) * 1.9f
                    if (now - lastSendTime > 28L && (kotlin.math.abs(dx) > 1.5f || kotlin.math.abs(dy) > 1.5f)) {
                        lastX = ev.x
                        lastY = ev.y
                        lastSendTime = now
                        sendRemoteInput(device, "type=move&dx=${dx.toInt()}&dy=${dy.toInt()}")
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val dist = hypot((ev.x - downX).toDouble(), (ev.y - downY).toDouble())
                    if (System.currentTimeMillis() - downTime < 260L && dist < 18.0) {
                        sendRemoteInput(device, "type=click")
                    }
                }
            }
            true
        }
        root.addView(touchpad)

        // Navigasyon ve Kaydırma Butonları
        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 8)
        }
        val btnBack = Button(this).apply {
            text = "◀ Geri"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, 95, 1f)
            setOnClickListener { sendRemoteInput(device, "type=nav&act=BACK") }
        }
        val btnHome = Button(this).apply {
            text = "⏺ Ana Ekran"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, 95, 1.1f)
            setOnClickListener { sendRemoteInput(device, "type=nav&act=HOME") }
        }
        val btnScrollUp = Button(this).apply {
            text = "▲ Kaydır"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, 95, 1f)
            setOnClickListener { sendRemoteInput(device, "type=scroll&dy=-350") }
        }
        val btnScrollDown = Button(this).apply {
            text = "▼ Kaydır"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, 95, 1f)
            setOnClickListener { sendRemoteInput(device, "type=scroll&dy=350") }
        }
        navRow.addView(btnBack)
        navRow.addView(btnHome)
        navRow.addView(btnScrollUp)
        navRow.addView(btnScrollDown)
        root.addView(navRow)

        // Klavye Gönderim Satırı
        val kbRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val edtRemoteKb = EditText(this).apply {
            hint = "Menuboard'a yazılacak metin..."
            textSize = 13f
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnSendText = Button(this).apply {
            text = "⌨️ Gönder"
            textSize = 11f
            setOnClickListener {
                val txt = edtRemoteKb.text.toString()
                if (txt.isNotEmpty()) {
                    sendRemoteInput(device, "type=text&val=${Uri.encode(txt)}")
                    edtRemoteKb.setText("")
                } else {
                    // Klavyeyi Aç
                    edtRemoteKb.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(edtRemoteKb, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
        val btnBackspace = Button(this).apply {
            text = "⌫"
            textSize = 13f
            setOnClickListener { sendRemoteInput(device, "type=key&code=BACKSPACE") }
        }
        kbRow.addView(edtRemoteKb)
        kbRow.addView(btnSendText)
        kbRow.addView(btnBackspace)
        root.addView(kbRow)

        val btnOpenAcc = Button(this).apply {
            text = "⚠️ Mouse Çalışmıyorsa Menuboard'da İzin Ekranını Aç"
            textSize = 10f
            setOnClickListener {
                sendRemoteInput(device, "type=nav&act=ACCESSIBILITY_SETTINGS")
                Toast.makeText(this@MainActivity, "Menuboard ekranında Erişilebilirlik ayarları açıldı. 'Anypay Uzaktan Mouse' servisini aktif edin.", Toast.LENGTH_LONG).show()
            }
        }
        root.addView(btnOpenAcc)

        AlertDialog.Builder(this)
            .setTitle("🖱️ ${device.nameTag} - Mouse & Klavye")
            .setView(root)
            .setPositiveButton("Kapat", null)
            .show()
    }

    private fun sendRemoteInput(device: DeviceItem, queryParams: String) {
        val req = Request.Builder()
            .url("http://${device.host}/api/input?$queryParams")
            .post("".toRequestBody(null))
            .build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
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
                    Toast.makeText(this@MainActivity, "Cihaza ulaşılamadı!", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "[]"
                response.close()
                try {
                    val type = object : TypeToken<List<RemoteAppInfo>>() {}.type
                    val apps: List<RemoteAppInfo> = gson.fromJson(body, type)
                    if (apps.isEmpty()) {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Uygulama listesi alınamadı.", Toast.LENGTH_LONG).show() }
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

    // --- ÇİFT YÖNLÜ ŞEFİM YEDEKLEME VE KURTARMA ---

    private fun saveLocalSefimState() {
        val dbPrefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val payload = SefimBackupPayload(
            dbServer = dbPrefs.getString("db_server", "192.168.2.167\\YIRMIBES") ?: "",
            dbName = dbPrefs.getString("db_name", "BALIKCISUKRU") ?: "",
            dbUser = dbPrefs.getString("db_user", "sa") ?: "",
            dbPass = dbPrefs.getString("db_pass", "") ?: "",
            restaurantName = edtRestaurantName.text.toString().trim(),
            headerNote = edtHeaderNote.text.toString().trim(),
            footerNote = edtFooterNote.text.toString().trim(),
            pageDurationSec = edtMenuPageDuration.text.toString().toIntOrNull() ?: 12,
            showChoice1 = cbShowChoice1.isChecked,
            showChoice2 = cbShowChoice2.isChecked,
            showOptions = cbShowOptions.isChecked,
            themeIndex = spinnerSefimTheme.selectedItemPosition,
            fontSizeIndex = spinnerSefimFontSize.selectedItemPosition,
            isBold = cbSefimBold.isChecked,
            bgObjectMode = bgObjectValues.getOrElse(spinnerSefimBgObject.selectedItemPosition) { "none" },
            products = sefimProducts,
            metaCacheByName = productMetaCache
        )
        dbPrefs.edit().putString("sefim_full_backup_json", gson.toJson(payload)).apply()
    }

    private fun loadLocalSefimState() {
        val dbPrefs = getSharedPreferences("sefim_db_prefs", Context.MODE_PRIVATE)
        val json = dbPrefs.getString("sefim_full_backup_json", null) ?: return
        try {
            val payload = gson.fromJson(json, SefimBackupPayload::class.java)
            edtRestaurantName.setText(payload.restaurantName)
            edtHeaderNote.setText(payload.headerNote)
            edtFooterNote.setText(payload.footerNote)
            edtMenuPageDuration.setText(payload.pageDurationSec.toString())
            cbShowChoice1.isChecked = payload.showChoice1
            cbShowChoice2.isChecked = payload.showChoice2
            cbShowOptions.isChecked = payload.showOptions

            productMetaCache.clear()
            productMetaCache.putAll(payload.metaCacheByName)

            sefimProducts.clear()
            sefimProducts.addAll(payload.products)
            sefimProducts.forEach { prod ->
                val meta = productMetaCache[prod.productName]
                if (meta != null) {
                    if (prod.imageBase64.isNullOrEmpty()) prod.imageBase64 = meta.imageBase64
                    if (prod.recipeContent.isEmpty()) prod.recipeContent = meta.recipeContent
                    if (prod.manualAllergen.isEmpty()) prod.manualAllergen = meta.manualAllergen
                }
            }
        } catch (_: Exception) {}
    }

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

    private fun syncSefimBackupWithMenuboard() {
        val host = activeDevice?.host ?: return
        val req = Request.Builder().url("http://$host/api/sefim/backup").build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val remoteJson = response.body?.string()?.trim() ?: ""
                response.close()

                if (remoteJson.isNotEmpty() && sefimProducts.isEmpty()) {
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

                        productMetaCache.putAll(payload.metaCacheByName)
                        sefimProducts.clear()
                        sefimProducts.addAll(payload.products)

                        runOnUiThread {
                            edtRestaurantName.setText(payload.restaurantName)
                            edtHeaderNote.setText(payload.headerNote)
                            edtFooterNote.setText(payload.footerNote)
                            edtMenuPageDuration.setText(payload.pageDurationSec.toString())
                            sefimAdapter.notifyDataSetChanged()
                            Toast.makeText(this@MainActivity, "Şefim ürünleri ve önbellek Menuboard'dan geri yüklendi!", Toast.LENGTH_LONG).show()
                        }
                    } catch (_: Exception) {}
                } else if (remoteJson.isEmpty() && sefimProducts.isNotEmpty()) {
                    pushSefimBackupToMenuboard()
                }
            }
        })
    }

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
            Toast.makeText(this, "Lütfen önce ⚙️ DB Ayar butonundan bilgileri girin!", Toast.LENGTH_LONG).show()
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

                val previousSelectionMap = sefimProducts.associate { it.productName to it.isSelected }

                while (rs.next()) {
                    val id = getColString(rs, meta, listOf("Id", "ProductId", "RECNO", "Kod"))
                    val name = getColString(rs, meta, listOf("ProductName", "Name", "UrunAdi", "Header", "Description"))
                    val rawPrice = getColString(rs, meta, listOf("Price", "Price1", "Fiyat", "SatisFiyati", "Amount"))

                    val inlineC1 = getColString(rs, meta, listOf("Choice1", "Choice1Name", "Secenek1"))
                    val inlineC2 = getColString(rs, meta, listOf("Choice2", "Choice2Name", "Secenek2"))
                    val inlineOpt = getColString(rs, meta, listOf("Option", "Options", "OptionName", "Opsiyon"))

                    val c1List = mutableListOf<String>()
                    if (inlineC1.isNotEmpty()) c1List.add(inlineC1)
                    choice1Map[id]?.let { c1List.addAll(it) }

                    val c2List = mutableListOf<String>()
                    if (inlineC2.isNotEmpty()) c2List.add(inlineC2)
                    choice2Map[id]?.let { c2List.addAll(it) }

                    val optList = mutableListOf<String>()
                    if (inlineOpt.isNotEmpty()) optList.add(inlineOpt)
                    optionsMap[id]?.let { optList.addAll(it) }

                    if (name.isNotEmpty()) {
                        val formattedPrice = if (rawPrice.isNotEmpty()) "$rawPrice ₺" else ""
                        val cachedMeta = productMetaCache[name]
                        val keepSelected = previousSelectionMap[name] ?: true

                        fetchedList.add(
                            SefimProductItem(
                                id = id,
                                productName = name,
                                price = formattedPrice,
                                choice1Text = c1List.distinct().joinToString(", "),
                                choice2Text = c2List.distinct().joinToString(", "),
                                optionsText = optList.distinct().joinToString(", "),
                                recipeContent = cachedMeta?.recipeContent ?: "",
                                manualAllergen = cachedMeta?.manualAllergen ?: "",
                                isSelected = keepSelected,
                                imageBase64 = cachedMeta?.imageBase64
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
                    Toast.makeText(this, "${fetchedList.size} ürün getirildi!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Veritabanı Bağlantı Hatası")
                        .setMessage("Hata: ${e.localizedMessage}\n\n'🧪 Test Menüleri' butonuna basarak hazır balık, döner, cafe veya tatlı menülerini hemen deneyebilirsiniz.")
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

    // 10 Modern Tema İçin Renk Paleti (Arka Plan, Ana Yazı, Vurgu/Fiyat)
    private fun getSelectedThemeColors(): Triple<Int, Int, Int> {
        return when (spinnerSefimTheme.selectedItemPosition) {
            1 -> Triple(Color.parseColor("#FAFAFA"), Color.parseColor("#212121"), Color.parseColor("#D32F2F")) // Modern Beyaz
            2 -> Triple(Color.parseColor("#1B0505"), Color.parseColor("#FFF8E1"), Color.parseColor("#FF5252")) // Restoran Ateşi
            3 -> Triple(Color.parseColor("#0A0E27"), Color.parseColor("#E0F7FA"), Color.parseColor("#00E5FF")) // Neon Cyberpunk
            4 -> Triple(Color.parseColor("#041E3A"), Color.parseColor("#E1F5FE"), Color.parseColor("#29B6F6")) // Okyanus Derinliği
            5 -> Triple(Color.parseColor("#0D2618"), Color.parseColor("#E8F5E9"), Color.parseColor("#69F0AE")) // Zümrüt Bahçesi
            6 -> Triple(Color.parseColor("#2B0912"), Color.parseColor("#FFF3E0"), Color.parseColor("#FFC107")) // Osmanlı Kehribar
            7 -> Triple(Color.parseColor("#FFF0F5"), Color.parseColor("#4A1525"), Color.parseColor("#D81B60")) // Paris Pastanesi
            8 -> Triple(Color.parseColor("#26140C"), Color.parseColor("#FFF8F0"), Color.parseColor("#FF8A65")) // İtalyan Trattoria
            9 -> Triple(Color.parseColor("#1E272C"), Color.parseColor("#ECEFF1"), Color.parseColor("#80CBC4")) // Minimalist Slate
            else -> Triple(Color.parseColor("#121212"), Color.parseColor("#FFFFFF"), Color.parseColor("#FFD700")) // Koyu Lüks Gold
        }
    }

    private fun applySefimPreviewBackground() {
        val (bgColor, _, _) = getSelectedThemeColors()
        rvSefimProducts.setBackgroundColor(bgColor)
    }

    // --- ÇOK SAYFALI, REÇETELİ VE ALERJEN UYARILI MENÜ ÇİZİMİ ---

    private fun publishSefimMenuToMenuboard() {
        val host = activeDevice?.host
        if (host.isNullOrEmpty()) {
            Toast.makeText(this, "Lütfen önce Cihazlar sekmesinden bir Menuboard seçin!", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedItems = sefimProducts.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Liste boş! Lütfen '🧪 Test Menüleri' butonundan örnek bir menü seçin.", Toast.LENGTH_LONG).show()
            return
        }

        val restName = edtRestaurantName.text.toString().trim().ifEmpty { "MENÜ" }
        val topInfo = edtHeaderNote.text.toString().trim()
        val bottomInfo = edtFooterNote.text.toString().trim()
        val pageDurSec = (edtMenuPageDuration.text.toString().toIntOrNull() ?: 12).coerceAtLeast(3)

        saveLocalSefimState()
        pushSefimBackupToMenuboard()

        val bgMode = bgObjectValues[spinnerSefimBgObject.selectedItemPosition]
        httpClient.newCall(
            Request.Builder().url("http://$host/api/sefim/bgobject?mode=$bgMode").post("".toRequestBody(null)).build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })

        Toast.makeText(this, "Menü sayfaları hazırlanıyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show()

        Thread {
            val width = 1080
            val height = 1920
            val (bgColor, textColor, accentColor) = getSelectedThemeColors()
            val isBold = cbSefimBold.isChecked
            val baseFontPt = fontSizeValues[spinnerSefimFontSize.selectedItemPosition]
            val titleSizePx = baseFontPt * 2.0f
            val subSizePx = baseFontPt * 1.25f

            val startTopY = if (topInfo.isNotEmpty()) 245f else 200f
            val maxBottomY = if (bottomInfo.isNotEmpty()) height - 150f else height - 90f

            val pages = mutableListOf<List<SefimProductItem>>()
            var currentPageItems = mutableListOf<SefimProductItem>()
            var testY = startTopY

            for (item in selectedItems) {
                val hasImg = !(item.imageBase64 ?: productMetaCache[item.productName]?.imageBase64).isNullOrEmpty()
                val choicesStr = buildFilteredChoicesString(item)
                val allergenStr = computeAllergenWarning(item)
                var extraLinesHeight = 0f
                if (choicesStr.isNotEmpty()) extraLinesHeight += subSizePx + 12f
                if (item.recipeContent.isNotEmpty()) extraLinesHeight += subSizePx + 10f
                if (allergenStr.isNotEmpty()) extraLinesHeight += subSizePx + 10f

                val rowHeight = maxOf(if (hasImg) 145f else 0f, titleSizePx + extraLinesHeight + 48f)

                if (testY + rowHeight > maxBottomY && currentPageItems.isNotEmpty()) {
                    pages.add(currentPageItems)
                    currentPageItems = mutableListOf()
                    testY = startTopY
                }
                currentPageItems.add(item)
                testY += rowHeight
            }
            if (currentPageItems.isNotEmpty()) {
                pages.add(currentPageItems)
            }

            playlist.removeAll { it.fileName.startsWith("sefim_menu_") }

            val batchTime = System.currentTimeMillis()
            val totalPages = pages.size

            for ((pageIdx, pageItems) in pages.withIndex()) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(bgColor)

                val paintRestName = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 58f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                val paintHeaderInfo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    alpha = 220
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                }

                val paintFooterInfo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                val paintPageBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    alpha = 180
                    textSize = 26f
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

                val paintRecipe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor
                    alpha = 190
                    textSize = subSizePx * 0.92f
                }

                val paintAllergen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF5252")
                    textSize = subSizePx * 0.92f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    alpha = 75
                    strokeWidth = 2f
                }

                canvas.drawText(restName, width / 2f, 92f, paintRestName)
                if (totalPages > 1) {
                    canvas.drawText("Sayfa ${pageIdx + 1}/$totalPages", width - 60f, 92f, paintPageBadge)
                }

                var headerLineY = 122f
                if (topInfo.isNotEmpty()) {
                    canvas.drawText(topInfo, width / 2f, 140f, paintHeaderInfo)
                    headerLineY = 166f
                }
                canvas.drawLine(60f, headerLineY, width - 60f, headerLineY, paintLine)

                var currentY = startTopY
                for (item in pageItems) {
                    var textStartX = 65f
                    val imgB64 = item.imageBase64 ?: productMetaCache[item.productName]?.imageBase64

                    if (!imgB64.isNullOrEmpty()) {
                        try {
                            val bytes = Base64.decode(imgB64, Base64.DEFAULT)
                            val prodBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (prodBmp != null) {
                                val dstRect = RectF(65f, currentY - 30f, 175f, currentY + 80f)
                                canvas.drawBitmap(prodBmp, null, dstRect, null)
                                textStartX = 200f
                            }
                        } catch (_: Exception) {}
                    }

                    canvas.drawText(item.productName, textStartX, currentY + 12f, paintTitle)
                    canvas.drawText(item.price, width - 65f, currentY + 12f, paintPrice)

                    var lineOffsetY = currentY + 12f

                    val choicesStr = buildFilteredChoicesString(item)
                    if (choicesStr.isNotEmpty()) {
                        lineOffsetY += subSizePx + 12f
                        canvas.drawText("↳ $choicesStr", textStartX + 10f, lineOffsetY, paintSub)
                    }

                    if (item.recipeContent.isNotEmpty()) {
                        lineOffsetY += subSizePx + 10f
                        canvas.drawText("İçindekiler: ${item.recipeContent}", textStartX + 10f, lineOffsetY, paintRecipe)
                    }

                    val allergenWarn = computeAllergenWarning(item)
                    if (allergenWarn.isNotEmpty()) {
                        lineOffsetY += subSizePx + 10f
                        canvas.drawText("⚠️ Alerjen Uyarısı: $allergenWarn", textStartX + 10f, lineOffsetY, paintAllergen)
                    }

                    val usedHeight = maxOf(if (!imgB64.isNullOrEmpty()) 125f else 0f, (lineOffsetY - currentY) + 25f)
                    currentY += usedHeight
                    canvas.drawLine(65f, currentY, width - 65f, currentY, paintLine)
                    currentY += 32f
                }

                if (bottomInfo.isNotEmpty()) {
                    canvas.drawLine(60f, height - 115f, width - 60f, height - 115f, paintLine)
                    canvas.drawText(bottomInfo, width / 2f, height - 65f, paintFooterInfo)
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
                                    durationSec = pageDurSec,
                                    waitAfterSec = 0,
                                    animation = "fade"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            runOnUiThread {
                Toast.makeText(this, "Toplam $totalPages sayfa menü ($pageDurSec sn geçişli) yayınlandı!", Toast.LENGTH_LONG).show()
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
            val txtAllergen: TextView = v.findViewById(R.id.txtProductAllergen)
            val txtPrice: TextView = v.findViewById(R.id.txtProductPrice)
            val btnEditRecipe: Button = v.findViewById(R.id.btnEditRecipe)
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

            val cachedB64 = item.imageBase64 ?: productMetaCache[item.productName]?.imageBase64
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
                                productMetaCache[item.productName]?.imageBase64 = null
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

            val filteredSub = buildFilteredChoicesString(item)
            val fullSubText = buildString {
                if (filteredSub.isNotEmpty()) append("↳ $filteredSub")
                if (item.recipeContent.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("🍽️ İçerik: ${item.recipeContent}")
                }
            }

            if (fullSubText.isNotEmpty()) {
                holder.txtChoices.visibility = View.VISIBLE
                holder.txtChoices.text = fullSubText
                holder.txtChoices.setTextColor(accentColor)
                holder.txtChoices.textSize = (fontSize - 4f).coerceAtLeast(10f)
            } else {
                holder.txtChoices.visibility = View.GONE
            }

            // Otomatik + Manuel Alerjen Gösterimi
            val allergenWarning = computeAllergenWarning(item)
            if (allergenWarning.isNotEmpty()) {
                holder.txtAllergen.visibility = View.VISIBLE
                holder.txtAllergen.text = "⚠️ Alerjen: $allergenWarning"
            } else {
                holder.txtAllergen.visibility = View.GONE
            }

            holder.btnEditRecipe.setOnClickListener {
                showEditRecipeAndAllergenDialog(item)
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
            val btnDeviceSettings: Button = v.findViewById(R.id.btnDeviceSettings)
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
