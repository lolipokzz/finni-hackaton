package ru.larpinovplay.finniapp.presentation.screens.adventure

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.presentation.adventure.AdventureLook
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Корзина: отметить, что купить, уложившись в бюджет. Над списком — полоска бюджета: сколько выбрано из сколько,
 * перебор — тёплым цветом и знаком «!». Выбранное помечено галочкой, обязательное — меткой «главное».
 */
@Composable
internal fun BasketScene(scene: AdventureScene.Basket, state: AdventureUiState, look: AdventureLook, onAction: (AdventureAction) -> Unit) {
    val locked = state.check != null
    val over = state.basketSum > scene.budget
    SceneCard(R.drawable.ic_nav_shop, "Покупки", look) {
        BodyText(scene.text)
        Hint(scene.hint)
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = (if (over) "Перебор! " else "") + "Выбрано на ${state.basketSum} из ${scene.budget}"
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (over) "! Больше, чем есть" else "Выбрано",
                    fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (over) FinniColors.WarnInk else FinniColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(20.dp))
                Text(" ${state.basketSum}", fontSize = 17.sp, fontWeight = FontWeight.Black, color = if (over) FinniColors.WarnInk else FinniColors.CoinInk)
                Text(" из ${scene.budget}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
            }
            val shown by animateFloatAsState((state.basketSum.toFloat() / scene.budget).coerceIn(0f, 1f), label = "basket")
            Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(FinniColors.Dashed)) {
                if (shown > 0f) {
                    Box(Modifier.fillMaxWidth(shown).fillMaxHeight().clip(CircleShape).background(if (over) FinniColors.DeedPending else FinniColors.TealBright))
                }
            }
        }
        scene.items.forEachIndexed { index, item ->
            val picked = index in state.inBasket
            Surface(
                checked = picked,
                onCheckedChange = { onAction(AdventureAction.ToggleBasketItem(index)) },
                enabled = !locked,
                shape = RoundedCornerShape(20.dp),
                color = if (picked) FinniColors.CardMint else Color.White,
                border = BorderStroke(2.dp, if (picked) FinniColors.TealBright else FinniColors.Dashed),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics { role = Role.Checkbox },
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickMark(picked)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                        if (item.required) {
                            Text(
                                "главное", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFA23566),
                                modifier = Modifier.clip(CircleShape).background(FinniColors.DreamTint).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.size(22.dp))
                    Text("${item.price}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                }
            }
        }
        if (!locked) {
            TealButton("Готово", R.drawable.ic_check, { onAction(AdventureAction.CheckBasket) }, enabled = state.inBasket.isNotEmpty())
        }
    }
}
