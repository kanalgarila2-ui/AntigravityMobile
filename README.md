# 🚀 Google Antigravity 2.0 — Android 11+ Port (`v0.1.0`)

Полный локальный порт **Google Antigravity 2.0** и **Antigravity SDK** под **Android 11+ (API 30 — API 34)** с собственной мини-платформой на **115+ команд**, мостом к **Termux / Termux:API**, билингвальным распознаванием смешанной речи (**RU + EN**), встроенным фоновым модулем **AgyUnlocker** (порт `confeden/Antigravity`) и полной автоматической сборкой APK через **GitHub Actions** с многоуровневым кэшированием.

---

## ✨ Что реализовано в `v0.1.0`

1. **Самописный Antigravity 2.0 SDK Runtime (Kotlin + Python Bridge)**:
   - Так как официальный `google-antigravity` на ПК использует десктопный бинарник `language_server` / `agy` (x86_64), в проекте реализованы:
     - Нативный Kotlin-движок `com.antigravity.android.sdk` (`AntigravityAgentOrchestrator`, `ImmutableSystemPrompt`, `AntigravitySettings`).
     - Полный Python-пакет `app/src/main/assets/antigravity_sdk_android/` — 1-в-1 совместимый с `from google.antigravity import Agent, LocalAgentConfig, ToolRunner, ToolContext, CapabilitiesConfig, BuiltinTools, policy`.
2. **Локальная папка проектов в Downloads**:
   - Все проекты и чаты хранятся локально в `/storage/emulated/0/Download/Antigravity/chats/<имя_проекта>/` (`workspace/`, `artifacts/`, `chat_history.json`, `settings.json`, `logs/`).
   - При создании нового чата обязательно задаётся имя проекта.
3. **Неперезаписываемый Системный Промпт (`ImmutableSystemPrompt.kt`)**:
   - Защищён на уровне ядра SDK. Подробно объясняет агенту его локальное окружение в `Downloads/Antigravity/chats/<project_name>/`, как вызывать 115+ встроенных команд рантайма и мост Termux.
   - **Динамический блок `Управление телефоном`**: секция `<phone_control_capabilities status="ENABLED">` добавляется в системный промпт **ТОЛЬКО** тогда, когда пользователь включил индикатор **«Управление телефоном»** в настройках!
4. **Мини-платформа (Custom CMD на 115+ команд + Termux Bridge)**:
   - Встроенный `AntigravityShellEngine.kt` поддерживает 115+ команд (удаление, создание, редактирование `sed`/`grep`, скачивание `download`/`wget`/`curl`, архивация `zip`/`unzip`, хэширование `sha256sum`/`md5sum`, `base64`, `git`, сетевая диагностика и др.), транслируемых в Android/JVM Runtime.
   - Если на устройстве установлен **Termux** и **Termux:API**, команды могут прозрачно делегироваться в `com.termux.RUN_COMMAND`.
5. **Билингвальный голосовой ввод (RU + EN Code-Switching)**:
   - Живая транскрипция речи (`BilingualVoiceEngine.kt`) со словарём смешанной русско-английской технической лексики: фраза *«я делаю drive приложение и пушу в main ветку через git»* распознаётся без превращения английских терминов в кириллическую транслитерацию.
6. **Порт `confeden/Antigravity` (`AgyUnlockerService.kt`)**:
   - Фоновая служба без root и без системного VPN: локальный прокси `127.0.0.1:53129` (`AG_LS_PROXY`), пул DoH + кастомный IPv4 DNS с проверкой и патч ответа `ineligible` -> `inexigible` для снятия ошибки 400 `User location is not supported` на `cloudcode-pa.googleapis.com`.
7. **Полные настройки Antigravity 2.0 + «Разрешить всё!»**:
   - Поддержка **API-ключей (`GEMINI_API_KEY`)**, **аккаунтов Google (OAuth / CloudCode)** и **Vertex AI**.
   - Лимит шагов увеличен до **100+ команд** + переключатель **«Автовыключение лимита шагов»**.
   - В самом конце списка настроек добавлен подсвечивающийся блок **«Разрешить всё!»** с пульсирующей рамкой осторожности.

---

## 🛠️ Как собрать APK через GitHub Actions (с полным кэшированием)

1. Создайте новый репозиторий на GitHub и загрузите в него содержимое этого архива.
2. Перейдите во вкладку **Actions** -> workflow **`Build Antigravity 2.0 Android APK (v0.1.0) with Full Cache`** запустится автоматически при `push` или вручную через кнопку **Run workflow**.
3. Workflow кэширует Gradle Wrapper, зависимости, промежуточные слои компиляции Kotlin и Android SDK, собирает и подписывает **`Antigravity-2.0-Android-v0.1.0-release.apk`** и **`Antigravity-2.0-Android-v0.1.0-debug.apk`**, а также публикует их в **GitHub Releases (`v0.1.0`)**.
