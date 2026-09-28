package ru.larpinovplay.finniapp.presentation.screens.shop

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import ru.larpinovplay.finniapp.presentation.components.spotlightTarget
import ru.larpinovplay.finniapp.presentation.components.TutorialSpotlight
import ru.larpinovplay.finniapp.presentation.components.SpotlightTargets
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.engine.GameRules
import ru.larpinovplay.finniapp.domain.game.model.BudgetDirection
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.presentation.components.BackButton
import ru.larpinovplay.finniapp.presentation.components.CardDialog
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.CoinPill
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.EffectChip
import ru.larpinovplay.finniapp.presentation.components.OnRoomLabel
import ru.larpinovplay.finniapp.presentation.components.PebbleButton
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.SoftButton
import ru.larpinovplay.finniapp.presentation.components.StatRow
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.screens.home.barColor
import ru.larpinovplay.finniapp.presentation.screens.home.sticker
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Магазин, ТЗ 2.5.6: товары двух типов; до покупки видны цена, категория и влияние на питомца;
 * покупка требует подтверждения; при нехватке монет — объяснение и варианты, а не просто отказ.
 *
 * Тот же язык наклеек, что у главного экрана и окон недели: кремовые карточки в белой обводке,
 * у категорий — наклейки дел, к которым они ведут (обязательное — еда, необязательное — радость).
 */
@Composable
fun ShopScreen(
    onGoToTasks: () -> Unit,
    onGoToWardrobe: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShopScreenContent(
        state = state,
        onAction = viewModel::onAction,
        onGoToTasks = onGoToTasks,
        onGoToWardrobe = onGoToWardrobe,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ShopScreenContent(
    state: ShopUiState,
    onAction: (ShopAction) -> Unit,
    onGoToTasks: () -> Unit,
    onGoToWardrobe: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Обучение: нажать можно только еду, а какую купить — решает ребёнок
    val goods = remember { SpotlightTargets() }
    val list = rememberLazyGridState()
    val coaching = state.coach && state.pending == null && state.feedback == null
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack)
                Text(
                    "Магазин",
                    style = OnRoomLabel.copy(fontSize = 26.sp),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                CoinPill(state.balance)
            }
            Spacer(Modifier.height(14.dp))
            CategoryTabs(selected = state.tab, onSelect = { onAction(ShopAction.TabSelected(it)) })
            Spacer(Modifier.height(12.dp))
            // Товары — плитки по три: картинка, название, цена. Что даёт вещь, видно в окне покупки
            LazyVerticalGrid(
                state = list,
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f),
            ) {
                val budget = state.budgets[state.tab]
                if (budget != null || state.tab == ShopCategory.MANDATORY) {
                    item(key = "plan-${state.tab}", span = { GridItemSpan(maxLineSpan) }) {
                        PlanCard(state.tab, budget, state.weekSatiety)
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    ShopItemTile(
                        item = item,
                        affordable = item.price <= state.balance,
                        owned = item.id in state.owned,
                        onBuy = { onAction(ShopAction.BuyClicked(item)) },
                        modifier = if (coaching) Modifier.spotlightTarget(goods, item.id) else Modifier,
                    )
                }
            }
        }
        if (coaching) {
            // Магазин всегда открывается на «Обязательном», а вкладки под затемнением не нажать
            TutorialSpotlight(
                "Еда — это обязательное: без неё мне плохо. Выбери, что мне купить!",
                goods.all,
                scroll = list,
                onSkip = { onAction(ShopAction.SkipTutorialStep) },
            )
        }
    }

    state.pending?.let { item ->
        PurchaseConfirmDialog(
            item = item,
            balance = state.balance,
            budget = state.budgets[item.category],
            onConfirm = { onAction(ShopAction.ConfirmPurchase) },
            onDismiss = { onAction(ShopAction.DismissPending) }
        )
    }
    state.feedback?.let { fb ->
        when (fb) {
            is PurchaseFeedback.Bought -> BoughtDialog(
                fb,
                onGoToWardrobe = { onAction(ShopAction.DismissFeedback); onGoToWardrobe() },
                onDismiss = { onAction(ShopAction.DismissFeedback) },
            )
            is PurchaseFeedback.NotEnough -> NotEnoughDialog(
                fb,
                onGoToTasks = { onAction(ShopAction.DismissFeedback); onGoToTasks() },
                onPick = { onAction(ShopAction.PickCheaper(it)) },
                onTakeFromSavings = { onAction(ShopAction.BuyWithSavings) },
                onDismiss = { onAction(ShopAction.DismissFeedback) }
            )
        }
    }
}

