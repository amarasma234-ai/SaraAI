package com.sara.ai

import android.content.Context
import android.graphics.*
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class AvatarView(ctx: Context) : View(ctx) {
    @Volatile var speaking = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hair = 0xFF5B3FA0.toInt()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val t = System.currentTimeMillis()
        p.style = Paint.Style.FILL
        p.color = hair
        c.drawRoundRect(RectF(w * 0.06f, h * 0.30f, w * 0.94f, h * 0.99f), w * 0.3f, w * 0.3f, p)
        c.drawOval(RectF(w * 0.1f, h * 0.04f, w * 0.9f, h * 0.7f), p)
        p.color = 0xFFFF7BAC.toInt()
        c.drawRoundRect(RectF(w * 0.22f, h * 0.82f, w * 0.78f, h * 1.05f), 40f, 40f, p)
        p.color = 0xFFFFE3D1.toInt()
        c.drawOval(RectF(w * 0.2f, h * 0.2f, w * 0.8f, h * 0.8f), p)
        p.color = hair
        val b = Path().apply {
            moveTo(w * 0.16f, h * 0.42f)
            quadTo(w * 0.5f, -h * 0.06f, w * 0.84f, h * 0.42f)
            lineTo(w * 0.72f, h * 0.30f); lineTo(w * 0.6f, h * 0.42f)
            lineTo(w * 0.5f, h * 0.28f); lineTo(w * 0.38f, h * 0.42f)
            lineTo(w * 0.28f, h * 0.30f); close()
        }
        c.drawPath(b, p)
        val blink = t % 3600 < 140
        val ey = h * 0.52f
        for (ex in floatArrayOf(w * 0.37f, w * 0.63f)) {
            if (blink) {
                p.color = 0xFF3B2A2A.toInt(); p.style = Paint.Style.STROKE; p.strokeWidth = 5f
                c.drawLine(ex - w * 0.08f, ey, ex + w * 0.08f, ey, p)
                p.style = Paint.Style.FILL
            } else {
                p.color = Color.WHITE
                c.drawOval(RectF(ex - w * 0.09f, ey - h * 0.075f, ex + w * 0.09f, ey + h * 0.075f), p)
                p.color = 0xFF2E86DE.toInt()
                c.drawOval(RectF(ex - w * 0.065f, ey - h * 0.07f, ex + w * 0.065f, ey + h * 0.07f), p)
                p.color = 0xFF14213D.toInt()
                c.drawOval(RectF(ex - w * 0.03f, ey - h * 0.035f, ex + w * 0.03f, ey + h * 0.035f), p)
                p.color = Color.WHITE
                c.drawCircle(ex - w * 0.025f, ey - h * 0.03f, w * 0.02f, p)
            }
        }
        p.color = 0x55FF6B81
        c.drawCircle(w * 0.27f, h * 0.64f, w * 0.06f, p)
        c.drawCircle(w * 0.73f, h * 0.64f, w * 0.06f, p)
        val mx = w * 0.5f; val my = h * 0.69f
        if (speaking) {
            val open = ((Math.sin(t / 80.0) + 1) / 2).toFloat()
            p.color = 0xFFC2185B.toInt()
            c.drawOval(RectF(mx - w * 0.05f, my - h * 0.005f, mx + w * 0.05f, my + h * (0.01f + 0.035f * open)), p)
        } else {
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = 0xFFC2185B.toInt()
            c.drawArc(RectF(mx - w * 0.05f, my - h * 0.02f, mx + w * 0.05f, my + h * 0.03f), 20f, 140f, false, p)
            p.style = Paint.Style.FILL
        }
        postInvalidateOnAnimation()
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
