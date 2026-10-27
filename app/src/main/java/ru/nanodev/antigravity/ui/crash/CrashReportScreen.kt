package ru.nanodev.antigravity.ui.crash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Показывается после падения: текст ошибки можно скопировать и отправить. */
@Composable
fun CrashReportScreen(report: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Приложение упало в прошлый раз",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Скопируйте текст ниже и отправьте разработчику.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            report,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = { clipboard.setText(AnnotatedString(report)) }) {
                Text("Скопировать")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    }
}
