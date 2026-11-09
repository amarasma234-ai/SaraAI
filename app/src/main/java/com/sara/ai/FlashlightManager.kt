package com.sara.ai

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

object FlashlightManager {
    private var isOn = false

    private fun cameraId(cm: CameraManager): String? = cm.cameraIdList.firstOrNull {
        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    fun set(ctx: Context, on: Boolean): String {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(cm) ?: return "Is phone mein flashlight hardware nahi mila."
        return try {
            cm.setTorchMode(id, on); isOn = on
            if (on) "Flashlight on kar di." else "Flashlight band kar di."
        } catch (e: Exception) { "Flashlight error: ${e.message}" }
    }

    fun toggle(ctx: Context) = set(ctx, !isOn)
}
