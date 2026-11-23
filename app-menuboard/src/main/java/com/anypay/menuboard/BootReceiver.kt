package com.anypay.menuboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val autoStart = try {
                val prefs = context.getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
                prefs.getBoolean("auto_start_on_boot", false)
            } catch (_: Exception) {
                false
            }
            if (!autoStart) return

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

            // 1. Doğrudan başlatmayı dene
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {}

            // 2. Android 10+ arka plan kısıtlamasına karşı Full-Screen Intent ile garantiye al
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val chId = "menuboard_boot_ch"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    nm.createNotificationChannel(
                        NotificationChannel(chId, "Menuboard Otomatik Başlatma", NotificationManager.IMPORTANCE_HIGH)
                    )
                }
                val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pi = PendingIntent.getActivity(context, 1001, launchIntent, piFlags)
                val notif = NotificationCompat.Builder(context, chId)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("Menuboard Yayın Sistemi")
                    .setContentText("Otomatik başlatılıyor...")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pi, true)
                    .setAutoCancel(true)
                    .build()
                nm.notify(1001, notif)
            } catch (_: Exception) {}
        }
    }
}
