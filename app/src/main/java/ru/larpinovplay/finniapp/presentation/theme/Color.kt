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
    val Wall = Color(0xFFE6EEFF)
    val WallBottom = Color(0xFFF3EEFA)
    val Card = Color(0xF5FFFFFF)         // белая карточка с лёгкой прозрачностью
    val CardPeach = Color(0xFFFFF3EA)
    val CardPink = Color(0xFFFFE8F1)
    val CardMint = Color(0xFFE8F8EF)
    val Track = Color(0xFFE9ECF7)        // фон шкал

    // Шкалы состояния
    val Satiety = Color(0xFFF9B92B)
    val Care = Color(0xFFFF7BA9)
    val Mood = Color(0xFF5CD69C)

    // Прочее
    val Green = Color(0xFF4CD37B)
    val Coral = Color(0xFFFF6F61)
    val Warning = Color(0xFFFF5A5F)
    val Sunny = Color(0xFFFFE7A3)

    // Комната
    val FloorTop = Color(0xFFF6E3C1)
    val FloorBottom = Color(0xFFEBCF9F)
    val RugLight = Color(0xFFF8F4EC)
    val RugRing = Color(0xFFBFD9F5)
    val WindowSkyTop = Color(0xFFBFE1FF)
    val WindowSkyBottom = Color(0xFFE3F4FF)
    val Beanbag = Color(0xFFF6D77A)
    val Plant = Color(0xFF8BCB8F)
}
