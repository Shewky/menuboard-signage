package com.anypay.menuboard

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException

class UpdateChecker(private val context: Context) {

    // Kendi GitHub kullanıcı adınızı ve repo adınızı buraya girin
    private val GITHUB_REPO = "KULLANICI_ADINIZ/menuboard-signage"
    private val httpClient = OkHttpClient()

    fun checkForUpdate(isMenuboard: Boolean) {
        val url = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return
                val json = JSONObject(response.body?.string() ?: "{}")
                
                // GitHub Release etiket formatı: "v1.01" -> "101"
                val tagName = json.optString("tag_name", "v1.00").replace("v", "").replace(".", "")
                val remoteVersionCode = tagName.toIntOrNull() ?: 100

                // Cihazdaki mevcut versionCode
                val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }

                if (remoteVersionCode > currentVersionCode) {
                    val assets = json.getJSONArray("assets")
                    var downloadUrl = ""
                    val targetApkName = if (isMenuboard) "menuboard" else "remote"

                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name").lowercase()
                        if (name.contains(targetApkName) && name.endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    if (downloadUrl.isNotEmpty()) {
                        (context as? android.app.Activity)?.runOnUiThread {
                            showUpdateDialog(json.optString("name", "v$remoteVersionCode"), downloadUrl)
                        }
                    }
                }
            }
        })
    }

    private fun showUpdateDialog(versionTitle: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Güncelleme Bulundu")
            .setMessage("Yeni bir sürüm yayınlandı ($versionTitle).\nŞimdi indirip yüklemek ister misiniz?")
            .setPositiveButton("Güncelle") { _, _ ->
                downloadAndInstall(downloadUrl)
            }
            .setNegativeButton("Daha Sonra", null)
            .setCancelable(false)
            .show()
    }

    private fun downloadAndInstall(url: String) {
        Toast.makeText(context, "Güncelleme indiriliyor...", Toast.LENGTH_SHORT).show()

        val fileName = "update_${System.currentTimeMillis()}.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Uygulama Güncellemesi")
            .setDescription("Yeni sürüm indiriliyor...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        // İndirme bittiğinde doğrudan APK yükleyicisini açan dinleyici
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(destination)
                }
            }
        }

        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
    }

    private fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}
