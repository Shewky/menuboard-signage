package com.anypay.menuboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isPackageReplaced = action == Intent.ACTION_MY_PACKAGE_REPLACED
        val isBoot = action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"

        if (!isPackageReplaced && !isBoot) return

        val prefs = context.getSharedPreferences("menuboard_prefs", Context.MODE_PRIVATE)
        val autoStart = prefs.getBoolean("auto_start_on_boot", false)

        if (isPackageReplaced || autoStart) {
            try {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                context.startActivity(launchIntent)
            } catch (_: Exception) {}
        }
    }
}
