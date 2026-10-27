package ru.nanodev.antigravity.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BilingualVoiceEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val ruToEnTechLexicon = mapOf(
        "гугл драйв" to "Google Drive",
        "драйв" to "drive",
        "антигравити" to "Antigravity",
        "гитхаб" to "GitHub",
        "пулл реквест" to "pull request",
        "коммит" to "commit",
        "гит" to "git",
        "мейн" to "main",
        "брэнч" to "branch",
        "билд" to "build",
        "сдк" to "SDK",
        "апи" to "API",
        "апк" to "APK",
        "термукс" to "Termux",
        "пайтон" to "Python",
        "котлин" to "Kotlin",
        "андроид" to "Android",
        "воркспейс" to "workspace",
        "мсп" to "MCP",
        "ремоут десктоп" to "Remote Desktop"
    )

    fun normalizeMixedRuEnSpeech(rawText: String, enableTechCodeSwitch: Boolean = true): String {
        if (!enableTechCodeSwitch || rawText.isBlank()) return rawText
        var result = rawText
        for ((cyrillic, english) in ruToEnTechLexicon.entries.sortedByDescending { it.key.length }) {
            val regex = Regex("(?<![а-яёa-z0-9])${Regex.escape(cyrillic)}(?![а-яёa-z0-9])", RegexOption.IGNORE_CASE)
            result = regex.replace(result, english)
        }
        return result
    }

    fun startLiveTranscription(
        enableTechCodeSwitch: Boolean = true,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("SpeechRecognizer unavailable")
            return
        }
        stopListening()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false }
                override fun onError(error: Int) {
                    _isListening.value = false
                    onError("Error $error")
                }
                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val raw = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    onFinalResult(normalizeMixedRuEnSpeech(raw, enableTechCodeSwitch))
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val raw = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (raw.isNotEmpty()) onPartialResult(normalizeMixedRuEnSpeech(raw, enableTechCodeSwitch))
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "ru-RU"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
        _isListening.value = false
    }
}
