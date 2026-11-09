package com.sara.ai

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var tts: TextToSpeech
    private lateinit var log: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts.language = Locale("hi", "IN") }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(TextView(this).apply { text = "Sara AI 💗"; textSize = 26f })
        root.addView(Button(this).apply {
            text = "🟢 Sara Jagao (hamesha sunna)"
            setOnClickListener { startSara() }
        })
        root.addView(Button(this).apply {
            text = "🔴 Sara Band Karo"
            setOnClickListener {
                startService(Intent(this@MainActivity, SaraService::class.java).setAction("STOP"))
                append("Sara: Theek hai, ab main nahi sunungi.")
            }
        })
        root.addView(Button(this).apply {
            text = "♿ Accessibility ON karo (touch/click/scroll)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "👧 Anime dikhao / chhupao"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    append("Sara: Pehle 'Display over other apps' ON karo.")
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else if (AvatarOverlay.visible()) AvatarOverlay.hide(this@MainActivity)
                else { AvatarOverlay.show(this@MainActivity); AvatarOverlay.speaking(true) }
            }
        })
        input = EditText(this).apply { hint = "Ya command likho: youtube kholo, torch on..." }
        root.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val row = LinearLayout(this)
        row.addView(Button(this).apply { text = "🎤 Bolo"; setOnClickListener { listen() } })
        row.addView(Button(this).apply {
            text = "Bhejo"
            setOnClickListener { run(input.text.toString()); input.setText("") }
        })
        root.addView(row)
        log = TextView(this).apply { textSize = 16f }
        root.addView(ScrollView(this).apply { addView(log) })
        setContentView(root)
        append("Sara: Hii! Main Sara hoon. 'Sara Jagao' dabao, phir bas \"Sara\" bolkar mujhe bulao. 😊")
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
            append("Sara: Background mein apps kholne ke liye 'Display over other apps' ON karo, phir wapas aakar dobara Sara Jagao dabao.")
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
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
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
        CommandRouter.handleAsync(this, text) { reply ->
            append("Sara: $reply")
            tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "sara")
        }
    }

    private fun append(s: String) { log.append(s + "\n\n") }
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
