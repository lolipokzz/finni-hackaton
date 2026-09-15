package ru.larpinovplay.finniapp.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Крупный, жирный, округлый по духу шрифт. Пока системный (Roboto);
 * когда в проект добавят Nunito или похожую гарнитуру с лицензией OFL,
 * достаточно заменить [FinniFont].
 * Основной текст не меньше 16 sp (ТЗ 3.6).
 */
val FinniFont: FontFamily = FontFamily.Default

val Typography = Typography(
    headlineMedium = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FinniFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)
