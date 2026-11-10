package com.sara.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object Agent {
    @Volatile var running = false
    @Volatile var stop = false
    private val blocked = listOf("paisa", "phonepe", "paytm", "npci", "bank", "wallet", "upi", "bhim", "razorpay", "cred.club")

    private class El(val node: AccessibilityNodeInfo, val line: String, val label: String)

    private val SYS = """
You are the hands of an Android phone assistant. You see a list of visible UI elements and must reach the user's GOAL step by step.
Output ONLY JSON: {"thought":"short","action":"click|type|enter|scroll_down|scroll_up|swipe_left|swipe_right|back|home|open_app|wait|done|fail","index":number,"text":"text to type","app":"app name","say":"final short message to the user in the user's language"}
Rules: one action per step. Use "index" from the element list for click/type/enter. To search: click the search field/icon, then type, then enter (or click the search button). Use "done" with "say" as soon as the goal is achieved. Use "fail" with "say" if stuck. NEVER do payments, purchases, money transfers, deleting data, or type passwords. Prefer the selected app unless the goal names another.
""".trimIndent()

    private fun dump(s: SaraAccessibilityService): List<El> {
        val nodes = s.collect { n ->
            n.isVisibleToUser && (n.isClickable || n.isEditable || n.isScrollable ||
                !n.text.isNullOrBlank() || !n.contentDescription.isNullOrBlank())
        }.take(70)
        return nodes.mapIndexed { i, n ->
            val t = if (n.isPassword) "(password field)" else (n.text?.toString() ?: "").take(60)
            val d = (n.contentDescription?.toString() ?: "").take(60)
            val f = buildString {
                if (n.isClickable) append("click ")
                if (n.isEditable) append("edit ")
                if (n.isScrollable) append("scroll ")
            }.trim()
            El(n, "[$i] $f text=\"$t\" desc=\"$d\"", if (t.isNotBlank()) t else d)
        }
    }

    fun run(ctx: Context, goal: String, openSelected: Boolean, approved: String = ""): String {
        val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
        if (Prefs.get(ctx, "apiKey").isBlank()) return "Khud kaam karne ke liye Gemini API key daalni hogi."
        if (running) return "Main pehle se ek kaam kar rahi hoon. 'Sara stop' bolo."
        running = true; stop = false
        try {
            val pkg = Prefs.get(ctx, "appPkg")
            if (openSelected && pkg.isNotBlank()) {
                ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
                    ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); Thread.sleep(2500)
                }
            }
            val history = mutableListOf<String>()
            for (step in 1..14) {
                if (stop) return "Theek hai, ruk gayi."
                val cur = s.rootInActiveWindow?.packageName?.toString() ?: ""
                if (blocked.any { cur.contains(it, true) })
                    return "Ye payment ya bank app lagta hai. Suraksha ke liye isme main khud kaam nahi karti."
                val els = dump(s)
                val prompt = "GOAL: $goal\nSelected app: ${Prefs.get(ctx, "appLabel")}\nCurrent app package: $cur\n" +
                    "STEP $step of 14\nPREVIOUS ACTIONS:\n${history.takeLast(8).joinToString("\n")}\n" +
                    "SCREEN ELEMENTS:\n${els.joinToString("\n") { it.line }}"
                val r = GeminiClient.ask(ctx, SYS, listOf("user" to prompt))
                val j = r.text?.let { GeminiClient.parseJson(it) }
                    ?: return "Gemini se jawab nahi mila (${r.err ?: "format"}), kaam adhura hai."
                val act = j.optString("action")
                val idx = j.optInt("index", -1)
                val text = j.optString("text")
                val say = j.optString("say")
                val el = els.getOrNull(idx)
                when (act) {
                    "done" -> return say.ifBlank { "Kaam ho gaya." }
                    "fail" -> return say.ifBlank { "Mujhse ye nahi ho paya." }
                    "click" -> {
                        if (el == null) { history.add("$step: click bad index"); continue }
                        if (ScreenActions.isSensitive(el.label) && !el.label.equals(approved, true)) {
                            val lbl = el.label
                            Pending.set { run(ctx, goal, false, lbl) }
                            return "\"$lbl\" sensitive lagta hai. Pakka dabaun? Haan bolo."
                        }
                        s.clickNode(el.node); history.add("$step: click ${el.label}")
                    }
                    "type" -> {
                        if (el == null || el.node.isPassword) { history.add("$step: type refused"); continue }
                        el.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                        val b = Bundle()
                        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                        el.node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                        history.add("$step: typed \"$text\"")
                    }
                    "enter" -> {
                        if (el != null && Build.VERSION.SDK_INT >= 30)
                            el.node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                        history.add("$step: enter")
                    }
                    "scroll_down" -> { s.scroll(true); history.add("$step: scroll down") }
                    "scroll_up" -> { s.scroll(false); history.add("$step: scroll up") }
                    "swipe_left" -> { s.swipeSide(true); history.add("$step: swipe left") }
                    "swipe_right" -> { s.swipeSide(false); history.add("$step: swipe right") }
                    "back" -> { s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK); history.add("$step: back") }
                    "home" -> { s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME); history.add("$step: home") }
                    "open_app" -> { history.add("$step: open ${j.optString("app")} -> " + AppLauncher.open(ctx, j.optString("app").lowercase())); Thread.sleep(1500) }
                    else -> { history.add("$step: wait"); Thread.sleep(1200) }
                }
                Thread.sleep(1600)
            }
            return "Kaafi koshish ki, par poora nahi ho paya."
        } finally {
            running = false
        }
    }
}
