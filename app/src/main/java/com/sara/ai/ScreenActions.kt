package com.sara.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Pending {
    private var action: (() -> String)? = null
    private var until = 0L
    fun set(a: () -> String) { action = a; until = System.currentTimeMillis() + 30000 }
    fun has() = action != null && System.currentTimeMillis() < until
    fun clear() { action = null; until = 0 }
    fun confirm(): String { val a = action; clear(); return a?.invoke() ?: "Koi pending kaam nahi hai." }
}

object ScreenActions {
    fun needAcc(ctx: Context): String {
        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Screen control ke liye Accessibility ON karni hogi. Settings khol di hai, Sara AI chuno aur ON karo. " +
            "Android 13 ya upar par pehle App info mein teen dot menu se 'Allow restricted settings' karo."
    }

    fun scroll(s: SaraAccessibilityService, down: Boolean): String {
        val ok = s.scroll(down)
        return if (ok) (if (down) "Neeche scroll kiya." else "Upar scroll kiya.") else "Scroll nahi ho paya."
    }

    fun scrollEdge(s: SaraAccessibilityService, bottom: Boolean): String {
        val n = s.scrollEdge(bottom)
        return if (n == 0) "Scroll karne layak kuch nahi mila." else if (bottom) "Sabse neeche aa gayi." else "Sabse upar aa gayi."
    }

    fun swipe(s: SaraAccessibilityService, left: Boolean): String =
        if (s.swipeSide(left)) (if (left) "Left swipe kiya." else "Right swipe kiya.") else "Swipe nahi ho paya."

    fun global(s: SaraAccessibilityService, action: Int, msg: String): String {
        s.performGlobalAction(action); return msg
    }

    fun click(s: SaraAccessibilityService, name: String): String =
        if (s.clickText(name)) "\"$name\" dabaya." else "\"$name\" screen par nahi mila."

    private fun waitClick(s: SaraAccessibilityService, text: String, ms: Long = 4000): Boolean {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            if (s.clickText(text)) return true
            Thread.sleep(400)
        }
        return false
    }

    fun whatsapp(ctx: Context, contact: String, msg: String): String {
        val s = SaraAccessibilityService.instance ?: return needAcc(ctx)
        val pm = ctx.packageManager
        val pkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pm.getLaunchIntentForPackage(it) != null }
            ?: return "WhatsApp install nahi mila."
        val digits = contact.filter { it.isDigit() }
        if (digits.length >= 10) {
            val num = if (digits.length == 10) "91$digits" else digits
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num?text=${Uri.encode(msg)}"))
                .setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(3500)
        } else {
            ctx.startActivity(pm.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(3000)
            if (!waitClick(s, "Search")) return "WhatsApp mein search button nahi mila. Chat khud kholo ya number ke saath bolo."
            Thread.sleep(800)
            s.typeText(contact)
            Thread.sleep(1800)
            val res = s.collect { it.text?.toString().equals(contact, true) && !it.isEditable }.firstOrNull()
                ?: s.collect { it.text?.toString()?.contains(contact, true) == true && !it.isEditable }.firstOrNull()
                ?: return "\"$contact\" naam ka contact nahi mila. Naam English mein ya number ke saath bolo."
            s.clickNode(res)
            Thread.sleep(2000)
            s.typeText(msg)
            Thread.sleep(800)
        }
        Pending.set {
            if (s.clickText("Send")) "Message bhej diya." else "Send button nahi mila."
        }
        return "$contact ko message likha hai: \"$msg\". Bhejun? Haan bolo."
    }
}
