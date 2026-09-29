package ru.larpinovplay.finniapp.presentation.screens.adventure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.adventure.BasketCheck
import ru.larpinovplay.finniapp.domain.adventure.PaymentCheck
import ru.larpinovplay.finniapp.domain.adventure.changeOptions
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.presentation.adventure.AdventureLook
import ru.larpinovplay.finniapp.presentation.adventure.AdventureSky
import ru.larpinovplay.finniapp.presentation.adventure.look
import ru.larpinovplay.finniapp.presentation.components.BackButton
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.PillButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureUiState.SceneCheck
import ru.larpinovplay.finniapp.presentation.theme.FinniColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Приключение недели: короткий сюжет по шагам. Здесь не комната, а «улица»: своё небо приключения с лучами, сверху
 * тропинка шагов, каждый шаг — кремовая карточка со значком, после ответа — разбор словами.
 */
@Composable
fun AdventureScreen(
    viewModel: AdventureViewModel,   // без значения по умолчанию: ему нужен id приключения из ключа маршрута
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
    } else {
        AdventureScreenContent(state = current, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
    }
}

@Composable
fun AdventureScreenContent(
    state: AdventureUiState,
    onAction: (AdventureAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = state.adventure.look
    Box(modifier = modifier.fillMaxSize()) {
        AdventureSky(look, Modifier.fillMaxSize(), sunY = 0.08f)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton(onBack)
                Text(
                    state.adventure.title,
                    fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            Spacer(Modifier.height(12.dp))
            Trail(state, look)
            Spacer(Modifier.height(12.dp))
            val scroll = rememberScrollState()
            // Новый шаг открывается сверху; после ответа экран сам доезжает до разбора и «Дальше»
            LaunchedEffect(state.sceneIndex, state.finish != null) { scroll.scrollTo(0) }
            LaunchedEffect(state.check) { if (state.check != null) scroll.animateScrollTo(scroll.maxValue) }
            Column(
                Modifier.weight(1f).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val finish = state.finish
                if (finish != null) {
                    FinishCard(finish, look, reward = state.adventure.reward, onDone = onBack)
                } else {
                    if (state.sceneIndex == 0) Intro(state.adventure.intro, look)
                    when (val scene = state.scene) {
                        is AdventureScene.Story -> SceneCard(R.drawable.ic_scene_story, if (state.sceneIndex == 0) "История" else "Что было дальше", look) {
                            BodyText(scene.textAfter(state.chosenOptions))
                        }
                        is AdventureScene.Pay -> PayScene(scene, state, look, onAction)
                        is AdventureScene.Change -> ChangeScene(scene, state, look, onAction)
                        is AdventureScene.Choice -> ChoiceScene(scene, state, look, onAction)
                        is AdventureScene.Basket -> BasketScene(scene, state, look, onAction)
                        null -> Unit
                    }
                    state.check?.let { CheckCard(it, onRetry = { onAction(AdventureAction.Retry) }) }
                    if (state.canGoNext) {
                        val last = state.sceneIndex == state.adventure.scenes.lastIndex
                        TealButton(
                            if (last) "Завершить приключение" else "Дальше",
                            if (last) R.drawable.ic_sun_small else null,
                            { onAction(AdventureAction.Next) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ---------- Тропинка шагов ----------

/** Тропинка: пройденные шаги — бирюзовые с галочкой, текущий — крупный со значком сцены, впереди — номера. */
@Composable
private fun Trail(state: AdventureUiState, look: AdventureLook) {
    val scenes = state.adventure.scenes
    val finished = state.finish != null
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clearAndSetSemantics {
                contentDescription = if (finished) "Все шаги пройдены" else "Шаг ${state.sceneIndex + 1} из ${scenes.size}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(4.dp)) {
            drawLine(
                Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            scenes.forEachIndexed { i, scene ->
                when {
                    finished || i < state.sceneIndex -> Box(
                        Modifier.size(32.dp).clip(CircleShape).background(FinniColors.Teal).border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp)) }
                    i == state.sceneIndex -> Box(
                        Modifier.size(46.dp).creamCard(CircleShape, elevation = 6.dp, border = 3.dp).background(look.tint),
                        contentAlignment = Alignment.Center,
                    ) { Image(painterResource(scene.icon), null, Modifier.size(26.dp)) }
                    else -> Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted) }
                }
            }
        }
    }
}

private val AdventureScene.icon: Int
    get() = when (this) {
        is AdventureScene.Story -> R.drawable.ic_scene_story
        is AdventureScene.Pay, is AdventureScene.Change -> R.drawable.ic_coin
        is AdventureScene.Choice -> R.drawable.ic_scene_question
        is AdventureScene.Basket -> R.drawable.ic_nav_shop
    }

// ---------- Общие части сцен ----------

/** Вступление на первом шаге: крупная эмблема приключения и пара слов, что будет. */
@Composable
private fun Intro(text: String, look: AdventureLook) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardSticker(look.emblem, look.tint, size = 116.dp, iconScale = 0.66f)
        Text(
            text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink, textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.7f)).padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Карточка шага: значок сцены и что это за шаг, ниже — содержимое. */
@Composable
internal fun SceneCard(icon: Int, label: String, look: AdventureLook, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(30.dp), elevation = 10.dp)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardSticker(icon, look.tint, size = 40.dp, iconScale = 0.6f)
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, modifier = Modifier.semantics { heading() })
        }
        content()
    }
}

