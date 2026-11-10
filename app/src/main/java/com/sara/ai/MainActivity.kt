package com.sara.ai

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var speaker: Speaker
    private lateinit var avatar: AvatarView
    private lateinit var bubble: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var root: LinearLayout
    private val ui = Handler(Looper.getMainLooper())

    private fun bgDrawable(): GradientDrawable {
        val bg = Prefs.color(this, "bgColor", 0xFF2B1B4E.toInt())
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(bg, shadeColor(bg, 0.4f)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = Speaker(this)
        speaker.finished = { runOnUiThread { avatar.speaking = false } }
        val d = resources.displayMetrics.density

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bgDrawable()
            setPadding((16 * d).toInt(), (40 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        status = TextView(this).apply {
            textSize = 15f; setTextColor(Color.WHITE); setPadding(8, 8, 8, 8)
            setOnClickListener { toggleSara() }
        }
        top.addView(status, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(Button(this).apply {
            text = "📱"
            setOnClickListener { AppPicker.show(this@MainActivity) {
                val l = Prefs.get(this@MainActivity, "appLabel")
                showBubble(if (l.isBlank()) "Koi app nahi chuna." else "Chuna hua app: $l. Ab bolo kya karna hai.")
            } }
        })
        top.addView(Button(this).apply {
            text = "⚙ Customize"
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        })
        root.addView(top)

        avatar = AvatarView(this)
        root.addView(avatar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        bubble = TextView(this).apply {
            textSize = 16f; setTextColor(Color.WHITE); maxLines = 5
            movementMethod = ScrollingMovementMethod()
            setPadding((16 * d).toInt(), (12 * d).toInt(), (16 * d).toInt(), (12 * d).toInt())
            background = GradientDrawable().apply { cornerRadius = 28 * d; setColor(0x33FFFFFF) }
        }
        root.addView(bubble, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = (8 * d).toInt() })

        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        input = EditText(this).apply {
            hint = "Likho ya mic dabao..."; setHintTextColor(0x88FFFFFF.toInt()); setTextColor(Color.WHITE)
        }
        row.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Button(this).apply {
            text = "➤"
            setOnClickListener { run(input.text.toString()); input.setText("") }
        })
        row.addView(Button(this).apply { text = "🎤"; textSize = 22f; setOnClickListener { listen() } })
        root.addView(row)

        setContentView(root)
        showBubble("Hii! Main Sara hoon 💗 Mic dabao ya bas \"Sara\" bolo.")
    }

    private fun showBubble(s: String) { bubble.text = s; bubble.scrollTo(0, 0) }

    private fun refreshStatus() {
        status.text = if (SaraService.active) "🟢 Sara sun rahi hai (tap = band)" else "⚪ Sara band hai (tap = chalu)"
    }

    private fun toggleSara() {
        if (SaraService.active) {
            startService(Intent(this, SaraService::class.java).setAction("STOP"))
            ui.postDelayed({ refreshStatus() }, 600)
        } else startSara()
    }

    private fun allGranted(): Boolean =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            Settings.canDrawOverlays(this)

    override fun onResume() {
        super.onResume()
        avatar.reload()
        root.background = bgDrawable()
        if (Prefs.get(this, "autostart", "1") == "1" && allGranted())
            startForegroundService(Intent(this, SaraService::class.java))
        refreshStatus()
        ui.postDelayed({ refreshStatus() }, 800)
    }

    private fun startSara() {
        val need = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) {
            showBubble("Sunne ke liye mic aur notification permission chahiye. Allow kar do.")
            requestPermissions(need.toTypedArray(), 2); return
        }
        if (!Settings.canDrawOverlays(this)) {
            showBubble("'Display over other apps' ON karo, phir wapas aakar status par tap karo.")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, SaraService::class.java))
        showBubble("Main sun rahi hoon. \"Sara, youtube kholo\" bolo! 💗")
        ui.postDelayed({ refreshStatus() }, 800)
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
        catch (e: ActivityNotFoundException) { showBubble("Is phone mein voice recognition nahi mila.") }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK)
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { run(it) }
    }

    private fun run(text: String) {
        if (text.isBlank()) return
        showBubble("Tum: $text")
        Brain.process(this, text) { reply, lang, add ->
            showBubble(reply)
            avatar.speaking = true
            speaker.speak(reply, lang, add)
        }
    }

    override fun onDestroy() { speaker.shutdown(); super.onDestroy() }
}
