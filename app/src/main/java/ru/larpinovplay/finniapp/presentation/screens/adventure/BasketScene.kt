package ru.larpinovplay.finniapp.presentation.screens.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Корзина: отметить, что купить, уложившись в бюджет. Выбранное помечено галочкой и словом «беру»,
 * обязательное — «главное». Над списком — сколько выбрано из бюджета, чтобы считать было проще.
 */
@Composable
internal fun BasketScene(scene: AdventureScene.Basket, state: AdventureUiState, onAction: (AdventureAction) -> Unit) {
    val locked = state.check != null
    val over = state.basketSum > scene.budget
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(scene.text, style = MaterialTheme.typography.bodyLarge)
            Hint(scene.hint)
            Text(
                (if (over) "! " else "") + "Выбрано на ${state.basketSum} из ${scene.budget}",
                style = MaterialTheme.typography.titleMedium,
                color = if (over) FinniColors.Warning else FinniColors.Navy,
            )
            scene.items.forEachIndexed { index, item ->
                val picked = index in state.inBasket
                Surface(
                    onClick = { onAction(AdventureAction.ToggleBasketItem(index)) },
                    enabled = !locked,
                    shape = RoundedCornerShape(16.dp),
                    color = if (picked) FinniColors.CardMint else FinniColors.BlueLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { role = Role.Checkbox; selected = picked },
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (picked) "✓" else "○", style = MaterialTheme.typography.titleLarge, color = if (picked) FinniColors.Mood else FinniColors.NavyMuted)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge)
                            val tags = listOfNotNull("главное".takeIf { item.required }, "беру".takeIf { picked })
                            if (tags.isNotEmpty()) {
                                Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = FinniColors.NavyMuted)
                            }
                        }
                        Text("${item.price}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (!locked) {
                Button(
                    onClick = { onAction(AdventureAction.CheckBasket) },
                    enabled = state.inBasket.isNotEmpty(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Готово", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
