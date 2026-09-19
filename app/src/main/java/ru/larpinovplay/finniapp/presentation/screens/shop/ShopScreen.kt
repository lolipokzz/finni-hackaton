package ru.larpinovplay.finniapp.presentation.screens.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Магазин, ТЗ 2.5.6: товары двух типов; до покупки видны цена, категория и влияние на питомца;
 * покупка требует подтверждения; при нехватке монет — объяснение и варианты, а не просто отказ.
 */
@Composable
fun ShopScreen(
    onGoToTasks: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShopScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onGoToTasks = onGoToTasks,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ShopScreenContent(
    state: ShopUiState,
    onAction: (ShopAction) -> Unit,
    onGoToTasks: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            ShopHeader(balance = state.balance, onBack = onBack)
            Spacer(Modifier.height(12.dp))
            CategoryTabs(selected = state.tab, onSelect = { onAction(ShopAction.TabSelected(it)) })
            Spacer(Modifier.height(10.dp))
            if (state.tab == ShopCategory.MANDATORY) {
                NeedsChecklist(state.foodCovered)
                Spacer(Modifier.height(10.dp))
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.items, key = { it.id }) { item ->
                    ShopItemCard(item = item, affordable = item.price <= state.balance, onBuy = { onAction(ShopAction.BuyClicked(item)) })
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    state.pending?.let { item ->
        PurchaseConfirmDialog(
            item = item,
            balance = state.balance,
            onConfirm = { onAction(ShopAction.ConfirmPurchase) },
            onDismiss = { onAction(ShopAction.DismissPending) }
        )
    }
    state.feedback?.let { fb ->
        when (fb) {
            is PurchaseFeedback.Bought -> BoughtDialog(fb, onDismiss = { onAction(ShopAction.DismissFeedback) })
            is PurchaseFeedback.NotEnough -> NotEnoughDialog(
                fb,
                onGoToTasks = { onAction(ShopAction.DismissFeedback); onGoToTasks() },
                onPick = { onAction(ShopAction.PickCheaper(it)) },
                onDismiss = { onAction(ShopAction.DismissFeedback) }
            )
        }
    }
}

// ---------- Шапка и вкладки ----------

@Composable
private fun ShopHeader(balance: Int, onBack: () -> Unit) {
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
        Text("Магазин", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        WhiteCard(shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.ic_coin), null, Modifier.size(26.dp))
                Text("$balance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: ShopCategory, onSelect: (ShopCategory) -> Unit) {
    WhiteCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ShopCategory.entries.forEach { category ->
                val isSelected = category == selected
                Surface(
                    onClick = { onSelect(category) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) FinniColors.Blue else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            category.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else FinniColors.Navy
                        )
                    }
                }
            }
        }
    }
}

/** Чек-лист обязательного на неделю (docs/03-processes.md, П5). Статус словом, не только галочкой. */
@Composable
private fun NeedsChecklist(foodCovered: Boolean) {
    WhiteCard(modifier = Modifier.fillMaxWidth(), color = FinniColors.CardPeach, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (foodCovered) "✓" else "○", style = MaterialTheme.typography.titleLarge, color = if (foodCovered) FinniColors.Mood else FinniColors.NavyMuted)
            Column(Modifier.padding(start = 10.dp)) {
                Text("Еда на неделю", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (foodCovered) "Куплено. Финни будет сыт" else "Ещё не куплено. Это важнее игрушек",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinniColors.NavyMuted
                )
            }
        }
    }
}

// ---------- Карточка товара ----------

@Composable
private fun ShopItemCard(item: ShopItem, affordable: Boolean, onBuy: () -> Unit) {
    WhiteCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(FinniColors.BlueLight, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(item.icon), null, Modifier.size(42.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.category.title, style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
                Text(item.effectText, style = MaterialTheme.typography.bodyMedium, color = FinniColors.Navy)
                Text(item.hint, style = MaterialTheme.typography.labelSmall, color = FinniColors.NavyMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.price}", style = MaterialTheme.typography.titleLarge)
                    Image(painterResource(R.drawable.ic_coin), null, Modifier.padding(start = 4.dp).size(20.dp))
                }
                // Кнопка активна всегда: попытка купить при нехватке — учебная ситуация (ТЗ 2.5.6)
                Button(
                    onClick = onBuy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (affordable) FinniColors.Green else FinniColors.LavenderDeep,
                        contentColor = if (affordable) Color.White else FinniColors.Navy
                    ),
                    modifier = Modifier.height(44.dp)
                ) { Text("Купить", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

// ---------- Диалоги ----------

@Composable
private fun PurchaseConfirmDialog(item: ShopItem, balance: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val remaining = balance - item.price
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        icon = { Image(painterResource(item.icon), null, Modifier.size(56.dp)) },
        title = { Text("Купить ${item.name.lowercase()}?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                InfoLine("Цена", "${item.price} монет")
                InfoLine("Категория", item.category.title)
                InfoLine("Эффект", item.effectText)
                InfoLine(
                    if (remaining >= 0) "Останется" else "Не хватает",
                    if (remaining >= 0) "$remaining монет" else "${-remaining} монет"
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Купить", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Не сейчас", style = MaterialTheme.typography.labelLarge) }
        }
    )
}

@Composable
private fun BoughtDialog(fb: PurchaseFeedback.Bought, onDismiss: () -> Unit) {
    val item = fb.item
    val explanation = if (item.category == ShopCategory.MANDATORY)
        "${item.name} — это нужное. Финни поел и доволен!"
    else
        "${item.name} порадовал Финни. Помни: это желаемое, а не еда"
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        icon = { Image(painterResource(item.icon), null, Modifier.size(56.dp)) },
        title = { Text("Что изменилось", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoLine("Монеты", "−${item.price}, осталось ${fb.balanceAfter}")
                InfoLine("Питомец", item.effectText)
                Spacer(Modifier.height(8.dp))
                Text(explanation, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Понятно", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/** Нехватка средств: сколько не хватает и что можно сделать (docs/03-processes.md, П5). */
@Composable
private fun NotEnoughDialog(
    fb: PurchaseFeedback.NotEnough,
    onGoToTasks: () -> Unit,
    onPick: (ShopItem) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Не хватает ${fb.missing} монет", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${fb.item.name} стоит ${fb.item.price}. Вот что можно сделать:", style = MaterialTheme.typography.bodyLarge)
                OptionButton("Выполнить задание и заработать", onGoToTasks)
                fb.cheaper.forEach { OptionButton("Выбрать дешевле: ${it.name} за ${it.price}") { onPick(it) } }
                OptionButton("Подождать следующую неделю", onDismiss)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Закрыть", style = MaterialTheme.typography.labelLarge) }
        }
    )
}

@Composable
private fun OptionButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = FinniColors.BlueLight,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = FinniColors.Navy)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    color: Color = FinniColors.Card,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f)),
        shape = shape,
        color = color,
        content = content
    )
}
