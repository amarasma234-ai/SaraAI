package com.sara.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SaraAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: SaraAccessibilityService? = null }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }

    fun collect(pred: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            if (pred(n)) out.add(n)
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(rootInActiveWindow)
        return out
    }

    fun matches(n: AccessibilityNodeInfo, q: String): Boolean {
        val t = n.text?.toString() ?: ""
        val d = n.contentDescription?.toString() ?: ""
        val id = (n.viewIdResourceName ?: "").substringAfterLast('/')
        return t.contains(q, true) || d.contains(q, true) || (id.isNotEmpty() && id.contains(q, true))
    }

    fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var x: AccessibilityNodeInfo? = n
        while (x != null && !x.isClickable) x = x.parent
        if (x != null && x.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val r = Rect(); n.getBoundsInScreen(r)
        return tap(r.centerX().toFloat(), r.centerY().toFloat())
    }

    fun clickText(q: String): Boolean {
        val nodes = collect { matches(it, q) }
        val best = nodes.firstOrNull {
            it.text?.toString().equals(q, true) || it.contentDescription?.toString().equals(q, true)
        } ?: nodes.firstOrNull() ?: return false
        return clickNode(best)
    }

    fun tap(x: Float, y: Float): Boolean {
        val p = Path().apply { moveTo(x, y) }
        return dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, dur: Long = 350): Boolean {
        val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        return dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, dur)).build(), null, null)
    }

    fun scroll(forward: Boolean): Boolean {
        val n = collect { it.isScrollable }.firstOrNull()
        val act = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        if (n != null && n.performAction(act)) return true
        val w = resources.displayMetrics.widthPixels.toFloat()
        val h = resources.displayMetrics.heightPixels.toFloat()
        return if (forward) swipe(w / 2, h * 0.7f, w / 2, h * 0.3f) else swipe(w / 2, h * 0.3f, w / 2, h * 0.7f)
    }

    fun scrollEdge(forward: Boolean): Int {
        val act = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        var c = 0
        while (c < 30) {
            val n = collect { it.isScrollable }.firstOrNull() ?: break
            if (!n.performAction(act)) break
            c++
            Thread.sleep(350)
        }
        return c
    }

    fun swipeSide(left: Boolean): Boolean {
        val w = resources.displayMetrics.widthPixels.toFloat()
        val h = resources.displayMetrics.heightPixels.toFloat()
        return if (left) swipe(w * 0.8f, h / 2, w * 0.2f, h / 2) else swipe(w * 0.2f, h / 2, w * 0.8f, h / 2)
    }

    fun typeText(s: String): Boolean {
        val n = collect { it.isEditable && !it.isPassword && it.isFocused }.firstOrNull()
            ?: collect { it.isEditable && !it.isPassword }.firstOrNull() ?: return false
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val b = Bundle()
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, s)
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
    }

    fun clickFirstResult(): Boolean {
        val h = resources.displayMetrics.heightPixels
        val best = collect { n ->
            if (!n.isClickable || !n.isVisibleToUser) false else {
                val r = Rect(); n.getBoundsInScreen(r)
                r.top > h * 0.18 && r.height() > h * 0.08 &&
                    ((n.contentDescription?.length ?: 0) > 15 || (n.text?.length ?: 0) > 15)
            }
        }.minByOrNull { val r = Rect(); it.getBoundsInScreen(r); r.top }
        return best?.let { clickNode(it) } ?: false
    }
}
