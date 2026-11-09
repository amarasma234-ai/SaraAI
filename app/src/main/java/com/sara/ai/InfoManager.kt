package com.sara.ai

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

object InfoManager {
    private fun gb(b: Long) = String.format("%.1f", b / 1073741824.0)

    fun battery(ctx: Context): String {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return "Battery info nahi mili."
        val lvl = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0
        val health = when (i.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "achhi"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "kharab"
            BatteryManager.BATTERY_HEALTH_COLD -> "bahut thandi"
            else -> "pata nahi"
        }
        return "Battery $lvl percent hai, ${if (charging) "charging ho rahi hai" else "charging nahi ho rahi"}. " +
            "Temperature $temp°C, health $health."
    }

    fun storage(ctx: Context): String {
        val s = StatFs(Environment.getDataDirectory().path)
        val total = s.totalBytes; val free = s.availableBytes
        return "Internal storage ${gb(total)} GB hai, ${gb(free)} GB khali, ${gb(total - free)} GB use ho chuka hai."
    }

    fun network(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            ?: return "Abhi koi network connected nahi hai."
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
        val net = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return "Tum $type se connected ho, internet ${if (net) "chal raha hai" else "nahi chal raha"}."
    }

    fun device(ctx: Context): String {
        val mi = ActivityManager.MemoryInfo()
        (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val up = SystemClock.elapsedRealtime() / 60000
        return "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}). " +
            "RAM ${gb(mi.totalMem)} GB, ${gb(mi.availMem)} GB free. Phone ${up / 60} ghante ${up % 60} minute se on hai."
    }
}
