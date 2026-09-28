package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.DoneBadge
import ru.larpinovplay.finniapp.presentation.components.PetHostOwner
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.ScreenHeader
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.components.rememberRoomAnchor
import ru.larpinovplay.finniapp.presentation.components.roomOrigin
import ru.larpinovplay.finniapp.presentation.components.roomPetSlot
import ru.larpinovplay.finniapp.presentation.screens.shop.icon
import ru.larpinovplay.finniapp.presentation.screens.shop.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Гардероб: купленная в магазине одежда. Сверху тот же 3D-питомец, что на главном экране (его рисует PetHost
 * в слоте этого экрана), поэтому надетая вещь сразу видна на нём. Снизу — наклейки по местам: на голову,
 * на глаза, на шею; место, где ещё ничего нет, — пунктирное и ведёт в магазин.
 */
@Composable
fun WardrobeScreen(
    petHost: PetHostState,
    onGoToShop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WardrobeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Модель задаёт главный экран; здесь меняется только набор надетых вещей
    SideEffect { petHost.spec = petHost.spec?.copy(accessories = state.accessories) }
    WardrobeScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onGoToShop = onGoToShop,
        onBack = onBack,
        petSlot = Modifier.onGloballyPositioned { petHost.setSlot(PetHostOwner.WARDROBE, it) },
        modifier = modifier,
    )
}

@Composable
fun WardrobeScreenContent(
    state: WardrobeUiState,
    onAction: (WardrobeAction) -> Unit,
    onGoToShop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    petSlot: Modifier = Modifier,
) {
    // Фон ставится по слоту питомца: пол и тень на нём совпадают с тем, где стоит питомец
    val room = rememberRoomAnchor()
    Box(modifier = modifier.fillMaxSize().roomOrigin(room)) {
        RoomBackground(anchor = room)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            ScreenHeader("Гардероб", onBack)
            // Самый большой квадрат, который влезает в свободное место: 3D-питомец рисуется в квадратный буфер
            // (см. PetModel3D), и неквадратный слот растянул бы его. Прижат к низу, к полу над вещами.
            // Вещи — под слотом, а не поверх него: вид питомца лежит над всем экраном
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    petSlot
                        .size(minOf(maxWidth, maxHeight))
                        .roomPetSlot(room)
                )
            }
            // По наклейке на каждое место; если на одно место куплено несколько вещей, показываются все
            val tiles = WearableSlot.entries.flatMap { slot ->
                state.items.filter { it.item.slot == slot }.ifEmpty { listOf(null) }.map { slot to it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.forEach { (slot, entry) ->
                    if (entry != null) {
                        ItemTile(entry, onToggle = { onAction(WardrobeAction.Toggle(entry.item)) }, modifier = Modifier.weight(1f))
                    } else {
                        EmptySlotTile(slot, onGoToShop, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Оттенок наклейки по месту: как у разделов — тёплый, голубой, розовый. */
private val WearableSlot.tint: Color
    get() = when (this) {
        WearableSlot.HEAD -> Color(0xFFFFF0E6)
        WearableSlot.EYES -> Color(0xFFE6EEFF)
        WearableSlot.NECK -> FinniColors.DreamTint
    }

/**
 * Купленная вещь: наклейка, название и кнопка. Надетая — в бирюзовой рамке, с галочкой и словом «надето»,
 * не только цветом. Нажимается вся плитка.
 */
@Composable
private fun ItemTile(entry: WardrobeItem, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val item = entry.item
    val slot = item.slot
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onToggle,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .creamCard(shape, elevation = 8.dp)
            // Белая обводка наклейки рисуется поверх, поэтому бирюзовая рамка шире: видны её внутренние 3 dp
            .then(if (entry.worn) Modifier.border(7.dp, FinniColors.Teal, shape) else Modifier)
            .clearAndSetSemantics {
                role = Role.Switch
                selected = entry.worn
                contentDescription = "${item.name}, ${slot?.title?.lowercase().orEmpty()}: " + if (entry.worn) "надето. Снять" else "не надето. Надеть"
            },
    ) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                CardSticker(item.icon, slot?.tint ?: FinniColors.Pebble, size = 64.dp, iconScale = 0.62f)
                if (entry.worn) DoneBadge(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 3.dp), size = 24.dp)
            }
            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (entry.worn) "надето" else slot?.title?.lowercase().orEmpty(),
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = if (entry.worn) FinniColors.Teal else FinniColors.InkMuted,
                maxLines = 1,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (entry.worn) FinniColors.Pebble else FinniColors.Teal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (entry.worn) "Снять" else "Надеть",
                    fontSize = 15.sp, fontWeight = FontWeight.Black,
                    color = if (entry.worn) FinniColors.InkMuted else Color.White,
                )
            }
        }
    }
}

/** Место, где ещё нет вещи: пунктирный кружок и подсказка, куда за ней идти. */
@Composable
private fun EmptySlotTile(slot: WearableSlot, onGoToShop: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onGoToShop,
        shape = shape,
        color = FinniColors.Cream.copy(alpha = 0.75f),
        modifier = modifier.clearAndSetSemantics {
            role = Role.Button
            contentDescription = "${slot.title}: пока пусто. В магазин"
        },
    ) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(slot.tint.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(64.dp)) {
                    val w = 2.5.dp.toPx()
                    drawCircle(
                        FinniColors.InkMuted.copy(alpha = 0.45f), radius = size.minDimension / 2 - w / 2,
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                    )
                }
                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted.copy(alpha = 0.7f))
            }
            Text(slot.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted, maxLines = 1, textAlign = TextAlign.Center)
            Text("пока пусто", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.InkMuted, maxLines = 1)
            Box(
                Modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(FinniColors.ActionPeach),
                contentAlignment = Alignment.Center,
            ) {
                Text("В магазин", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.ActionPeachInk, maxLines = 1)
            }
        }
    }
}
