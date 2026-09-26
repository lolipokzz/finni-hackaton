package ru.larpinovplay.finniapp.presentation.screens.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.larpinovplay.finniapp.domain.adventure.BasketCheck
import ru.larpinovplay.finniapp.domain.adventure.PaymentCheck
import ru.larpinovplay.finniapp.domain.adventure.changeOptions
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureUiState.SceneCheck
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Приключение недели: короткий сюжет по шагам. Каждый шаг — карточка с текстом и действием,
 * после ответа — разбор словами. В конце итог и награда; куда идти потом, решает навигация ([onBack]).
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
    Box(modifier = modifier.fillMaxSize()) {
        RoomBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Header(state.adventure.title, onBack)
            val finish = state.finish
            if (finish != null) {
                FinishCard(finish, onDone = onBack)
                return@Column
            }
            Text(
                "Шаг ${state.sceneIndex + 1} из ${state.adventure.scenes.size}",
                style = MaterialTheme.typography.labelLarge,
                color = FinniColors.NavyMuted,
                modifier = Modifier.padding(start = 6.dp),
            )
            if (state.sceneIndex == 0) {
                Card(color = FinniColors.CardPeach) {
                    Text(state.adventure.intro, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                }
            }
            when (val scene = state.scene) {
                is AdventureScene.Story -> StoryScene(scene.textAfter(state.chosenOptions))
                is AdventureScene.Pay -> PayScene(scene, state, onAction)
                is AdventureScene.Change -> ChangeScene(scene, state, onAction)
                is AdventureScene.Choice -> ChoiceScene(scene, state, onAction)
                is AdventureScene.Basket -> BasketScene(scene, state, onAction)
                null -> Unit
            }
            state.check?.let { CheckCard(it, onRetry = { onAction(AdventureAction.Retry) }) }
            if (state.canGoNext) {
                Button(
                    onClick = { onAction(AdventureAction.Next) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    val last = state.sceneIndex == state.adventure.scenes.lastIndex
                    Text(if (last) "Завершить приключение" else "Дальше", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------- Шаги ----------

@Composable
private fun StoryScene(text: String) {
    Card {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ChangeScene(scene: AdventureScene.Change, state: AdventureUiState, onAction: (AdventureAction) -> Unit) {
    val paid = state.paid ?: return
    val check = state.check as? SceneCheck.Change
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (check?.chosen == null && check != null) return@Column   // без сдачи: всё скажет разбор ниже
            Text(scene.text, style = MaterialTheme.typography.bodyLarge)
            Text("Ты дал $paid, покупка стоит ${scene.price}", style = MaterialTheme.typography.titleMedium)
            Hint(scene.hint)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                changeOptions(paid - scene.price).forEach { option ->
                    val chosen = check?.chosen == option
                    Surface(
                        onClick = { onAction(AdventureAction.ChooseChange(option)) },
                        enabled = check == null,
                        shape = RoundedCornerShape(16.dp),
                        color = if (chosen) FinniColors.Blue else FinniColors.BlueLight,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$option",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (chosen) Color.White else FinniColors.Navy,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Вопрос: варианты во всю ширину; после ответа выбранный подсвечен, остальные неактивны. */
@Composable
private fun ChoiceScene(scene: AdventureScene.Choice, state: AdventureUiState, onAction: (AdventureAction) -> Unit) {
    val chosen = (state.check as? SceneCheck.Choice)?.option
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(scene.text, style = MaterialTheme.typography.bodyLarge)
            Hint(scene.hint)
            scene.options.forEach { option ->
                val isChosen = option.id == chosen?.id
                Surface(
                    onClick = { onAction(AdventureAction.ChooseOption(option.id)) },
                    enabled = chosen == null,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isChosen) FinniColors.Blue else FinniColors.BlueLight,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            option.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isChosen) Color.White else FinniColors.Navy,
                        )
                    }
                }
            }
        }
    }
}

// ---------- Разбор ответа и итог ----------

/** Разбор: верно — ✓ и объяснение, неверно — ! и что поправить. Значок и слова, не только цвет. */
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
    Card(color = if (right) FinniColors.CardMint else FinniColors.CardPeach) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    if (right) "✓" else "!",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (right) FinniColors.Mood else FinniColors.Warning,
                )
                Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 10.dp))
            }
            // Оплату и корзину поправляют и пробуют снова; вопрос и сдачу разбираем и идём дальше
            if ((check is SceneCheck.Payment || check is SceneCheck.Basket) && !right) {
                TextButton(onClick = onRetry, modifier = Modifier.height(48.dp)) {
                    Text("Поправить", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FinishCard(finish: AdventureUiState.Finish, onDone: () -> Unit) {
    val feedback = LocalFeedback.current
    val result = finish.result
    Card(color = FinniColors.CardMint) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Приключение пройдено!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (result == null) {
                Text(feedback.text(FeedbackKey.ADVENTURE_REPLAY), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            } else {
                Text(
                    feedback.text(if (result.perfect) FeedbackKey.ADVENTURE_DONE_PERFECT else FeedbackKey.ADVENTURE_DONE_MISTAKES),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text("+${result.reward} монет", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onDone, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Готово", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------- Общие элементы ----------

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(onClick = onBack, shape = CircleShape, color = FinniColors.Lavender, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = FinniColors.Navy)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun Hint(text: String) {
    var shown by remember { mutableStateOf(false) }   // раскрыта ли подсказка — состояние элемента, не экрана
    Column {
        TextButton(onClick = { shown = !shown }, modifier = Modifier.height(48.dp)) {
            Text(if (shown) "Скрыть подсказку" else "Подсказка", style = MaterialTheme.typography.labelLarge)
        }
        if (shown) Text(text, style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted)
    }
}

@Composable
internal fun Card(color: Color = FinniColors.Card, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = FinniColors.Navy.copy(alpha = 0.15f), spotColor = FinniColors.Navy.copy(alpha = 0.15f)),
        shape = shape,
        color = color,
        content = content,
    )
}