// ---------- Цвета и наклейки категорий: те же, что у дел недели и плана ----------

private val ShopCategory.deed: Deed
    get() = when (this) {
        ShopCategory.MANDATORY -> Deed.FED
        ShopCategory.OPTIONAL -> Deed.NOT_BORED
    }

private val ShopCategory.direction: BudgetDirection
    get() = when (this) {
        ShopCategory.MANDATORY -> BudgetDirection.MANDATORY
        ShopCategory.OPTIONAL -> BudgetDirection.OPTIONAL
    }

private val ShopCategory.tint: Color get() = deed.sticker.second

private val SatietyTint = Color(0xFFFFF0E6)
private val MoodTint = Color(0xFFFFF5C9)
private val WarnInk = FinniColors.WarnInk

// ---------- Вкладки ----------

/** Две вкладки-наклейки в одной кремовой плашке. Выбранная — бирюзовая, как главные кнопки. */
@Composable
private fun CategoryTabs(selected: ShopCategory, onSelect: (ShopCategory) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .creamCard(CircleShape, elevation = 8.dp)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ShopCategory.entries.forEach { category ->
            val isSelected = category == selected
            Surface(
                onClick = { onSelect(category) },
                shape = CircleShape,
                color = if (isSelected) FinniColors.Teal else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(category.tint).border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Image(painterResource(category.deed.sticker.first), null, Modifier.size(20.dp)) }
                    Text(
                        category.title,
                        fontSize = 15.sp, fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.White else FinniColors.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ---------- План категории и еда на неделю ----------

/**
 * Карточка над товарами: сколько по плану осталось на эту категорию, и — у обязательного —
 * куплено ли еды на неделю (дело «Финни сыт»). Перерасход помечен знаком и словами, не только цветом.
 */
@Composable
private fun PlanCard(category: ShopCategory, budget: ShopUiState.CategoryBudget?, weekSatiety: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(26.dp), elevation = 8.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        budget?.let { b ->
            val over = b.left < 0
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = if (over) "Сверх плана на ${-b.left}. Потрачено ${b.spent} из ${b.planned}"
                    else "По плану осталось ${b.left} из ${b.planned}"
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (over) "! Сверх плана на ${-b.left}" else "По плану осталось",
                        fontSize = 15.sp, fontWeight = FontWeight.Black,
                        color = if (over) WarnInk else FinniColors.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (!over) {
                        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(20.dp))
                        Text(
                            " ${b.left}",
                            fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk,
                        )
                        Text(" из ${b.planned}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                    }
                }
                // Полоска — остаток: тает с каждой покупкой; сверх плана — вся тёплая, как предупреждение
                Meter(
                    fraction = when {
                        over -> 1f
                        b.planned > 0 -> b.left.toFloat() / b.planned
                        else -> 0f
                    },
                    color = if (over) FinniColors.DeedPending else category.direction.barColor,
                )
            }
        }
        if (category == ShopCategory.MANDATORY) {
            if (budget != null) DashedDivider()
            FoodLine(weekSatiety)
        }
    }
}

/** Дело «Финни сыт»: сколько сытости куплено из нужных на неделю (docs/03-processes.md, П5). */
@Composable
private fun FoodLine(weekSatiety: Int) {
    val need = GameRules.WEEKLY_HUNGER
    val done = weekSatiety >= need
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "Еда на неделю: куплено $weekSatiety сытости из $need" + if (done) ". Сделано" else ""
        },
    ) {
        DeedMark(done)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Еда на неделю", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                Text("${weekSatiety.coerceAtMost(need)} из $need", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (done) FinniColors.Teal else FinniColors.InkMuted)
            }
            Meter(fraction = weekSatiety.toFloat() / need, color = if (done) FinniColors.TealBright else FinniColors.SatietyRing)
            if (!done) Text("Сначала еда — потом радости", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        }
    }
}

