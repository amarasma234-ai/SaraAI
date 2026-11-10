package com.sara.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

object Brain {
    private val history = ArrayDeque<Pair<String, String>>()
    private val yes = Regex("^(haan|han|ha|yes|yeah|ok|okay|theek hai|ji|ji haan|bhej do|bhejo|confirm|kar do)$")
    private val no = Regex("^(nahi|nahin|no|cancel|ruko|mat karo)$")
    private val stopRe = Regex("(?i)\\b(stop|ruko|ruk ja|band karo)\\b|रुको|रुक जा|बंद करो")

    private fun system(ctx: Context): String {
        val app = Prefs.get(ctx, "appLabel")
        return """
You are Sara: a sweet, caring, playful girlfriend-style AI assistant who also controls the user's Android phone like Jarvis.
Always reply in the SAME language the user spoke (Hindi/Hinglish, English, Bengali, Tamil, Telugu, Marathi, Gujarati, Punjabi, Urdu etc.). Keep replies short (1-2 warm sentences).
Output ONLY JSON: {"reply": string, "lang": BCP-47 tag of the reply language like "hi-IN","en-IN","bn-IN","ta-IN", "actions": [ ... ]}
Available actions (use several in order if needed):
{"cmd":"open_app","name":"youtube"}
{"cmd":"youtube_search","query":"..."}
{"cmd":"flashlight","state":"on|off|toggle"}
{"cmd":"volume","mode":"up|down|set|mute|unmute","value":0-100}
{"cmd":"brightness","mode":"up|down|set","value":0-100}
{"cmd":"info","what":"battery|storage|network|device"}
{"cmd":"scroll","dir":"down|up|top|bottom"}
{"cmd":"swipe","dir":"left|right"}
{"cmd":"click","text":"visible button text"}
{"cmd":"type","text":"..."}
{"cmd":"global","what":"back|home|recents|notifications|quick_settings"}
{"cmd":"first_result"}
{"cmd":"whatsapp","contact":"name or phone number","message":"..."}
{"cmd":"agent","goal":"..."}  <- for multi-step tasks that need looking at the screen and tapping several things (e.g. "open Instagram and search for X", "find a video about Y and play it"). The agent operates the phone by itself.
Rules: if the user only chats, actions=[]. For info questions use the info action and just say you are checking. Phone temperature, turning Wi-Fi/mobile data on or off, and payments cannot be done by you: say so honestly. Never do payments, deleting, or password entry.
User's currently selected app: ${if (app.isBlank()) "none" else app}.
""".trimIndent()
    }

    private fun isProblem(r: String) =
        r.contains("nahi") || r.contains("Error") || r.contains("Accessibility")

    fun process(ctx: Context, text: String, say: (String, String?, Boolean) -> Unit) {
        val app = ctx.applicationContext
        val main = Handler(Looper.getMainLooper())
        fun out(t: String, lang: String?, add: Boolean) { main.post { say(t, lang, add) } }
        Thread {
            try {
                val n = Normalizer.apply(text.lowercase().trim())
                if (Agent.running && stopRe.containsMatchIn(text.lowercase())) {
                    Agent.stop = true; out("Theek hai, ruk rahi hoon.", null, false); return@Thread
                }
                if (Pending.has() && yes.matches(n)) { out(Pending.confirm(), null, false); return@Thread }
                if (Pending.has() && no.matches(n)) { Pending.clear(); out("Theek hai, cancel kar diya.", null, false); return@Thread }
                if (Prefs.get(app, "apiKey").isBlank()) {
                    out(CommandRouter.handle(app, text), null, false); return@Thread
                }
                val contents = history.toList() + ("user" to text)
                val r = GeminiClient.ask(app, system(app), contents)
                if (r.text == null) {
                    val note = when (r.err) {
                        "network" -> "Internet nahi hai, local mode mein hoon. "
                        "badkey" -> "Gemini API key galat lagti hai. "
                        "rate" -> "Gemini ki free limit abhi khatam hai. "
                        else -> "Gemini se jawab nahi aaya. "
                    }
                    out(note + CommandRouter.handle(app, text), null, false); return@Thread
                }
                val j = GeminiClient.parseJson(r.text)
                if (j == null) { out(r.text.take(300), null, false); return@Thread }
                val reply = j.optString("reply")
                val lang = j.optString("lang").ifBlank { null }
                val acts = j.optJSONArray("actions") ?: JSONArray()
                history.addLast("user" to text); history.addLast("model" to reply)
                while (history.size > 12) history.removeFirst()

                val info = StringBuilder()
                for (i in 0 until acts.length()) {
                    val a = acts.getJSONObject(i)
                    if (a.optString("cmd") == "info") info.append(' ').append(exec(app, a))
                }
                out((reply + info).trim(), lang, false)

                val follow = StringBuilder()
                for (i in 0 until acts.length()) {
                    val a = acts.getJSONObject(i)
                    val cmd = a.optString("cmd")
                    if (cmd == "info") continue
                    val res = try { exec(app, a) } catch (e: Exception) { "Error: ${e.message}" }
                    if (cmd == "whatsapp" || cmd == "agent" || isProblem(res)) follow.append(res).append(' ')
                    if (i < acts.length() - 1)
                        Thread.sleep(if (cmd == "open_app" || cmd == "youtube_search") 2200L else 900L)
                }
                if (follow.isNotBlank()) out(follow.toString().trim(), lang, true)
            } catch (e: Exception) {
                out("Error: ${e.message}", null, false)
            }
        }.start()
    }

