package com.sara.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

class Speaker(private val ctx: Context, private val onReady: (() -> Unit)? = null) {
    private var tts: TextToSpeech? = null
    @Volatile var ready = false
    @Volatile private var lastId = ""
    var finished: (() -> Unit)? = null

    init {
        tts = TextToSpeech(ctx.applicationContext) { st ->
            if (st == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { if (id == lastId) finished?.invoke() }
                    override fun onError(id: String?) { if (id == lastId) finished?.invoke() }
                })
                ready = true
                onReady?.invoke()
            }
        }
    }

    fun voices(): List<Voice> = try {
        tts?.voices?.filter { !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            ?.sortedBy { it.locale.toString() } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    fun speak(text: String, langTag: String? = null, add: Boolean = false) {
        val t = tts ?: return
        t.setPitch(Prefs.getF(ctx, "pitch", 1.15f))
        t.setSpeechRate(Prefs.getF(ctx, "rate", 1.0f))
        val vname = Prefs.get(ctx, "voice")
        val voice = if (vname.isNotBlank()) t.voices?.firstOrNull { it.name == vname } else null
        val want = langTag?.substringBefore('-')?.lowercase()
        if (voice != null && (want == null || voice.locale.language == want)) {
            t.voice = voice
        } else {
            val loc = if (langTag != null) Locale.forLanguageTag(langTag) else Locale("hi", "IN")
            if (t.setLanguage(loc) < TextToSpeech.LANG_AVAILABLE) t.setLanguage(Locale("en", "IN"))
        }
        val id = "u" + System.nanoTime()
        lastId = id
        t.speak(text, if (add) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun shutdown() { tts?.shutdown() }
}
