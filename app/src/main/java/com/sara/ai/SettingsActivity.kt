package com.sara.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.Voice
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : Activity() {
    private lateinit var speaker: Speaker
    private lateinit var preview: AvatarView
    private lateinit var voiceSpinner: Spinner
    private lateinit var appView: TextView
    private lateinit var out: TextView
    private var voiceList: List<Voice> = emptyList()
    private val langs = listOf("Auto (sab bhasha)" to "auto", "हिन्दी" to "hi-IN", "English (India)" to "en-IN",
        "English (US)" to "en-US", "বাংলা" to "bn-IN", "தமிழ்" to "ta-IN", "తెలుగు" to "te-IN", "मराठी" to "mr-IN",
        "ગુજરાતી" to "gu-IN", "ਪੰਜਾਬੀ" to "pa-IN", "ಕನ್ನಡ" to "kn-IN", "മലയാളം" to "ml-IN", "اردو" to "ur-IN")

    private fun c(v: Long) = v.toInt()
    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }
    private fun head(t: String) = TextView(this).apply { text = t; textSize = 18f; setPadding(0, 36, 0, 8) }

    private fun swatches(title: String, key: String, colors: List<Int>): View {
        val d = resources.displayMetrics.density
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(this).apply { text = title; setPadding(0, 12, 0, 0) })
        val row = LinearLayout(this)
        for (col in colors) row.addView(View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col); setStroke(3, Color.GRAY) }
            layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), (42 * d).toInt()).apply { setMargins(8, 8, 8, 8) }
            setOnClickListener { Prefs.set(this@SettingsActivity, key, col.toString()); preview.reload() }
        })
        box.addView(HorizontalScrollView(this).apply { addView(row) })
        return box
    }

    private fun seek(label: String, key: String, def: Float): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val tv = TextView(this)
        val cur = Prefs.getF(this, key, def)
        tv.text = "$label: ${"%.2f".format(cur)}"
        val sb = SeekBar(this).apply { max = 150; progress = ((cur * 100) - 50).toInt().coerceIn(0, 150) }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                val v = (p + 50) / 100f
                Prefs.setF(this@SettingsActivity, key, v)
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
        title = "Sara - Customize & Settings"
        speaker = Speaker(this) { runOnUiThread { fillVoices() } }
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 24, 32, 48) }

        root.addView(head("🎨 Anime girl customize"))
        preview = AvatarView(this)
        root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (280 * d).toInt()))
        root.addView(swatches("Baal (hair)", "hairColor", listOf(c(0xFF5B3FA0), c(0xFFFF7BAC), c(0xFF1B1B1B),
            c(0xFF6B3E26), c(0xFFF2C94C), c(0xFF2D6CDF), c(0xFFC9CED6), c(0xFFD62839))))
        root.addView(swatches("Aankhen (eyes)", "eyeColor", listOf(c(0xFF2E86DE), c(0xFF2EB872), c(0xFF9B51E0),
            c(0xFFE5484D), c(0xFFF2A900), c(0xFFFF5C8A), c(0xFF5A3A22))))
        root.addView(swatches("Kapde (outfit)", "outfitColor", listOf(c(0xFFFF7BAC), c(0xFFF5F5F5), c(0xFF222222),
            c(0xFF3B82F6), c(0xFFE11D48), c(0xFF10B981), c(0xFF8B5CF6))))
        root.addView(swatches("Skin", "skinColor", listOf(c(0xFFFFE3D1), c(0xFFF7D0B5), c(0xFFE8B48F), c(0xFFC68642), c(0xFF8D5524))))
        root.addView(swatches("Background", "bgColor", listOf(c(0xFF2B1B4E), c(0xFF0F2A43), c(0xFF3B0F2E),
            c(0xFF102A1E), c(0xFF1A1A1A), c(0xFF4A2C0F))))
        root.addView(btn("🖼️ Gallery se apni anime image lo") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), 5)
        })
        root.addView(btn("↩ Default anime girl wapas") {
            listOf("avatarImage", "hairColor", "eyeColor", "outfitColor", "skinColor", "bgColor").forEach { Prefs.set(this, it, "") }
            preview.reload()
        })

        root.addView(head("🧠 Sara ka dimaag (Gemini)"))
        root.addView(TextView(this).apply { text = "Free key: aistudio.google.com/apikey" })
        val key = EditText(this).apply {
            hint = "Gemini API key yahan paste karo"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(Prefs.get(this@SettingsActivity, "apiKey"))
        }
        val model = EditText(this).apply { hint = "Model naam"; setText(Prefs.get(this@SettingsActivity, "model", "gemini-3.8-flash")) }
        root.addView(key); root.addView(model)
        root.addView(btn("💾 Save") {
            Prefs.set(this, "apiKey", key.text.toString().trim())
            Prefs.set(this, "model", model.text.toString().trim())
            Toast.makeText(this, "Save ho gaya ✅", Toast.LENGTH_SHORT).show()
        })

        root.addView(head("🌐 Bolne ki bhasha"))
        val ls = Spinner(this)
        ls.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langs.map { it.first })
        ls.setSelection(langs.indexOfFirst { it.second == Prefs.get(this, "sttLang", "hi-IN") }.coerceAtLeast(0))
        ls.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.set(this@SettingsActivity, "sttLang", langs[pos].second)
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
        root.addView(btn("📱 App chuno") { AppPicker.show(this) { appView.text = selectedText() } })
        val goal = EditText(this).apply { hint = "Kya karwana hai? (jaise: reels kholo, X search karo)" }
        root.addView(goal)
        root.addView(btn("▶ Sara khud kare (mujhe touch nahi karna)") {
            val g = goal.text.toString().trim()
            if (g.isNotEmpty()) {
                out.text = "Kaam shuru: $g"
                Thread {
                    val r = Agent.run(this, g, true)
                    runOnUiThread { out.text = r; speaker.speak(r) }
                }.start()
            }
        })
        root.addView(btn("⏹ Stop") { Agent.stop = true; out.text = "Rok rahi hoon." })
        out = TextView(this)
        root.addView(out)

        root.addView(head("🔐 Permissions"))
        root.addView(Switch(this).apply {
            text = "App khulte hi Sara apne aap on ho"
            isChecked = Prefs.get(this@SettingsActivity, "autostart", "1") == "1"
            setOnCheckedChangeListener { _, ch -> Prefs.set(this@SettingsActivity, "autostart", if (ch) "1" else "0") }
        })
        root.addView(btn("♿ Accessibility ON karo (khud touch/click)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("🪟 Display over other apps ON karo") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("👧 Anime overlay test (dikhao/chhupao)") {
            if (!Settings.canDrawOverlays(this)) Toast.makeText(this, "Pehle overlay permission ON karo", Toast.LENGTH_SHORT).show()
            else if (AvatarOverlay.visible()) AvatarOverlay.hide(this)
            else { AvatarOverlay.show(this); AvatarOverlay.speaking(true) }
        })
        setContentView(ScrollView(this).apply { addView(root) })
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
                Prefs.set(this@SettingsActivity, "voice", if (pos == 0) "" else voiceList[pos - 1].name)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                var s = 1
                while (o.outWidth / s > 1200 || o.outHeight / s > 1200) s *= 2
                val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })
                val f = File(filesDir, "avatar.png")
                FileOutputStream(f).use { bm.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Prefs.set(this, "avatarImage", f.absolutePath)
                preview.reload()
            } catch (e: Exception) {
                Toast.makeText(this, "Image load nahi hui: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() { speaker.shutdown(); super.onDestroy() }
}
