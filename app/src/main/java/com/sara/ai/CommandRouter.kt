package com.sara.ai

import android.content.Context

object CommandRouter {
    fun handle(ctx: Context, raw: String): String {
        val t = raw.lowercase().trim()
        fun has(vararg w: String) = w.any { t.contains(it) }
        val on = has(" on", "chalu", "jala", "enable")
        val off = has(" off", "band", "bujha", "disable")
        val up = has("badha", "increase", "up", "zyada", "tej")
        val down = has("kam", "decrease", "down", "ghata", "dhima")

        // Flashlight
        if (has("torch", "flashlight", "flash light", "tourch")) {
            return when {
                off -> FlashlightManager.set(ctx, false)
                on -> FlashlightManager.set(ctx, true)
                else -> FlashlightManager.toggle(ctx)
            }
        }
        // Volume
        if (has("volume", "awaaz", "awaz", "mute")) {
            val n = Regex("(\\d+)").find(t)?.value?.toIntOrNull()
            return when {
                has("unmute") -> VolumeManager.mute(ctx, false)
                has("mute") -> VolumeManager.mute(ctx, true)
                n != null -> VolumeManager.setPercent(ctx, n)
                up -> VolumeManager.change(ctx, true)
                down -> VolumeManager.change(ctx, false)
                else -> VolumeManager.info(ctx)
            }
        }
        // Brightness
        if (has("brightness", "britness", "brightnes")) {
            val n = Regex("(\\d+)").find(t)?.value?.toIntOrNull()
            return when {
                n != null -> BrightnessManager.setPercent(ctx, n)
                up -> BrightnessManager.change(ctx, 15)
                down -> BrightnessManager.change(ctx, -15)
                else -> BrightnessManager.info(ctx)
            }
        }
        // Info
        if (has("battery", "charge", "charging")) return InfoManager.battery(ctx)
        if (has("storage", "memory", "space")) return InfoManager.storage(ctx)
        if (has("wifi", "wi-fi", "network", "internet", "data")) return InfoManager.network(ctx)
        if (has("device", "model", "android version", "ram", "uptime")) return InfoManager.device(ctx)
        if (has("temperature", "temp", "garam")) return InfoManager.battery(ctx) +
            " (Phone ka temperature control Android allow nahi karta, sirf dekh sakti hoon.)"

        // YouTube search: "youtube pe arijit singh songs chalao"
        Regex("youtube\\s*(?:pe|par|me|mein|on)\\s+(.+?)(?:\\s+(?:chalao|chala do|play|search|lagao|dikhao))?$")
            .find(t)?.let { return AppLauncher.youtubeSearch(ctx, it.groupValues[1]) }

        // Open app
        if (has("open", "kholo", "khol", "chalao", "launch", "start")) {
            val name = t
                .replace(Regex("\\b(hey|hi|sara|please|open|kholo|khol do|khol|do|karo|chalao|launch|start|app|ko|ye|meri)\\b"), " ")
                .replace(Regex("\\s+"), " ").trim()
            if (name.isNotEmpty()) return AppLauncher.open(ctx, name)
        }
        return "Mujhe ye command samajh nahi aayi: \"$raw\". Try karo: youtube kholo, torch on, volume badhao, brightness 80, battery kitni hai."
    }
}
