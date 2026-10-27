package ru.nanodev.antigravity.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/** Общие кривые движения приложения. */
val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val EaseInOutSoft: Easing = CubicBezierEasing(0.45f, 0f, 0.2f, 1f)

/**
 * Каталог из 59 именованных анимаций приложения.
 * Каждая запись — длительность и кривая, которые используются в конкретном месте UI.
 */
enum class AgAnim(val durationMs: Int, val easing: Easing) {
    // Запуск и онбординг
    SPLASH_BACKDROP_DRIFT(9000, LinearEasing),
    SPLASH_LOGO_SCALE_IN(900, EaseOutExpo),
    SPLASH_LOGO_FADE_IN(600, FastOutSlowInEasing),
    SPLASH_TITLE_SLIDE_UP(700, EaseOutExpo),
    SPLASH_SUBTITLE_FADE(800, FastOutSlowInEasing),
    ONBOARD_CARD_ENTER(650, EaseOutExpo),
    ONBOARD_FIELD_FOCUS_BORDER(220, FastOutSlowInEasing),
    ONBOARD_VISIBILITY_TOGGLE(180, FastOutSlowInEasing),
    ONBOARD_BUTTON_PRESS(120, FastOutLinearInEasing),
    ONBOARD_CHECK_RING_PULSE(1400, EaseInOutSoft),
    ONBOARD_SPINNER_ROTATE(1100, LinearEasing),
    ONBOARD_SHAKE_ERROR(420, FastOutSlowInEasing),
    ONBOARD_SUCCESS_CHECK_DRAW(500, EaseOutExpo),
    ONBOARD_SUCCESS_EXIT_FADE(400, FastOutLinearInEasing),
    ONBOARD_ERROR_TEXT_SWAP(260, FastOutSlowInEasing),
    ONBOARD_STATUS_COLOR(300, FastOutSlowInEasing),

    // Чат
    CHAT_BUBBLE_ENTER(380, EaseOutExpo),
    CHAT_BUBBLE_EXPAND(260, FastOutSlowInEasing),
    CHAT_TYPING_DOT(600, EaseInOutSoft),
    CHAT_SEND_PULSE(240, FastOutSlowInEasing),
    CHAT_MIC_PULSE(900, EaseInOutSoft),
    CHAT_SCROLL_TO_END(320, FastOutSlowInEasing),
    CHAT_PROJECT_SHEET_SLIDE(340, EaseOutExpo),
    CHAT_TOOL_CARD_EXPAND(260, FastOutSlowInEasing),
    CHAT_INPUT_FOCUS_GLOW(200, FastOutSlowInEasing),

    // Навигация и вкладки
    NAV_INDICATOR_MORPH(300, EaseOutExpo),
    NAV_ICON_BOUNCE(360, EaseOutExpo),
    NAV_LABEL_FADE(180, FastOutSlowInEasing),
    TAB_CONTENT_FADE(220, FastOutSlowInEasing),
    TAB_CONTENT_SLIDE_LEFT(300, EaseOutExpo),
    TAB_CONTENT_SLIDE_RIGHT(300, EaseOutExpo),
    DRAWER_SLIDE(320, EaseOutExpo),
    GLASS_BORDER_SHIMMER(3200, EaseInOutSoft),
    BACKDROP_HUE_SHIFT(12000, LinearEasing),

    // Карточки и настройки
    CARD_ENTER_STAGGER(420, EaseOutExpo),
    CARD_PRESS_SCALE(110, FastOutLinearInEasing),
    SWITCH_THUMB_SLIDE(200, FastOutSlowInEasing),
    SLIDER_VALUE_TWEEN(240, FastOutSlowInEasing),
    CHIP_SELECT(180, FastOutSlowInEasing),
    REMOTE_BADGE_PULSE(1600, EaseInOutSoft),
    BETA_BADGE_FADE(260, FastOutSlowInEasing),
    SETTINGS_SECTION_EXPAND(280, FastOutSlowInEasing),
    WARNING_BORDER_BLINK(900, EaseInOutSoft),
    MODEL_CHIP_SWAP(200, FastOutSlowInEasing),
    TOGGLE_CARD_COLOR(240, FastOutSlowInEasing),
    SNACKBAR_SLIDE(280, EaseOutExpo),

    // Терминал и инструменты
    TERMINAL_LINE_APPEAR(160, FastOutSlowInEasing),
    TERMINAL_PROMPT_BLINK(530, LinearEasing),
    TOOL_LIST_ENTER(340, EaseOutExpo),
    TOOL_RUN_SPIN(900, LinearEasing),
    TASK_STATUS_COLOR(260, FastOutSlowInEasing),
    PROGRESS_LINEAR_SMOOTH(400, LinearOutSlowInEasing),
    EMPTY_STATE_FLOAT(2600, EaseInOutSoft),
    ERROR_BANNER_SLIDE(300, EaseOutExpo),

    // Диалоги и общие
    DIALOG_SCRIM_FADE(200, FastOutSlowInEasing),
    DIALOG_SCALE_IN(260, EaseOutExpo),
    SHEET_EXPAND(320, EaseOutExpo),
    DIALOG_EXIT(180, FastOutLinearInEasing),
    QUOTA_BAR_FILL(700, EaseOutExpo);

    /** Готовая tween-спецификация для animate*AsState / AnimatedVisibility. */
    fun <T> spec(): TweenSpec<T> = tween(durationMillis = durationMs, easing = easing)
}
