package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.presentation.components.PetHostOwner
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.rememberRoomAnchor
import ru.larpinovplay.finniapp.presentation.components.roomOrigin
import ru.larpinovplay.finniapp.presentation.components.roomPetSlot
import ru.larpinovplay.finniapp.presentation.screens.shop.icon
import ru.larpinovplay.finniapp.presentation.screens.shop.title
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Гардероб: купленная в магазине одежда. Сверху тот же 3D-питомец, что на главном экране (его рисует PetHost
 * в слоте этого экрана), поэтому надетая вещь сразу видна на нём.
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
        onToggle = viewModel::onToggle,
        onGoToShop = onGoToShop,
        onBack = onBack,
        petSlot = Modifier.onGloballyPositioned { petHost.setSlot(PetHostOwner.WARDROBE, it) },
        modifier = modifier,
    )
}

@Composable
fun WardrobeScreenContent(
    state: WardrobeUiState,
    onToggle: (ShopItem) -> Unit,
    onGoToShop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    petSlot: Modifier = Modifier,
) {
    // Фон ставится по слоту питомца: пол и тень на нём совпадают с тем, где стоит питомец
    val room = rememberRoomAnchor()
    Box(modifier = modifier.fillMaxSize().roomOrigin(room)) {
        RoomBackground(anchor = room)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Header(onBack)
            // Самый большой квадрат, который влезает в свободное место: 3D-питомец рисуется в квадратный буфер
            // (см. PetModel3D), и неквадратный слот растянул бы его. Прижат к низу, к полу над вещами.
            // Вещи и подсказка — под слотом, а не поверх него: вид питомца лежит над всем экраном
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    petSlot
                        .size(minOf(maxWidth, maxHeight))
                        .roomPetSlot(room)
                )
            }
            when {
                !state.supported -> Hint("Одежда пока есть только у котика", button = null)
                state.items.isEmpty() -> Hint(
                    "Здесь будут вещи ${state.petName}. Купи кепку, очки или бабочку в магазине — они останутся навсегда",
                    button = "В магазин" to onGoToShop,
                )
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.items.forEach {
                        WardrobeTile(it, onToggle = { onToggle(it.item) }, modifier = Modifier.weight(1f))
                    }
                    // Пустые места, чтобы одна-две вещи не растягивались на всю ширину
                    repeat(MAX_TILES - state.items.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private const val MAX_TILES = 3

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(onClick = onBack, shape = CircleShape, color = FinniColors.Lavender, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = FinniColors.Navy)
            }
        }
        Text("Гардероб", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
    }
}

/** Плитка вещи: картинка, название, место и кнопка. Плитки стоят в ряд, чтобы питомцу хватило места на полу. */
@Composable
private fun WardrobeTile(entry: WardrobeItem, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val item = entry.item
    WhiteCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(if (entry.worn) FinniColors.BlueLight else FinniColors.Lavender, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(item.icon), null, Modifier.size(36.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                if (entry.worn) "надето" else item.slot?.title.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = FinniColors.NavyMuted,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onToggle,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (entry.worn) FinniColors.LavenderDeep else FinniColors.Green,
                    contentColor = if (entry.worn) FinniColors.Navy else Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) { Text(if (entry.worn) "Снять" else "Надеть", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        }
    }
}

@Composable
private fun Hint(text: String, button: Pair<String, () -> Unit>?) {
    WhiteCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            button?.let { (label, onClick) ->
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun WhiteCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier.shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f)),
        shape = shape,
        color = FinniColors.Card,
        content = content
    )
}