@Composable
internal fun BodyText(text: String) {
    Text(text, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink)
}

/** Подсказка: спокойная пилюля с лампочкой; по нажатию раскрывается жёлтое облачко. */
@Composable
internal fun Hint(text: String) {
    var shown by remember { mutableStateOf(false) }   // раскрыта ли подсказка — состояние элемента, не экрана
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { shown = !shown },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF5C9), contentColor = FinniColors.CoinInk),
            contentPadding = PaddingValues(start = 10.dp, end = 16.dp),
            modifier = Modifier.height(48.dp),
        ) {
            Image(painterResource(R.drawable.ic_bulb), null, Modifier.padding(end = 6.dp).size(24.dp))
            Text(if (shown) "Скрыть подсказку" else "Подсказка", fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        AnimatedVisibility(shown) {
            Text(
                text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.CoinInk,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFFFF5C9)).padding(12.dp),
            )
        }
    }
}

// ---------- Шаги ----------

/** Сдача: варианты — крупные золотые монеты с числом. */
@Composable
private fun ChangeScene(scene: AdventureScene.Change, state: AdventureUiState, look: AdventureLook, onAction: (AdventureAction) -> Unit) {
    val paid = state.paid ?: return
    val check = state.check as? SceneCheck.Change
    if (check != null && check.chosen == null) return   // без сдачи: всё скажет разбор ниже
    SceneCard(R.drawable.ic_coin, "Сдача", look) {
        BodyText(scene.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FactChip("Ты дал", paid)
            FactChip("Стоит", scene.price)
        }
        Hint(scene.hint)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
            changeOptions(paid - scene.price).forEach { option ->
                val chosen = check?.chosen == option
                Surface(
                    selected = chosen,
                    onClick = { onAction(AdventureAction.ChooseChange(option)) },
                    enabled = check == null,
                    shape = CircleShape,
                    color = Color(0xFFFFC23D),
                    shadowElevation = if (chosen) 0.dp else 4.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .alpha(if (check != null && !chosen) 0.45f else 1f)
                        .border(if (chosen) 4.dp else 0.dp, if (chosen) FinniColors.Teal else Color.Transparent, CircleShape)
                        .semantics { contentDescription = "Сдача $option"; role = Role.RadioButton },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFFFDD7A)))
                        Text("$option", fontSize = 24.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun FactChip(label: String, value: Int) {
    Row(
        Modifier.clip(CircleShape).background(FinniColors.Pebble).padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(20.dp))
        Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk, modifier = Modifier.padding(end = 6.dp))
    }
}

/** Вопрос: варианты с буквами А, Б, В; после ответа выбранный подсвечен и помечен знаком, остальные бледнеют. */
@Composable
private fun ChoiceScene(scene: AdventureScene.Choice, state: AdventureUiState, look: AdventureLook, onAction: (AdventureAction) -> Unit) {
    val chosen = (state.check as? SceneCheck.Choice)?.option
    SceneCard(R.drawable.ic_scene_question, "Вопрос", look) {
        BodyText(scene.text)
        Hint(scene.hint)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) { scene.options.forEachIndexed { i, option ->
            val isChosen = option.id == chosen?.id
            val (bg, badge, badgeInk) = when {
                isChosen && option.correct -> Triple(FinniColors.CardMint, FinniColors.Teal, Color.White)
                isChosen -> Triple(FinniColors.WarnTint, FinniColors.DeedPending, Color.White)
                else -> Triple(Color.White, look.tint, FinniColors.Ink)
            }
            Surface(
                selected = isChosen,
                onClick = { onAction(AdventureAction.ChooseOption(option.id)) },
                enabled = chosen == null,
                shape = RoundedCornerShape(20.dp),
                color = bg,
                border = BorderStroke(2.dp, if (isChosen) badge else FinniColors.Dashed),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .alpha(if (chosen != null && !isChosen) 0.5f else 1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
                        when {
                            isChosen && option.correct -> Image(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp))
                            isChosen -> Text("!", fontSize = 18.sp, fontWeight = FontWeight.Black, color = badgeInk)
                            else -> Text(LETTERS.getOrElse(i) { "${i + 1}" }, fontSize = 17.sp, fontWeight = FontWeight.Black, color = badgeInk)
                        }
                    }
                    Text(option.text, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink, modifier = Modifier.weight(1f))
                }
            }
        } }
    }
}

private val LETTERS = listOf("А", "Б", "В", "Г", "Д")

// ---------- Разбор ответа и итог ----------

