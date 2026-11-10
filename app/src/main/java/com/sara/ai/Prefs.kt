package com.sara.ai

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.applicationContext.getSharedPreferences("sara", Context.MODE_PRIVATE)
    fun get(c: Context, k: String, d: String = ""): String = sp(c).getString(k, d) ?: d
    fun set(c: Context, k: String, v: String) { sp(c).edit().putString(k, v).apply() }
    fun getF(c: Context, k: String, d: Float): Float = sp(c).getFloat(k, d)
    fun setF(c: Context, k: String, v: Float) { sp(c).edit().putFloat(k, v).apply() }
}
