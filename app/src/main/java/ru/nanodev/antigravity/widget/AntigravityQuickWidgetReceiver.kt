package ru.nanodev.antigravity.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ru.nanodev.antigravity.MainActivity
import ru.nanodev.antigravity.R
import ru.nanodev.antigravity.accounts.AccountSwitcherManager
import ru.nanodev.antigravity.storage.DownloadsProjectManager

/**
 * Виджет быстрого запуска Antigravity 2.0 на домашний экран Android (`ru.nanodev.antigravity.widget`).
 * Позволяет в 1 касание:
 * - Запустить голосовой ввод RU+EN сразу в режиме `/goal`
 * - Открыть Remote Desktop
 * - Быстро переключить активный аккаунт Google Antigravity
 */
class AntigravityQuickWidgetReceiver : AppWidgetProvider() {

    companion object {
        const val ACTION_VOICE_GOAL = "ru.nanodev.antigravity.ACTION_WIDGET_VOICE_GOAL"
        const val ACTION_REMOTE_DESKTOP = "ru.nanodev.antigravity.ACTION_WIDGET_REMOTE_DESKTOP"
        const val ACTION_SWITCH_ACCOUNT = "ru.nanodev.antigravity.ACTION_WIDGET_SWITCH_ACCOUNT"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val projectManager = DownloadsProjectManager(context)
        val settings = projectManager.loadGlobalSettings()
        val activeAcc = settings.accounts.firstOrNull { it.isActive }?.email ?: "dev.user@gmail.com"
        val latestProject = projectManager.listProjects().firstOrNull()?.projectName ?: "drive-android-app"

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_antigravity_quick)
            views.setTextViewText(R.id.widget_account_badge, activeAcc)
            views.setTextViewText(R.id.widget_project_label, "📁 /Download/Antigravity/chats/$latestProject")

            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("quick_action", "voice_goal")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_voice_goal,
                PendingIntent.getActivity(context, 101, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val rdIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("quick_action", "remote_desktop")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_remote_desktop,
                PendingIntent.getActivity(context, 102, rdIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val switchAccIntent = Intent(context, AntigravityQuickWidgetReceiver::class.java).apply {
                action = ACTION_SWITCH_ACCOUNT
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_switch_acc,
                PendingIntent.getBroadcast(context, 103, switchAccIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SWITCH_ACCOUNT) {
            val pm = DownloadsProjectManager(context)
            val switcher = AccountSwitcherManager(pm)
            switcher.cycleToNextAccount(pm.loadGlobalSettings())
        }
    }
}
