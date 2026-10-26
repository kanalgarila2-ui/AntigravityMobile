package ru.nanodev.antigravity.storage

import android.content.Context
import android.os.Environment
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.nanodev.antigravity.sdk.AntigravitySettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val thoughtTrace: String = "",
    val toolName: String = "",
    val stepIndex: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ArtifactItem(
    val id: String,
    val title: String,
    val type: String,
    val relativePath: String,
    val status: String = "ready",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ProjectChatSession(
    val projectName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val folderPath: String,
    val worktreeMode: String = "local",
    val messages: List<ChatMessage> = emptyList(),
    val artifacts: List<ArtifactItem> = emptyList()
)

class DownloadsProjectManager(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun getAntigravityRootDir(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val root = File(downloadsDir, "Antigravity")
        if (!root.exists()) root.mkdirs()
        if (!root.canWrite()) {
            val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Antigravity")
            fallback.mkdirs()
            return fallback
        }
        return root
    }

    fun getChatsRootDir(): File = File(getAntigravityRootDir(), "chats").apply { mkdirs() }

    fun sanitizeProjectName(rawName: String): String {
        val cleaned = rawName.trim()
            .replace(Regex("[\\/:*?"<>|]"), "_")
            .replace(Regex("\s+"), "-")
        return cleaned.ifEmpty {
            "project-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        }
    }

    fun createOrOpenProjectChat(rawProjectName: String, worktreeMode: String = "local"): ProjectChatSession {
        val projectName = sanitizeProjectName(rawProjectName)
        val projectDir = File(getChatsRootDir(), projectName)
        val workspaceDir = File(projectDir, "workspace").apply { mkdirs() }
        val artifactsDir = File(projectDir, "artifacts").apply { mkdirs() }
        File(projectDir, "logs").mkdirs()

        val historyFile = File(projectDir, "chat_history.json")
        if (historyFile.exists()) {
            runCatching { return json.decodeFromString<ProjectChatSession>(historyFile.readText()) }
        }

        val initialPlanFile = File(artifactsDir, "implementation_plan.md")
        if (!initialPlanFile.exists()) {
            initialPlanFile.writeText("# Implementation Plan — $projectName\n- Path: `${workspaceDir.absolutePath}`\n")
        }

        val session = ProjectChatSession(
            projectName = projectName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            folderPath = projectDir.absolutePath,
            worktreeMode = worktreeMode,
            messages = listOf(
                ChatMessage(
                    id = "msg_init",
                    role = "agent",
                    content = "Локальный проект **$projectName** (`ru.nanodev.antigravity`) инициализирован в `${projectDir.absolutePath}`.\n" +
                        "Доступны: 118+ команд мини-платформы, MCP-сервер, оффлайн-модель LiteRT, планировщик `/schedule` и Remote Desktop.",
                    thoughtTrace = "Initialized /Download/Antigravity/chats/$projectName"
                )
            ),
            artifacts = listOf(
                ArtifactItem("art_1", "implementation_plan.md", "implementation_plan", "artifacts/implementation_plan.md")
            )
        )
        saveProjectSession(session)
        return session
    }

    fun saveProjectSession(session: ProjectChatSession) {
        val projectDir = File(getChatsRootDir(), session.projectName).apply { mkdirs() }
        File(projectDir, "chat_history.json").writeText(
            json.encodeToString(session.copy(updatedAt = System.currentTimeMillis()))
        )
    }

    fun listProjects(): List<ProjectChatSession> {
        val dirs = getChatsRootDir().listFiles()?.filter { it.isDirectory } ?: emptyList()
        val sessions = dirs.map { createOrOpenProjectChat(it.name) }.toMutableList()
        if (sessions.isEmpty()) {
            sessions.add(createOrOpenProjectChat("drive-android-app"))
        }
        return sessions.sortedByDescending { it.updatedAt }
    }

    fun saveGlobalSettings(settings: AntigravitySettings) {
        File(getAntigravityRootDir(), "settings.json").writeText(json.encodeToString(settings))
    }

    fun loadGlobalSettings(): AntigravitySettings {
        val file = File(getAntigravityRootDir(), "settings.json")
        if (file.exists()) {
            runCatching { return json.decodeFromString<AntigravitySettings>(file.readText()) }
        }
        return AntigravitySettings().also { saveGlobalSettings(it) }
    }
}
