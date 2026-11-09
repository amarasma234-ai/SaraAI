package com.sara.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper

object CommandRouter {
    private val main = Handler(Looper.getMainLooper())
    private val waRegex = Regex(
        "(?:whatsapp|whats app|व्हाट्सएप|व्हाट्सऐप|वाट्सएप|वॉट्सऐप|व्हाट्सप|व्हाट्स एप|वाट्सऐप)\\s*" +
        "(?:pe|par|me|mein|on|पे|पर|में)?\\s*(.+?)\\s+(?:ko|को)\\s+(.+?)\\s*(?:message|msg|मैसेज|text)?\\s*" +
        "(?:bhejo|bhej do|bhej|send|भेजो|भेज दो|भेज|likho|लिखो)$")
    private val splitRe = Regex("\\s+(?:and|aur|phir|fir|then|और|फिर)\\s+")
    private val sensitive = listOf("pay", "payment", "transfer", "send money", "delete", "uninstall", "remove",
        "buy", "purchase", "place order", "password", "reset", "log out", "logout", "paisa", "bhugtan", "पेमेंट", "डिलीट")

    private fun clean(raw: String) = raw.trim().trimEnd('.', '!', '?', '।')

    fun handleAsync(ctx: Context, raw: String, cb: (String) -> Unit) {
        val app = ctx.applicationContext
        Thread {
            val c = clean(raw)
            val parts = if (waRegex.containsMatchIn(c.lowercase())) listOf(c) else c.split(splitRe)
            val replies = mutableListOf<String>()
            for ((i, p) in parts.withIndex()) {
                replies.add(try { handle(app, p) } catch (e: Exception) { "Error: ${e.message}" })
                if (i < parts.size - 1) Thread.sleep(2200)
            }
            main.post { cb(replies.joinToString(" ")) }
        }.start()
    }

    fun handle(ctx: Context, raw: String): String {
        val c = clean(raw)
        waRegex.find(c.lowercase())?.let { m ->
            return ScreenActions.whatsapp(ctx, m.groupValues[1].trim(), m.groupValues[2].trim())
        }
        val t = Normalizer.apply(c.lowercase())
        fun has(vararg w: String) = w.any { t.contains(it) }
        fun w(r: String) = Regex("\\b$r\\b").containsMatchIn(t)
        val on = has(" on", "chalu", "jala", "enable")
        val off = has(" off", "band", "bujha", "disable")
        val up = has("badha", "increase", "up", "zyada", "tej")
        val down = has("kam", "decrease", "down", "ghata", "dhima")

        // Confirm / cancel pending action
        if (Pending.has() && Regex("^(haan|han|yes|ok|okay|theek hai|bhej do|bhejo|confirm)$").matches(t))
            return Pending.confirm()
        if (Pending.has() && Regex("^(nahi|no|cancel|ruko)$").matches(t)) {
            Pending.clear(); return "Theek hai, cancel kar diya."
        }

        // Scroll / swipe
        if (has("scroll", "swipe", "neeche", "niche", "upar") || w("top") || w("bottom")) {
            val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
            val left = w("left") || has("baayen", "bayen")
            val right = w("right") || has("daayen", "dayen")
            return when {
                has("bottom", "last tak", "end tak", "neeche tak", "niche tak") -> ScreenActions.scrollEdge(s, true)
                w("top") || has("shuru tak", "upar tak") -> ScreenActions.scrollEdge(s, false)
                has("swipe") || left || right ->
                    if (!left && !right) "Swipe left karun ya right?" else ScreenActions.swipe(s, left)
                has("upar") || w("up") -> ScreenActions.scroll(s, false)
                else -> ScreenActions.scroll(s, true)
            }
        }
        // Global buttons
        val isBack = w("back") || has("wapas", "peeche", "piche")
        val isHome = w("home")
        val isRecents = has("recent")
        val isQuick = has("quick settings", "quick setting")
        val isNotif = has("notification")
        if (isBack || isHome || isRecents || isQuick || isNotif) {
            val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
            return when {
                isQuick -> ScreenActions.global(s, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "Quick settings khol di.")
                isNotif -> ScreenActions.global(s, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Notifications khol di.")
                isRecents -> ScreenActions.global(s, AccessibilityService.GLOBAL_ACTION_RECENTS, "Recent apps khol diye.")
                isHome -> ScreenActions.global(s, AccessibilityService.GLOBAL_ACTION_HOME, "Home par aa gayi.")
                else -> ScreenActions.global(s, AccessibilityService.GLOBAL_ACTION_BACK, "Peeche gayi.")
            }
        }
        // First result
        if (has("first result", "pehla result", "pehla video", "first video", "pehla wala")) {
            val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
            return if (s.clickFirstResult()) "Pehla result khol diya." else "Pehla result nahi mila."
        }
        // Type text
        Regex("^(?:type|likho|write)\\s+(.+)").find(c.lowercase())?.let { m ->
            val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
            return if (s.typeText(m.groupValues[1])) "Likh diya." else "Koi text box nahi mila."
        }
        // Click
        if (has("click", "tap", "dabao", "select")) {
            val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
            val name = t.replace(Regex("\\b(click|tap|dabao|select|karo|kro|kar|do|pe|par|pr|on|the|button|ko)\\b"), " ")
                .replace(Regex("\\s+"), " ").trim()
            if (name.isBlank()) return "Kispe click karun?"
            if (sensitive.any { name.contains(it) }) {
                Pending.set { ScreenActions.click(s, name) }
                return "\"$name\" sensitive kaam ho sakta hai. Pakka dabaun? Haan bolo."
            }
            return ScreenActions.click(s, name)
        }

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

        // YouTube search
        Regex("youtube\\s*(?:pe|par|me|mein|on)\\s+(.+?)(?:\\s+(?:chalao|chala do|play|search|lagao|dikhao))?$")
            .find(t)?.let { return AppLauncher.youtubeSearch(ctx, it.groupValues[1]) }

        // Open app
        if (has("open", "kholo", "khol", "chalao", "launch", "start")) {
            val name = t
                .replace(Regex("\\b(hey|hi|sara|please|open|kholo|khol do|khol|do|karo|chalao|launch|start|app|ko|ye|meri)\\b"), " ")
                .replace(Regex("\\s+"), " ").trim()
            if (name.isNotEmpty()) return AppLauncher.open(ctx, name)
        }
        return "Mujhe ye command samajh nahi aayi: \"$raw\"."
    }
}
