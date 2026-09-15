package ru.larpinovplay.finniapp.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Одна светлая схема без динамических цветов: детское приложение должно выглядеть
 * одинаково на любом устройстве и совпадать с иллюстрациями.
 */
private val FinniColorScheme = lightColorScheme(
    primary = FinniColors.Blue,
    onPrimary = Color.White,
    primaryContainer = FinniColors.BlueLight,
    onPrimaryContainer = FinniColors.Navy,
    secondary = FinniColors.BlueDeep,
    onSecondary = Color.White,
    secondaryContainer = FinniColors.Lavender,
    onSecondaryContainer = FinniColors.Navy,
    tertiary = FinniColors.Coral,
    onTertiary = Color.White,
    tertiaryContainer = FinniColors.Sunny,
    onTertiaryContainer = FinniColors.Navy,
    background = FinniColors.Wall,
    onBackground = FinniColors.Navy,
    surface = Color.White,
    onSurface = FinniColors.Navy,
    surfaceVariant = FinniColors.BlueLight,
    onSurfaceVariant = FinniColors.NavyMuted,
    outline = FinniColors.LavenderDeep,
    error = FinniColors.Warning,
    onError = Color.White,
)

@Composable
fun FinniAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FinniColorScheme,
        typography = Typography,
        content = content
    )
}
