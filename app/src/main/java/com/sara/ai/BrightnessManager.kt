package com.sara.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object BrightnessManager {
    private fun current(ctx: Context) =
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)

    fun info(ctx: Context) = "Brightness abhi ${current(ctx) * 100 / 255} percent hai."

    private fun needPermission(ctx: Context): String? {
        if (Settings.System.canWrite(ctx)) return null
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Brightness badalne ke liye Sara ko 'Modify system settings' permission chahiye. Settings khol di hai, ON kar do, phir dobara bolo."
    }

    fun setPercent(ctx: Context, p: Int): String {
        needPermission(ctx)?.let { return it }
        val pct = p.coerceIn(1, 100)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, pct * 255 / 100)
        return "Brightness $pct percent kar di."
    }

    fun change(ctx: Context, deltaPct: Int): String =
        setPercent(ctx, current(ctx) * 100 / 255 + deltaPct)
}