/** Отметка дела: бирюзовый кружок с галочкой или пунктирное место под наклейку — как в карточке дел. */
@Composable
private fun DeedMark(done: Boolean) {
    if (done) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(FinniColors.Teal).border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp)) }
    } else {
        Box(Modifier.size(36.dp).clip(CircleShape).background(SatietyTint), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(36.dp)) {
                val w = 2.5.dp.toPx()
                drawCircle(
                    FinniColors.DeedPending, radius = size.minDimension / 2 - w / 2,
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
            Image(painterResource(R.drawable.ic_meter_apple), null, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Meter(fraction: Float, color: Color) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "meter")
    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(FinniColors.Dashed)) {
        if (shown > 0f) Box(Modifier.fillMaxWidth(shown).fillMaxHeight().clip(CircleShape).background(color))
    }
}

// ---------- Карточка товара ----------

/** Что даёт вещь: наклейки-числа сытости и настроения; у одежды — ещё «навсегда». */
@Composable
private fun EffectChips(item: ShopItem, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (item.satiety != 0) EffectChip(R.drawable.ic_meter_apple, item.satiety, SatietyTint)
        if (item.mood != 0) EffectChip(R.drawable.ic_meter_smile, item.mood, MoodTint)
        if (item.isWearable) {
            Text(
                "навсегда", fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal,
                modifier = Modifier.clip(CircleShape).background(FinniColors.CardMint).padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * Плитка товара: крупная картинка, название и цена. Нажимается вся плитка — дальше окно покупки,
 * где видно, что вещь даст Финни. Нажать можно и когда монет не хватает: это учебная ситуация (ТЗ 2.5.6),
 * тогда цена просто спокойнее. Одежду, которая уже есть, второй раз не купить.
 */
@Composable
private fun ShopItemTile(item: ShopItem, affordable: Boolean, owned: Boolean, onBuy: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onBuy,
        enabled = !owned,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .creamCard(shape, elevation = 8.dp)
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = when {
                    owned -> "${item.name}: уже есть в гардеробе"
                    affordable -> "${item.name}, ${item.price} монет. Купить"
                    else -> "${item.name}, ${item.price} монет. Монет не хватает"
                }
            },
    ) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardSticker(item.icon, item.category.tint, size = 78.dp, iconScale = 0.66f)
            Text(
                item.name,
                fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink,
                textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            PriceTag(item.price, affordable, owned)
        }
    }
}

/** Цена на плитке: персиковая, если хватает монет, спокойная — если нет; у купленной одежды — «есть». */
@Composable
private fun PriceTag(price: Int, affordable: Boolean, owned: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    owned -> FinniColors.CardMint
                    affordable -> FinniColors.ActionPeach
                    else -> FinniColors.Pebble
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (owned) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(FinniColors.Teal), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_check), null, Modifier.size(10.dp))
            }
            Text("есть", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal)
        } else {
            Image(painterResource(R.drawable.ic_coin), null, Modifier.size(24.dp))
            Text(
                "$price", fontSize = 18.sp, fontWeight = FontWeight.Black,
                color = if (affordable) FinniColors.ActionPeachInk else FinniColors.InkMuted,
            )
        }
    }
}

// ---------- Окна покупки ----------

/** Шапка окна покупки: наклейка товара, крупная строка и бирюзовая подстрока. */
@Composable
private fun ItemHeader(item: ShopItem, title: String, subtitle: String, done: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box {
            CardSticker(item.icon, item.category.tint, size = 72.dp)
            if (done) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FinniColors.Teal)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp)) }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, modifier = Modifier.semantics { heading() })
            Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal)
        }
    }
}

@Composable
private fun PurchaseConfirmDialog(
    item: ShopItem,
    balance: Int,
    budget: ShopUiState.CategoryBudget?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val remaining = balance - item.price
    CardDialog(onDismiss = onDismiss) {
        ItemHeader(item, "Купить ${item.name.lowercase()}?", "${item.category.title} · ${item.price} монет")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Финни получит", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
            EffectChips(item)
        }
        Text(item.hint, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (remaining >= 0) StatRow("Останется монет", "$remaining", coin = true)
            else StatRow("Не хватает монет", "${-remaining}", coin = true, warn = true)
            budget?.let {
                val leftAfter = it.left - item.price
                // Покупку не запрещаем: план — намерение ребёнка, а не запрет
                if (leftAfter >= 0) StatRow("По плану останется", "$leftAfter из ${it.planned}")
                else StatRow("Сверх плана", "! ${-leftAfter}", warn = true)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TealButton("Купить", R.drawable.ic_coin, onConfirm)
            SoftButton("Не сейчас", onDismiss)
        }
    }
}

