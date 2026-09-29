package ru.larpinovplay.finniapp.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра приложения по референсу: пастельная детская комната,
 * белые полупрозрачные карточки, тёмно-синий текст, сочные акценты.
 */
object FinniColors {
    // Текст
    val Navy = Color(0xFF2E3A6E)         // основной текст и цифры
    val NavyMuted = Color(0xFF7078A3)    // подписи

    // Акцент (кнопки, выбранный раздел, карточка задания)
    val Blue = Color(0xFF5B7BFF)
    val BlueDeep = Color(0xFF4A66E8)
    val BlueLight = Color(0xFFE4EBFF)
    val Lavender = Color(0xFFD6DDFF)
    val LavenderDeep = Color(0xFFB9C6FF)

    // Поверхности
    val Wall = Color(0xFFB095D9)         // верх стены на фоне-иллюстрации: виден под системными панелями
    val Card = Color(0xF5FFFFFF)         // белая карточка с лёгкой прозрачностью
    val CardMint = Color(0xFFE8F8EF)
    val Track = Color(0xFFE9ECF7)        // фон шкал

    // Шкала настроения в заданиях
    val Mood = Color(0xFF5CD69C)

    // Прочее
    val Coral = Color(0xFFFF6F61)
    val Warning = Color(0xFFFF5A5F)
    val Sunny = Color(0xFFFFE7A3)

    // Прилавок в приключениях: тёплое дерево
    val Counter = Color(0xFFF6E3C1)
    val CounterEdge = Color(0xFFEBCF9F)

    // Карточки-наклейки главного экрана и его окон: кремовая поверхность, толстая белая обводка
    val Cream = Color(0xFFFFFDF6)
    val Ink = Color(0xFF1F3B4D)          // текст на кремовом
    val InkMuted = Color(0xFF3D5566)
    val Teal = Color(0xFF0F7F6A)         // «сделано», стадия, главная кнопка
    val TealBright = Color(0xFF2EC4A6)
    val Dashed = Color(0xFFEDE7DA)       // пунктир и разделители на кремовом
    val Pebble = Color(0xFFF2EFE6)       // круглые служебные кнопки: закрыть, настройки
    val ActionPeach = Color(0xFFFFB48F)  // кнопка действия в реплике Финни
    val ActionPeachInk = Color(0xFF5A2410)
    val CoinPill = Color(0xFFFFF1C7)
    val CoinInk = Color(0xFF7A4B00)
    val SatietyRing = Color(0xFFFFA24C)
    val SatietyTrack = Color(0xFFFFF3E2)
    val MoodRing = Color(0xFF2EC4A6)
    val MoodTrack = Color(0xFFE2F7F1)
    val DreamRing = Color(0xFFFF6FA8)
    val DreamTrack = Color(0xFFFBD9E8)
    val PawNew = Color(0xFFFFB020)       // шаг роста, заработанный на этой неделе
    val PawEmpty = Color(0xFFD6ECE6)
    val DeedPending = Color(0xFFFF9A62)
    val WarnInk = Color(0xFFB4471B)      // мягкое предупреждение: «сверх плана», «не вышло»
    val WarnTint = Color(0xFFFFF0E6)
    val DreamTint = Color(0xFFFFE6F0)    // цель и копилка
}