    private fun exec(ctx: Context, a: JSONObject): String {
        val v = a.optInt("value", -1)
        return when (a.optString("cmd")) {
            "open_app" -> AppLauncher.open(ctx, a.optString("name").lowercase())
            "youtube_search" -> AppLauncher.youtubeSearch(ctx, a.optString("query"))
            "flashlight" -> when (a.optString("state")) {
                "on" -> FlashlightManager.set(ctx, true)
                "off" -> FlashlightManager.set(ctx, false)
                else -> FlashlightManager.toggle(ctx)
            }
            "volume" -> when (a.optString("mode")) {
                "up" -> VolumeManager.change(ctx, true)
                "down" -> VolumeManager.change(ctx, false)
                "mute" -> VolumeManager.mute(ctx, true)
                "unmute" -> VolumeManager.mute(ctx, false)
                "set" -> if (v >= 0) VolumeManager.setPercent(ctx, v) else VolumeManager.info(ctx)
                else -> VolumeManager.info(ctx)
            }
            "brightness" -> when (a.optString("mode")) {
                "up" -> BrightnessManager.change(ctx, 15)
                "down" -> BrightnessManager.change(ctx, -15)
                "set" -> if (v >= 0) BrightnessManager.setPercent(ctx, v) else BrightnessManager.info(ctx)
                else -> BrightnessManager.info(ctx)
            }
            "info" -> when (a.optString("what")) {
                "battery" -> InfoManager.battery(ctx)
                "storage" -> InfoManager.storage(ctx)
                "network" -> InfoManager.network(ctx)
                "device" -> InfoManager.device(ctx)
                else -> ""
            }
            "scroll", "swipe", "click", "type", "global", "first_result" -> {
                val s = SaraAccessibilityService.instance ?: return ScreenActions.needAcc(ctx)
                when (a.optString("cmd")) {
                    "scroll" -> when (a.optString("dir")) {
                        "up" -> ScreenActions.scroll(s, false)
                        "top" -> ScreenActions.scrollEdge(s, false)
                        "bottom" -> ScreenActions.scrollEdge(s, true)
                        else -> ScreenActions.scroll(s, true)
                    }
                    "swipe" -> ScreenActions.swipe(s, a.optString("dir") == "left")
                    "click" -> {
                        val t = a.optString("text")
                        if (ScreenActions.isSensitive(t)) {
                            Pending.set { ScreenActions.click(s, t) }
                            "\"$t\" sensitive kaam ho sakta hai. Pakka dabaun? Haan bolo."
                        } else ScreenActions.click(s, t)
                    }
                    "type" -> if (s.typeText(a.optString("text"))) "Likh diya." else "Koi text box nahi mila."
                    "global" -> when (a.optString("what")) {
                        "home" -> ScreenActions.global(s, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME, "Home par aa gayi.")
                        "recents" -> ScreenActions.global(s, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS, "Recent apps khol diye.")
                        "notifications" -> ScreenActions.global(s, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Notifications khol di.")
                        "quick_settings" -> ScreenActions.global(s, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "Quick settings khol di.")
                        else -> ScreenActions.global(s, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK, "Peeche gayi.")
                    }
                    else -> if (s.clickFirstResult()) "Pehla result khol diya." else "Pehla result nahi mila."
                }
            }
            "whatsapp" -> ScreenActions.whatsapp(ctx, a.optString("contact"), a.optString("message"))
            "agent" -> Agent.run(ctx, a.optString("goal"), false)
            else -> ""
        }
    }
}
