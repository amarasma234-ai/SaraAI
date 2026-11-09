package com.sara.ai

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppLauncher {
    private val aliases = mapOf(
        "youtube" to "youtube", "yt" to "youtube",
        "whatsapp" to "whatsapp", "whats app" to "whatsapp", "what's app" to "whatsapp",
        "play store" to "play store", "playstore" to "play store",
        "google" to "google", "chrome" to "chrome", "crome" to "chrome",
        "instagram" to "instagram", "insta" to "instagram",
        "facebook" to "facebook", "fb" to "facebook",
        "settings" to "settings", "setting" to "settings"
    )

    private fun launchables(ctx: Context) = ctx.packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
    )

    fun open(ctx: Context, spoken: String): String {
        val key = aliases[spoken] ?: aliases.entries.firstOrNull { spoken.contains(it.key) }?.value ?: spoken
        val pm = ctx.packageManager
        val match = launchables(ctx).firstOrNull {
            it.loadLabel(pm).toString().lowercase() == key
        } ?: launchables(ctx).firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(key)
        } ?: return "\"$spoken\" naam ka app mujhe phone mein nahi mila. Install hai kya?"
        val i = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            ?: return "Is app ko kholne ka tareeka nahi mila."
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return "${match.loadLabel(pm)} khol rahi hoon."
    }

    fun youtubeSearch(ctx: Context, query: String): String {
        val i = Intent(Intent.ACTION_SEARCH).setPackage("com.google.android.youtube")
            .putExtra("query", query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(i); "YouTube par \"$query\" search kar rahi hoon."
        } catch (e: Exception) {
            val web = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(web); "YouTube app nahi mila, browser mein \"$query\" khol rahi hoon."
        }
    }
}
