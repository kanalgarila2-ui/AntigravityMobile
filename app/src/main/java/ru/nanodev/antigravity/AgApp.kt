package ru.nanodev.antigravity

import android.app.Application
import ru.nanodev.antigravity.crash.CrashLogger

/** Точка входа приложения: ставит перехватчик падений до первого экрана. */
class AgApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
