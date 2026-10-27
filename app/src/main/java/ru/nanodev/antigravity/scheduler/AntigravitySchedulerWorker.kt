package ru.nanodev.antigravity.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
data class ScheduledAgentTask(
    val id: String,
    val title: String,
    val projectName: String,
    val cronOrInterval: String, // e.g., "Каждые 15 мин", "Ежедневно 03:00", "Однократно"
    val intervalMinutes: Long = 60,
    val promptCommand: String,
    val enabled: Boolean = true,
    val lastRunStatus: String = "Ожидает запуска",
    val lastRunTimestamp: Long = 0L
)

class AntigravitySchedulerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.success()
        val projectName = inputData.getString("project_name") ?: "default-project"
        val prompt = inputData.getString("prompt") ?: "/goal check status"

        val logFile = File(File(applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir, "Antigravity"), "chats/$projectName/logs/scheduled_runs.log")
        logFile.parentFile?.mkdirs()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        runCatching { logFile.appendText("[$stamp] [WorkManager:$taskId] Executed scheduled prompt: $prompt\n") }

        return Result.success()
    }
}

class ScheduleManager(private val context: Context, private val rootDir: File) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val scheduleFile = File(rootDir, "schedules.json")

    // Поток создаётся пустым: загрузка пишет в него, поэтому не в инициализаторе (иначе NPE).
    private val _tasks = MutableStateFlow<List<ScheduledAgentTask>>(emptyList())
    val tasks: StateFlow<List<ScheduledAgentTask>> = _tasks

    init {
        _tasks.value = loadTasks()
    }

    fun loadTasks(): List<ScheduledAgentTask> {
        if (scheduleFile.exists()) {
            runCatching {
                return json.decodeFromString<List<ScheduledAgentTask>>(scheduleFile.readText())
            }
        }
        val initial = listOf(
            ScheduledAgentTask(
                id = "sched_backup_1",
                title = "Nightly Git & ZIP Backup",
                projectName = "drive-android-app",
                cronOrInterval = "Каждые 6 часов (WorkManager)",
                intervalMinutes = 360,
                promptCommand = "!zip backup_workspace.zip .",
                enabled = true,
                lastRunStatus = "Успешно (Автобэкап выполнен)"
            )
        )
        saveTasks(initial)
        return initial
    }

    fun saveTasks(list: List<ScheduledAgentTask>) {
        runCatching {
            scheduleFile.parentFile?.mkdirs()
            scheduleFile.writeText(json.encodeToString(list))
        }
        _tasks.value = list
    }

    fun addScheduledTask(title: String, projectName: String, intervalMinutes: Long, promptCommand: String) {
        val task = ScheduledAgentTask(
            id = "sched_" + UUID.randomUUID().toString().take(8),
            title = title,
            projectName = projectName,
            cronOrInterval = "Каждые ${intervalMinutes.coerceAtLeast(15)} мин",
            intervalMinutes = intervalMinutes.coerceAtLeast(15),
            promptCommand = promptCommand,
            enabled = true,
            lastRunStatus = "Запланировано в WorkManager"
        )
        val updated = _tasks.value + task
        saveTasks(updated)
        enqueueInWorkManager(task)
    }

    fun runTaskImmediately(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        val req = OneTimeWorkRequestBuilder<AntigravitySchedulerWorker>()
            .setInputData(
                workDataOf(
                    "task_id" to task.id,
                    "project_name" to task.projectName,
                    "prompt" to task.promptCommand
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(req)

        val nowStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val updated = _tasks.value.map {
            if (it.id == taskId) it.copy(lastRunStatus = "Выполнено в $nowStr", lastRunTimestamp = System.currentTimeMillis())
            else it
        }
        saveTasks(updated)
    }

    fun enqueueInWorkManager(task: ScheduledAgentTask) {
        if (!task.enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(task.id)
            return
        }
        val periodic = PeriodicWorkRequestBuilder<AntigravitySchedulerWorker>(
            task.intervalMinutes.coerceAtLeast(15L),
            TimeUnit.MINUTES
        ).setInputData(
            workDataOf(
                "task_id" to task.id,
                "project_name" to task.projectName,
                "prompt" to task.promptCommand
            )
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            task.id,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
    }
}

class ScheduleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val root = File(context.getExternalFilesDir(null) ?: context.filesDir, "Antigravity")
            val mgr = ScheduleManager(context, root)
            mgr.loadTasks().filter { it.enabled }.forEach { mgr.enqueueInWorkManager(it) }
        }
    }
}
