package com.sara.ai

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent

object AppPicker {
    fun show(a: Activity, onDone: () -> Unit) {
        val pm = a.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }.sortedBy { it.first.lowercase() }
        AlertDialog.Builder(a).setTitle("App chuno")
            .setItems(apps.map { it.first }.toTypedArray()) { _, i ->
                Prefs.set(a, "appLabel", apps[i].first); Prefs.set(a, "appPkg", apps[i].second); onDone()
            }
            .setNegativeButton("Koi nahi") { _, _ ->
                Prefs.set(a, "appLabel", ""); Prefs.set(a, "appPkg", ""); onDone()
            }.show()
    }
}
