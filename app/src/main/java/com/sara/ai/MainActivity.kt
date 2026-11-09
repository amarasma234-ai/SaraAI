package com.sara.ai

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
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
        input = EditText(this).apply { hint = "Command likho: youtube kholo, torch on, battery..." }
        root.addView(input, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val row = LinearLayout(this)
        row.addView(Button(this).apply {
            text = "🎤 Bolo"
            setOnClickListener { listen() }
        })
        row.addView(Button(this).apply {
            text = "Bhejo"
            setOnClickListener { run(input.text.toString()); input.setText("") }
        })
        root.addView(row)
        log = TextView(this).apply { textSize = 16f }
        root.addView(ScrollView(this).apply { addView(log) })
        setContentView(root)
        append("Sara: Hii! Main Sara hoon. Bolo kya karna hai? 😊")
    }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sara sun rahi hai...")
        }
        try { startActivityForResult(i, 1) }
        catch (e: ActivityNotFoundException) { append("Sara: Is phone mein voice recognition nahi mila.") }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { run(it) }
        }
    }

    private fun run(text: String) {
        if (text.isBlank()) return
        append("Tum: $text")
        val reply = try { CommandRouter.handle(this, text) } catch (e: Exception) { "Error: ${e.message}" }
        append("Sara: $reply")
        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "sara")
    }

    private fun append(s: String) { log.append(s + "\n\n") }

    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
