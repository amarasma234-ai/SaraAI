package com.sara.ai

import android.content.Context
import android.media.AudioManager

object VolumeManager {
    private fun am(ctx: Context) = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun change(ctx: Context, up: Boolean): String {
        am(ctx).adjustStreamVolume(AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return (if (up) "Volume badha diya. " else "Volume kam kar diya. ") + info(ctx)
    }

    fun setPercent(ctx: Context, p: Int): String {
        val a = am(ctx)
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        a.setStreamVolume(AudioManager.STREAM_MUSIC, (p.coerceIn(0, 100) * max / 100), AudioManager.FLAG_SHOW_UI)
        return "Volume ${p.coerceIn(0, 100)} percent kar diya."
    }

    fun mute(ctx: Context, m: Boolean): String {
        am(ctx).adjustStreamVolume(AudioManager.STREAM_MUSIC,
            if (m) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
        return if (m) "Mute kar diya." else "Unmute kar diya."
    }

    fun info(ctx: Context): String {
        val a = am(ctx)
        val p = a.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return "Media volume abhi $p percent hai."
    }
}
