package com.sara.ai

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SaraService : Service(), RecognitionListener {
    companion object { @Volatile var active = false }
    private val h = Handler(Looper.getMainLooper())
    private var sr: SpeechRecognizer? = null
    private lateinit var speaker: Speaker
    private var speaking = false
    private var running = false
    private var awaitUntil = 0L
    private val wake = listOf("sara", "sarah", "saara", "sera", "सारा", "सरा", "সারা", "சாரா", "సారా",
        "સારા", "ਸਾਰਾ", "ಸಾರಾ", "സാറ", "سارا", "ساره")

    private val hideTask = object : Runnable {
        override fun run() {
            if (!speaking && !Agent.running && System.currentTimeMillis() >= awaitUntil)
                AvatarOverlay.hide(this@SaraService)
            else if (running) h.postDelayed(this, 3000)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        speaker = Speaker(this)
        speaker.finished = { afterSpeak() }
    }

    private fun afterSpeak() {
        h.post {
            speaking = false
            AvatarOverlay.speaking(false)
            if (Pending.has()) awaitUntil = System.currentTimeMillis() + 10000
            h.removeCallbacks(hideTask)
            h.postDelayed(hideTask, 4000)
            listen()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            running = false
            active = false
            AvatarOverlay.hide(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("sara", "Sara", NotificationManager.IMPORTANCE_LOW))
        val openPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getService(this, 1,
            Intent(this, SaraService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "sara")
            .setContentTitle("Sara sun rahi hai")
            .setContentText("\"Sara\" bolo, main active ho jaungi")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Band karo", stopPi).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, n)
        active = true
        if (!running) { running = true; listen() }
        return START_STICKY
    }

    private fun recognizerIntent(): Intent {
        val lang = Prefs.get(this, "sttLang", "hi-IN")
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (lang == "auto") {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                if (Build.VERSION.SDK_INT >= 33) {
                    putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES,
                        arrayListOf("hi-IN", "en-IN", "bn-IN", "ta-IN", "te-IN", "mr-IN", "gu-IN", "pa-IN", "kn-IN", "ml-IN", "ur-IN"))
                }
            } else putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
        }
    }

    private fun listen() {
        if (!running || speaking) return
        h.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    say("Is phone mein voice recognition available nahi hai."); running = false; stopSelf(); return@post
                }
                sr?.destroy()
                sr = SpeechRecognizer.createSpeechRecognizer(this).also {
                    it.setRecognitionListener(this)
                    it.startListening(recognizerIntent())
                }
            } catch (e: Exception) { h.postDelayed({ listen() }, 1500) }
        }
    }

    override fun onResults(results: Bundle?) {
        val all = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
        val withWake = all.firstOrNull { a -> wake.any { a.lowercase().contains(it) } }
        val awaiting = System.currentTimeMillis() < awaitUntil
        if (withWake == null && !awaiting && !Agent.running) { listen(); return }
        AvatarOverlay.show(this)
        var cmd = (withWake ?: all.firstOrNull() ?: "")
        wake.forEach { cmd = cmd.replace(it, " ", ignoreCase = true) }
        listOf("hey ", "hi ", "hello", "हे ", "हेलो").forEach { cmd = cmd.replace(it, " ", ignoreCase = true) }
        cmd = cmd.trim()
        awaitUntil = 0
        if (cmd.isBlank()) {
            awaitUntil = System.currentTimeMillis() + 8000
            say("Ji, boliye.")
        } else {
            Brain.process(this, cmd) { t, l, a -> say(t, l, a) }
        }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            say("Mic permission nahi hai."); running = false; stopSelf(); return
        }
        val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
            error == SpeechRecognizer.ERROR_TOO_MANY_REQUESTS) 2000L else 300L
        h.postDelayed({ listen() }, delay)
    }

    private fun say(text: String, lang: String? = null, add: Boolean = false) {
        if (!speaker.ready) { listen(); return }
        speaking = true
        AvatarOverlay.show(this)
        AvatarOverlay.speaking(true)
        sr?.cancel()
        speaker.speak(text, lang, add)
    }

    override fun onReadyForSpeech(p: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(v: Float) {}
    override fun onBufferReceived(b: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(p: Bundle?) {}
    override fun onEvent(t: Int, p: Bundle?) {}

    override fun onDestroy() {
        running = false
        active = false
        h.removeCallbacks(hideTask)
        AvatarOverlay.hide(this)
        sr?.destroy()
        speaker.shutdown()
        super.onDestroy()
    }
}
