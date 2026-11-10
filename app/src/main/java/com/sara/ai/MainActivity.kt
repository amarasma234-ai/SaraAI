package com.sara.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.Voice
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var speaker: Speaker
    private lateinit var log: TextView
    private lateinit var input: EditText
    private lateinit var appView: TextView
    private lateinit var voiceSpinner: Spinner
    private var voiceList: List<Voice> = emptyList()
    private val langs = listOf("Auto (sab bhasha)" to "auto", "हिन्दी" to "hi-IN", "English (India)" to "en-IN",
        "English (US)" to "en-US", "বাংলা" to "bn-IN", "தமிழ்" to "ta-IN", "తెలుగు" to "te-IN", "मराठी" to "mr-IN",
        "ગુજરાતી" to "gu-IN", "ਪੰਜਾਬੀ" to "pa-IN", "ಕನ್ನಡ" to "kn-IN", "മലയാളം" to "ml-IN", "اردو" to "ur-IN")

    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
    private fun head(t: String) = TextView(this).apply { text = t; textSize = 18f; setPadding(0, 36, 0, 8) }

    private fun seek(label: String, key: String, def: Float): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tv = TextView(this)
        val cur = Prefs.getF(this, key, def)
        tv.text = "$label: ${"%.2f".format(cur)}"
        val sb = SeekBar(this).apply { max = 150; progress = ((cur * 100) - 50).toInt().coerceIn(0, 150) }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                val v = (p + 50) / 100f
                Prefs.setF(this@MainActivity, key, v)
                tv.text = "$label: ${"%.2f".format(v)}"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        box.addView(tv); box.addView(sb)
        return box
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = Speaker(this) { runOnUiThread { fillVoices() } }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 48) }
        root.addView(TextView(this).apply { text = "Sara AI 💗"; textSize = 26f })

        root.addView(btn("🟢 Sara Jagao (hamesha sunna)") { startSara() })
        root.addView(btn("🔴 Sara Band Karo") {
            startService(Intent(this, SaraService::class.java).setAction("STOP"))
            append("Sara: Theek hai, ab main nahi sunungi.")
        })
        root.addView(Switch(this).apply {
            text = "App khulte hi Sara apne aap on ho"
            isChecked = Prefs.get(this@MainActivity, "autostart", "1") == "1"
            setOnCheckedChangeListener { _, c -> Prefs.set(this@MainActivity, "autostart", if (c) "1" else "0") }
        })
        root.addView(btn("♿ Accessibility ON karo (khud touch/click)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("👧 Anime dikhao / chhupao") {
            if (!Settings.canDrawOverlays(this)) {
                append("Sara: Pehle 'Display over other apps' ON karo.")
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else if (AvatarOverlay.visible()) AvatarOverlay.hide(this)
            else { AvatarOverlay.show(this); AvatarOverlay.speaking(true) }
        })

        root.addView(head("🧠 Sara ka dimaag (Gemini)"))
        root.addView(TextView(this).apply { text = "Free key: aistudio.google.com/apikey" })
        val key = EditText(this).apply {
            hint = "Gemini API key yahan paste karo"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(Prefs.get(this@MainActivity, "apiKey"))
        }
        val model = EditText(this).apply {
            hint = "Model naam"
            setText(Prefs.get(this@MainActivity, "model", "gemini-3.8-flash"))
        }
        root.addView(key); root.addView(model)
        root.addView(btn("💾 Save") {
            Prefs.set(this, "apiKey", key.text.toString().trim())
            Prefs.set(this, "model", model.text.toString().trim())
            append("Sara: Save ho gaya. ✅")
        })

        root.addView(head("🌐 Bolne ki bhasha"))
        val ls = Spinner(this)
        ls.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langs.map { it.first })
        ls.setSelection(langs.indexOfFirst { it.second == Prefs.get(this, "sttLang", "hi-IN") }.coerceAtLeast(0))
        ls.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.set(this@MainActivity, "sttLang", langs[pos].second)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(ls)

        root.addView(head("🎙️ Sara ki awaaz"))
        voiceSpinner = Spinner(this)
        root.addView(voiceSpinner)
        root.addView(seek("Pitch (patli/moti awaaz)", "pitch", 1.15f))
        root.addView(seek("Speed (bolne ki raftaar)", "rate", 1.0f))
        root.addView(btn("🔊 Awaaz test karo") { speaker.speak("Hii, main Sara hoon. Meri awaaz kaisi lagi?") })

        root.addView(head("📱 App chuno aur Sara se kaam karwao"))
        appView = TextView(this).apply { text = selectedText() }
        root.addView(appView)
        root.addView(btn("📱 App chuno") { pickApp() })
        val goal = EditText(this).apply { hint = "Kya karwana hai? (jaise: reels kholo, X search karo)" }
        root.addView(goal)
        root.addView(btn("▶ Sara khud kare (mujhe touch nahi karna)") {
            val g = goal.text.toString().trim()
            if (g.isNotEmpty()) {
                append("Tum (agent): $g")
                Thread {
                    val r = Agent.run(this, g, true)
                    runOnUiThread { append("Sara: $r"); speaker.speak(r) }
                }.start()
            }
        })
        root.addView(btn("⏹ Stop") { Agent.stop = true; append("Sara: Rok rahi hoon.") })

        root.addView(head("💬 Command"))
        input = EditText(this).apply { hint = "Command likho ya bolo..." }
        root.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val row = LinearLayout(this)
        row.addView(btn("🎤 Bolo") { listen() })
        row.addView(btn("Bhejo") { run(input.text.toString()); input.setText("") })
        root.addView(row)
        log = TextView(this).apply { textSize = 16f }
        root.addView(log)
        setContentView(ScrollView(this).apply { addView(root) })
        append("Sara: Hii! Main Sara hoon. 'Sara Jagao' dabao, phir bas \"Sara\" bolkar mujhe bulao. 😊")
    }

    private fun selectedText(): String {
        val l = Prefs.get(this, "appLabel")
        return if (l.isBlank()) "Chuna hua app: koi nahi" else "Chuna hua app: $l"
    }

    private fun fillVoices() {
        voiceList = speaker.voices()
        val names = listOf("Default (auto)") + voiceList.map { "${it.locale.displayName} · ${it.name}" }
        voiceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val saved = Prefs.get(this, "voice")
        voiceSpinner.setSelection(voiceList.indexOfFirst { it.name == saved } + 1)
        voiceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.set(this@MainActivity, "voice", if (pos == 0) "" else voiceList[pos - 1].name)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun pickApp() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }.sortedBy { it.first.lowercase() }
        AlertDialog.Builder(this).setTitle("App chuno")
            .setItems(apps.map { it.first }.toTypedArray()) { _, i ->
                Prefs.set(this, "appLabel", apps[i].first); Prefs.set(this, "appPkg", apps[i].second)
                appView.text = selectedText()
            }
            .setNegativeButton("Koi nahi") { _, _ ->
                Prefs.set(this, "appLabel", ""); Prefs.set(this, "appPkg", ""); appView.text = selectedText()
            }.show()
    }

    private fun allGranted(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            Settings.canDrawOverlays(this)

    override fun onResume() {
        super.onResume()
        if (Prefs.get(this, "autostart", "1") == "1" && allGranted())
            startForegroundService(Intent(this, SaraService::class.java))
    }

    private fun startSara() {
        val need = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) {
            append("Sara: Sunne ke liye mic aur notification permission chahiye. Allow kar do.")
            requestPermissions(need.toTypedArray(), 2); return
        }
        if (!Settings.canDrawOverlays(this)) {
            append("Sara: 'Display over other apps' ON karo, phir wapas aakar dobara Sara Jagao dabao.")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, SaraService::class.java))
        append("Sara: Main sun rahi hoon. \"Sara, youtube kholo\" bolo! 💗")
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (code == 2) startSara()
    }

    private fun listen() {
        val lang = Prefs.get(this, "sttLang", "hi-IN")
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == "auto") "hi-IN" else lang)
        }
        try { startActivityForResult(i, 1) }
        catch (e: ActivityNotFoundException) { append("Sara: Is phone mein voice recognition nahi mila.") }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK)
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { run(it) }
    }

    private fun run(text: String) {
        if (text.isBlank()) return
        append("Tum: $text")
        Brain.process(this, text) { reply, lang, add ->
            append("Sara: $reply")
            speaker.speak(reply, lang, add)
        }
    }

    private fun append(s: String) { log.append(s + "\n\n") }
    override fun onDestroy() { speaker.shutdown(); super.onDestroy() }
}
