package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardTitle
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Окно демо-режима по нажатию на плашку «Демо»: эксперт добавляет себе монет, чтобы проверить покупки и копилку без
 * ожидания, и выходит из демо — тогда возвращается игра ребёнка, отложенная на время демо.
 */
@Composable
fun DemoDialog(panel: HomeUiState.DemoPanel, onAction: (HomeAction) -> Unit) {
    CardDialog(onDismiss = { onAction(HomeAction.CloseDemo) }) {
        if (!panel.confirmExit) {
            CardTitle("Демо-режим", "Недели завершаются кнопкой, без ожидания")
            Text(
                panel.savedPetName?.let { "Игра с питомцем $it отложена и вернётся такой же после выхода из демо." }
                    ?: "Отложенной игры нет: после выхода из демо откроется создание питомца.",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
            )
            TealButton("Добавить ${GameRules.DEMO_COINS} монет", R.drawable.ic_coin, { onAction(HomeAction.AddDemoCoins) })
            SoftButton("Выйти из демо", { onAction(HomeAction.AskExitDemo) })
            SoftButton("Закрыть", { onAction(HomeAction.CloseDemo) })
        } else {
            CardTitle("Выйти из демо?", "Всё, что сделано в демо, удалится")
            Text(
                panel.savedPetName?.let { "Вернётся игра с питомцем $it — такой, какой она была до демо." }
                    ?: "Отложенной игры нет: откроется создание питомца.",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
            )
            TealButton("Выйти из демо", null, { onAction(HomeAction.ExitDemo) })
            SoftButton("Остаться в демо", { onAction(HomeAction.CloseDemo) })
        }
    }
}