@Composable
private fun BoughtDialog(fb: PurchaseFeedback.Bought, onGoToWardrobe: () -> Unit, onDismiss: () -> Unit) {
    val item = fb.item
    val explanation = when {
        item.category == ShopCategory.MANDATORY -> "${item.name} — это обязательное. Финни поел и доволен!"
        item.isWearable -> "${item.name} теперь в гардеробе навсегда. Надень это Финни! Помни: это необязательное, а не еда"
        else -> "${item.name} порадовал Финни. Помни: это необязательное, а не еда"
    }
    CardDialog(onDismiss = onDismiss) {
        ItemHeader(item, "Куплено!", item.name, done = true)
        Text(explanation, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Финни получил", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
            EffectChips(item)
        }
        DashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (fb.fromSavings > 0) StatRow("Взято из копилки", "${fb.fromSavings}", coin = true)
            StatRow("Потрачено", "${item.price}", coin = true)
            StatRow("Осталось монет", "${fb.balanceAfter}", coin = true)
        }
        if (item.isWearable) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TealButton("Надеть", R.drawable.ic_nav_wardrobe, onGoToWardrobe)
                SoftButton("Потом", onDismiss)
            }
        } else {
            TealButton("Понятно", R.drawable.ic_check, onDismiss)
        }
    }
}

/** Нехватка средств: сколько не хватает и что можно сделать (docs/03-processes.md, П5). */
@Composable
private fun NotEnoughDialog(
    fb: PurchaseFeedback.NotEnough,
    onGoToTasks: () -> Unit,
    onPick: (ShopItem) -> Unit,
    onTakeFromSavings: () -> Unit,
    onDismiss: () -> Unit,
) {
    CardDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Не хватает ${fb.missing} монет",
                    fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text("${fb.item.name} стоит ${fb.item.price}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal)
            }
            PebbleButton(R.drawable.ic_close, "Закрыть", onDismiss)
        }
        Text("Что можно сделать", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (fb.canTakeFromSavings) {
                OptionRow(R.drawable.ic_deed_pig, Color(0xFFFFE6F0), "Взять ${fb.missing} из копилки и купить", savingsConsequence(fb), onTakeFromSavings)
            }
            OptionRow(R.drawable.ic_nav_tasks, Color(0xFFE6F8F2), "Заработать на задании", null, onGoToTasks)
            fb.cheaper.forEach { cheaper ->
                OptionRow(cheaper.icon, cheaper.category.tint, "Выбрать дешевле: ${cheaper.name}", "${cheaper.price} монет · ${cheaper.effectText}") { onPick(cheaper) }
            }
            OptionRow(R.drawable.ic_moon, Color(0xFFEEF0FF), "Подождать следующую неделю", "Придут новые карманные", onDismiss)
        }
    }
}

/** Вариант выхода из нехватки: наклейка, что сделать и что из этого будет, стрелка — туда. */
@Composable
private fun OptionRow(icon: Int, tint: Color, title: String, subtitle: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = tint.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardSticker(icon, tint, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                subtitle?.let { Text(it, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted) }
            }
            Image(
                painterResource(R.drawable.ic_arrow_right), null, Modifier.size(18.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(FinniColors.InkMuted),
            )
        }
    }
}

/** Что станет с копилкой, если взять недостающее: «В копилке 40 → 30, до «Кроватки» не хватит 60». */
private fun savingsConsequence(fb: PurchaseFeedback.NotEnough): String {
    val after = fb.savings - fb.missing
    val goal = fb.goal ?: return "В копилке ${fb.savings} → станет $after"
    return "В копилке ${fb.savings} → станет $after. До «${goal.name}» будет не хватать ${(goal.cost - after).coerceAtLeast(0)}"
}
