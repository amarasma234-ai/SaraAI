package com.sara.ai

import android.content.Context
import android.graphics.*
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import java.io.File

fun shadeColor(c: Int, f: Float): Int = Color.rgb(
    (Color.red(c) * f).toInt().coerceIn(0, 255),
    (Color.green(c) * f).toInt().coerceIn(0, 255),
    (Color.blue(c) * f).toInt().coerceIn(0, 255))

class AvatarView(ctx: Context) : View(ctx) {
    @Volatile var speaking = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var hair = 0; private var eye = 0; private var outfit = 0; private var skin = 0
    private var bmp: Bitmap? = null

    init { reload() }

    fun reload() {
        val c = context
        hair = Prefs.color(c, "hairColor", 0xFF5B3FA0.toInt())
        eye = Prefs.color(c, "eyeColor", 0xFF2E86DE.toInt())
        outfit = Prefs.color(c, "outfitColor", 0xFFFF7BAC.toInt())
        skin = Prefs.color(c, "skinColor", 0xFFFFE3D1.toInt())
        val path = Prefs.get(c, "avatarImage")
        bmp = try { if (path.isNotBlank() && File(path).exists()) BitmapFactory.decodeFile(path) else null }
        catch (e: Exception) { null }
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val t = System.currentTimeMillis()
        val bob = (Math.sin(t / (if (speaking) 160.0 else 700.0)) * h * (if (speaking) 0.008 else 0.006)).toFloat()
        c.save()
        c.translate(0f, bob)
        val b = bmp
        if (b != null) {
            val s = minOf(w / b.width, h / b.height) * (if (speaking) 1f + 0.012f * Math.sin(t / 120.0).toFloat() else 1f)
            val dw = b.width * s; val dh = b.height * s
            c.drawBitmap(b, null, RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), p)
        } else girl(c, w, h, t)
        c.restore()
        postInvalidateOnAnimation()
    }

    private fun girl(c: Canvas, w: Float, h: Float, t: Long) {
        val hairD = shadeColor(hair, 0.72f)
        val hairL = shadeColor(hair, 1.3f)
        val dark = 0xFF2B1B2B.toInt()
        p.style = Paint.Style.FILL
        // twin tails and back hair
        p.color = hairD
        c.drawOval(RectF(w * 0.01f, h * 0.40f, w * 0.27f, h * 0.99f), p)
        c.drawOval(RectF(w * 0.73f, h * 0.40f, w * 0.99f, h * 0.99f), p)
        p.color = hair
        c.drawRoundRect(RectF(w * 0.12f, h * 0.28f, w * 0.88f, h * 0.97f), w * 0.28f, w * 0.28f, p)
        c.drawOval(RectF(w * 0.1f, h * 0.03f, w * 0.9f, h * 0.68f), p)
        // body
        p.color = outfit
        c.drawRoundRect(RectF(w * 0.18f, h * 0.84f, w * 0.82f, h * 1.1f), 60f, 60f, p)
        // neck
        p.color = shadeColor(skin, 0.93f)
        c.drawRect(RectF(w * 0.43f, h * 0.74f, w * 0.57f, h * 0.88f), p)
        // collar and bow
        p.color = Color.WHITE
        c.drawPath(Path().apply {
            moveTo(w * 0.28f, h * 0.855f); lineTo(w * 0.5f, h * 0.96f); lineTo(w * 0.72f, h * 0.855f)
            lineTo(w * 0.62f, h * 0.835f); lineTo(w * 0.5f, h * 0.89f); lineTo(w * 0.38f, h * 0.835f); close()
        }, p)
        p.color = shadeColor(outfit, 0.7f)
        c.drawPath(Path().apply {
            moveTo(w * 0.5f, h * 0.9f); lineTo(w * 0.4f, h * 0.87f); lineTo(w * 0.4f, h * 0.94f); close()
            moveTo(w * 0.5f, h * 0.9f); lineTo(w * 0.6f, h * 0.87f); lineTo(w * 0.6f, h * 0.94f); close()
        }, p)
        c.drawCircle(w * 0.5f, h * 0.9f, w * 0.018f, p)
        // face
        p.color = skin
        c.drawOval(RectF(w * 0.2f, h * 0.2f, w * 0.8f, h * 0.8f), p)
        // bangs
        p.color = hair
        c.drawPath(Path().apply {
            moveTo(w * 0.16f, h * 0.44f)
            quadTo(w * 0.5f, -h * 0.07f, w * 0.84f, h * 0.44f)
            lineTo(w * 0.74f, h * 0.31f); lineTo(w * 0.62f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.29f); lineTo(w * 0.38f, h * 0.44f)
            lineTo(w * 0.26f, h * 0.31f); close()
        }, p)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = w * 0.008f; p.color = hairL
        c.drawPath(Path().apply { moveTo(w * 0.34f, h * 0.2f); quadTo(w * 0.4f, h * 0.12f, w * 0.5f, h * 0.1f) }, p)
        c.drawPath(Path().apply { moveTo(w * 0.62f, h * 0.14f); quadTo(w * 0.7f, h * 0.18f, w * 0.74f, h * 0.26f) }, p)
        // ahoge
        p.strokeWidth = w * 0.014f; p.color = hair
        c.drawPath(Path().apply { moveTo(w * 0.5f, h * 0.06f); quadTo(w * 0.46f, -h * 0.02f, w * 0.55f, -h * 0.01f) }, p)
        // side locks
        p.style = Paint.Style.FILL; p.color = hair
        c.drawOval(RectF(w * 0.16f, h * 0.36f, w * 0.26f, h * 0.7f), p)
        c.drawOval(RectF(w * 0.74f, h * 0.36f, w * 0.84f, h * 0.7f), p)
        // eyes
        val blink = t % 3800 < 150
        val ey = h * 0.53f
        for (ex in floatArrayOf(w * 0.37f, w * 0.63f)) {
            if (blink) {
                p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = dark
                c.drawLine(ex - w * 0.08f, ey, ex + w * 0.08f, ey, p)
                p.style = Paint.Style.FILL
            } else {
                p.style = Paint.Style.FILL
                p.color = Color.WHITE
                c.drawOval(RectF(ex - w * 0.1f, ey - h * 0.09f, ex + w * 0.1f, ey + h * 0.09f), p)
                p.color = eye
                c.drawOval(RectF(ex - w * 0.075f, ey - h * 0.085f, ex + w * 0.075f, ey + h * 0.085f), p)
                p.color = shadeColor(eye, 0.55f)
                c.drawOval(RectF(ex - w * 0.075f, ey - h * 0.085f, ex + w * 0.075f, ey - h * 0.02f), p)
                p.color = dark
                c.drawOval(RectF(ex - w * 0.034f, ey - h * 0.04f, ex + w * 0.034f, ey + h * 0.04f), p)
                p.color = Color.WHITE
                c.drawCircle(ex - w * 0.03f, ey - h * 0.04f, w * 0.022f, p)
                c.drawCircle(ex + w * 0.025f, ey + h * 0.035f, w * 0.01f, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 7f; p.color = dark
                c.drawArc(RectF(ex - w * 0.1f, ey - h * 0.09f, ex + w * 0.1f, ey + h * 0.09f), 190f, 160f, false, p)
                p.style = Paint.Style.FILL
            }
            // brow
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = hairD
            c.drawArc(RectF(ex - w * 0.08f, ey - h * 0.17f, ex + w * 0.08f, ey - h * 0.1f), 200f, 140f, false, p)
            p.style = Paint.Style.FILL
        }
        // blush, nose, mouth
        p.color = 0x66FF6B81
        c.drawOval(RectF(w * 0.22f, h * 0.62f, w * 0.34f, h * 0.68f), p)
        c.drawOval(RectF(w * 0.66f, h * 0.62f, w * 0.78f, h * 0.68f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = shadeColor(skin, 0.7f)
        c.drawLine(w * 0.5f, h * 0.62f, w * 0.495f, h * 0.645f, p)
        val mx = w * 0.5f; val my = h * 0.7f
        if (speaking) {
            val open = ((Math.sin(t / 80.0) + 1) / 2).toFloat()
            p.style = Paint.Style.FILL; p.color = 0xFFC2185B.toInt()
            c.drawOval(RectF(mx - w * 0.05f, my - h * 0.005f, mx + w * 0.05f, my + h * (0.012f + 0.04f * open)), p)
        } else {
            p.strokeWidth = 5f; p.color = 0xFFC2185B.toInt()
            c.drawArc(RectF(mx - w * 0.05f, my - h * 0.02f, mx + w * 0.05f, my + h * 0.03f), 20f, 140f, false, p)
        }
        p.style = Paint.Style.FILL
    }
}

object AvatarOverlay {
    private var view: AvatarView? = null

    fun show(ctx: Context) {
        val app = ctx.applicationContext
        if (view != null || !Settings.canDrawOverlays(app)) return
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d = app.resources.displayMetrics.density
        val lp = WindowManager.LayoutParams(
            (220 * d).toInt(), (260 * d).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.END; x = 16; y = 120 }
        view = AvatarView(app)
        try { wm.addView(view, lp) } catch (e: Exception) { view = null }
    }

    fun hide(ctx: Context) {
        val v = view ?: return
        val wm = ctx.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try { wm.removeView(v) } catch (e: Exception) {}
        view = null
    }

    fun speaking(b: Boolean) { view?.speaking = b }
    fun visible() = view != null
}