/** Разбор: верно — бирюзовая галочка и объяснение, неверно — тёплый «!» и что поправить. Значок и слова, не только цвет. */
@Composable
private fun CheckCard(check: SceneCheck, onRetry: () -> Unit) {
    val feedback = LocalFeedback.current
    val (right, text) = when (check) {
        is SceneCheck.Payment -> when (val r = check.result) {
            PaymentCheck.Exact -> true to feedback.text(FeedbackKey.ADVENTURE_PAY_EXACT)
            is PaymentCheck.WithChange -> true to feedback.text(FeedbackKey.ADVENTURE_PAY_CHANGE, "paid" to r.paid)
            is PaymentCheck.NotEnough -> false to feedback.text(FeedbackKey.ADVENTURE_PAY_NOT_ENOUGH, "n" to r.missing)
            is PaymentCheck.ExtraPiece -> false to feedback.text(FeedbackKey.ADVENTURE_PAY_EXTRA, "coin" to r.piece)
        }
        is SceneCheck.Change -> check.right to when {
            check.chosen == null -> feedback.text(FeedbackKey.ADVENTURE_CHANGE_NONE, "price" to check.price)
            check.right -> feedback.text(FeedbackKey.ADVENTURE_CHANGE_OK, "paid" to check.paid, "price" to check.price, "change" to check.correct)
            else -> feedback.text(FeedbackKey.ADVENTURE_CHANGE_WRONG, "paid" to check.paid, "price" to check.price, "change" to check.correct)
        }
        is SceneCheck.Choice -> check.option.correct to check.option.explanation
        is SceneCheck.Basket -> when (val r = check.result) {
            is BasketCheck.Fits -> true to feedback.text(FeedbackKey.ADVENTURE_BASKET_FITS, "total" to r.total, "left" to r.left)
            is BasketCheck.OverBudget -> false to feedback.text(FeedbackKey.ADVENTURE_BASKET_OVER, "n" to r.over)
            is BasketCheck.MissingRequired -> false to feedback.text(FeedbackKey.ADVENTURE_BASKET_MISSING, "item" to r.names.joinToString())
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(26.dp), elevation = 8.dp)
            .background(if (right) FinniColors.CardMint else FinniColors.WarnTint)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (right) FinniColors.Teal else FinniColors.DeedPending).border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (right) Image(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                else Text("!", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Text(
                text, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = if (right) Color(0xFF0B5E4F) else FinniColors.WarnInk,
                modifier = Modifier.weight(1f).padding(top = 8.dp),
            )
        }
        // Оплату и корзину поправляют и пробуют снова; вопрос и сдачу разбираем и идём дальше
        if ((check is SceneCheck.Payment || check is SceneCheck.Basket) && !right) {
            PillButton("Поправить", onRetry, modifier = Modifier.align(Alignment.End))
        }
    }
}

/** Финал: эмблема на небе приключения с галочкой, похвала и награда. */
@Composable
private fun FinishCard(finish: AdventureUiState.Finish, look: AdventureLook, reward: Int, onDone: () -> Unit) {
    val feedback = LocalFeedback.current
    val result = finish.result
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(34.dp), elevation = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            AdventureSky(look, Modifier.matchParentSize(), sunY = 0.5f)
            Box {
                CardSticker(look.emblem, look.tint, size = 124.dp, iconScale = 0.66f)
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp).size(42.dp).clip(CircleShape)
                        .background(FinniColors.Teal).border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(20.dp)) }
            }
        }
        Column(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Приключение пройдено!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() },
            )
            if (result == null) {
                Text(feedback.text(FeedbackKey.ADVENTURE_REPLAY), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center)
            } else {
                Text(
                    feedback.text(if (result.perfect) FeedbackKey.ADVENTURE_DONE_PERFECT else FeedbackKey.ADVENTURE_DONE_MISTAKES),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(CircleShape).background(FinniColors.CoinPill).padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Image(painterResource(R.drawable.ic_coin), null, Modifier.size(30.dp))
                        Text("+${result.reward}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = FinniColors.CoinInk)
                    }
                    if (result.perfect) {
                        Text(
                            "без ошибок", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal,
                            modifier = Modifier.clip(CircleShape).background(FinniColors.CardMint).padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    } else if (reward > result.reward) {
                        Text(
                            "без ошибок было бы +$reward", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            TealButton("Готово", R.drawable.ic_sun_small, onDone)
        }
    }
}

/** Отметка «выбрано»: бирюзовый кружок с галочкой или пустой кружок с пунктиром. */
@Composable
internal fun PickMark(picked: Boolean, size: Dp = 30.dp) {
    if (picked) {
        Box(Modifier.size(size).clip(CircleShape).background(FinniColors.Teal), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_check), null, Modifier.size(size * 0.45f))
        }
    } else {
        Canvas(Modifier.size(size)) {
            val w = 2.5.dp.toPx()
            drawCircle(
                FinniColors.InkMuted.copy(alpha = 0.5f), radius = this.size.minDimension / 2 - w / 2,
                style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
            )
        }
    }
}
