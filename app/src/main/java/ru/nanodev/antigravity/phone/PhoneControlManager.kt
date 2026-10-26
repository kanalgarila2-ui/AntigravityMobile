package ru.nanodev.antigravity.phone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.AlarmClock

class PhoneControlManager(private val context: Context) {

    fun executePhoneAction(action: String, params: Map<String, String>, enabledInSettings: Boolean): String {
        if (!enabledInSettings) {
            return "DENIED: Переключатель «Управление телефоном» выключен в настройках Antigravity 2.0."
        }
        return try {
            when (action) {
                "battery" -> {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    "Заряд батареи: ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
                }
                "torch" -> {
                    val on = params["state"] == "on"
                    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val id = cm.cameraIdList.firstOrNull() ?: return "Вспышка не найдена"
                    cm.setTorchMode(id, on)
                    "Фонарик: ${if (on) "ВКЛ" else "ВЫКЛ"}"
                }
                "vibrate" -> {
                    val ms = params["ms"]?.toLongOrNull() ?: 300L
                    val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                    "Вибрация: $ms мс"
                }
                "volume" -> {
                    val level = params["level"]?.toIntOrNull() ?: 50
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max) / 100, 0)
                    "Громкость: $level%"
                }
                "clipboard_set" -> {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Antigravity", params["text"] ?: ""))
                    "Скопировано в буфер обмена"
                }
                "alarm" -> {
                    val hour = params["hour"]?.toIntOrNull() ?: 8
                    val min = params["minutes"]?.toIntOrNull() ?: 0
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, min)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Будильник установлен на %02d:%02d".format(hour, min)
                }
                "dial" -> {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${params["number"] ?: ""}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    "Открыт вызов ${params["number"]}"
                }
                else -> "Выполнено действие телефона: $action"
            }
        } catch (e: Exception) {
            "Ошибка: ${e.message}"
        }
    }
}
