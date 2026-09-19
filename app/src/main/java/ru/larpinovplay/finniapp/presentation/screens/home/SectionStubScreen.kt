package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Заглушка раздела в стиле приложения. Каждый раздел описан в docs/07-screens.md и
 * docs/03-processes.md; здесь только напоминание, что в нём должно быть.
 */
@Composable
fun SectionStubScreen(section: HomeSection, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val shape = RoundedCornerShape(28.dp)
            Surface(
                shape = shape,
                color = FinniColors.Card,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(section.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = sectionHint(section),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FinniColors.NavyMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text("Назад", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}

private fun sectionHint(section: HomeSection): String = when (section) {
    HomeSection.TASKS -> "Задания реализованы в TasksScreen"
    HomeSection.SHOP -> "Магазин реализован в ShopScreen"
    HomeSection.SAVINGS -> "Копилка реализована в SavingsScreen"
    HomeSection.PROGRESS -> "Прогресс реализован в ProgressScreen"
    HomeSection.SETTINGS -> "Настройки реализованы в SettingsScreen"
    HomeSection.ADULT -> "Арифметический барьер, цели приложения, прогресс без оценок, сброс и удаление профиля, демо-режим. (ТЗ 2.5.12, П11)"
}
