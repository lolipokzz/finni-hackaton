package ru.larpinovplay.finniapp.presentation.screens.petcreation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.presentation.components.BackButton
import ru.larpinovplay.finniapp.presentation.components.BubbleTail
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.DoneBadge
import ru.larpinovplay.finniapp.presentation.components.PetHostOwner
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.components.PetSpec
import ru.larpinovplay.finniapp.presentation.components.RoomBackground
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.components.rememberRoomAnchor
import ru.larpinovplay.finniapp.presentation.components.roomOrigin
import ru.larpinovplay.finniapp.presentation.components.roomPetSlot
import ru.larpinovplay.finniapp.presentation.navigation.MainNavigation
import ru.larpinovplay.finniapp.presentation.pet.PetHits
import ru.larpinovplay.finniapp.presentation.pet.PetPettingAnimation
import ru.larpinovplay.finniapp.presentation.pet.PetTapAnimation
import ru.larpinovplay.finniapp.presentation.pet.modelAsset
import ru.larpinovplay.finniapp.presentation.pet.modelScale
import ru.larpinovplay.finniapp.presentation.pet.skinAsset
import ru.larpinovplay.finniapp.presentation.pet.title
import ru.larpinovplay.finniapp.presentation.screens.home.short
import ru.larpinovplay.finniapp.presentation.screens.home.sticker
import ru.larpinovplay.finniapp.presentation.storage.text
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Первый экран приложения: при запуске ищет сохранённую игру. Игра есть — сразу открывает граф навигации
 * ([MainNavigation]); игры нет — знакомство с Финни; сохранение не прочиталось — предлагает повторить.
 *
 * Знакомство (ТЗ 2.5.1) идёт в комнате с настоящим 3D-питомцем: Финни здоровается и сам рассказывает о себе,
 * ребёнок выбирает раскраску (кот перекрашивается сразу) и имя, потом три коротких урока — карманные и план,
 * дела недели, мечта. Уроки можно пропустить. В конце питомец создаётся, и неделя начинается с окна плана —
 * первого настоящего решения.
 */
@Composable
fun PetCreationScreen(
    petHost: PetHostState,
    modifier: Modifier = Modifier,
    viewModel: PetCreationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PetCreationScreenContent(state = state, onAction = viewModel::onAction, petHost = petHost, modifier = modifier)
}

@Composable
fun PetCreationScreenContent(
    state: PetCreationUiState,
    onAction: (PetCreationAction) -> Unit,
    petHost: PetHostState?,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is PetCreationUiState.Loaded -> petHost?.let { MainNavigation(it, modifier) }
        is PetCreationUiState.Creation -> Introduction(state, onAction, petHost, modifier)
        PetCreationUiState.Loading -> Box(modifier.fillMaxSize()) {
            RoomBackground()
            CircularProgressIndicator(color = FinniColors.Teal, modifier = Modifier.align(Alignment.Center))
        }
        is PetCreationUiState.LoadFailed -> Box(modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            RoomBackground()
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth().creamCard(RoundedCornerShape(30.dp)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(state.error.text(), fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, textAlign = TextAlign.Center)
                TealButton("Повторить", null, { onAction(PetCreationAction.RetryLoadClicked) })
            }
        }
    }
}

// ---------- Знакомство ----------

/** Что Финни говорит на шаге. На уроках у фразы есть заголовок — чему этот урок. */
private fun CreationStep.speech(): Pair<String?, String> = when (this) {
    CreationStep.HELLO -> null to "Привет! Я Финни. Поможешь мне разобраться с монетами?"
    CreationStep.COLOR -> null to "Какого я цвета? Выбирай — я сразу перекрашусь!"
    CreationStep.NAME -> null to "А как меня будут звать? Можно оставить «Финни»."
    CreationStep.PLAN -> "Карманные монеты" to "Каждую неделю мне дают карманные. Ты раскладываешь их на три кучки — это план недели."
    CreationStep.GROW -> "Дела недели" to "Я расту, когда за неделю получаются четыре дела. Каждое — шаг роста!"
    CreationStep.DREAM -> "Мечта" to "А ещё мы вместе копим на мечту. Монеты из копилки превращаются в кроватку, велосипед или море!"
}

private fun CreationStep.button(): String = when (this) {
    CreationStep.HELLO -> "Привет, Финни!"
    CreationStep.COLOR -> "Вот такой!"
    CreationStep.NAME -> "Так и зовут"
    CreationStep.PLAN, CreationStep.GROW -> "Понятно"
    CreationStep.DREAM -> "Начнём!"
}

@Composable
private fun Introduction(
    state: PetCreationUiState.Creation,
    onAction: (PetCreationAction) -> Unit,
    petHost: PetHostState?,
    modifier: Modifier = Modifier,
) {
    val step = state.step
    BackHandler(enabled = step != CreationStep.HELLO) { onAction(PetCreationAction.PreviousStep) }

    // Настоящий Финни в комнате: малыш той раскраски, что выбрана сейчас
    val look = PetLook(state.color)
    val spec = PetSpec(
        assetName = PetGrowthStage.BABY.modelAsset,
        tintArgb = state.color.argb,
        modelScale = PetGrowthStage.BABY.modelScale,
        skin = look.skinAsset,
        animationsEnabled = true,
        soundEnabled = false,
        idleAnimation = "IdleHappy",
        tapAnimation = PetTapAnimation,
        hitAnimations = PetHits,
        pettingAnimation = PetPettingAnimation,
    )
    if (petHost != null) {
        SideEffect { petHost.spec = spec }
        DisposableEffect(petHost) {
            petHost.show(PetHostOwner.CREATION, true)
            onDispose { petHost.show(PetHostOwner.CREATION, false) }
        }
    }

    val room = rememberRoomAnchor()
    Box(modifier.fillMaxSize().roomOrigin(room)) {
        RoomBackground(anchor = room)
        Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(10.dp))
            TopBar(step, onBack = { onAction(PetCreationAction.PreviousStep) }, onSkip = { onAction(PetCreationAction.CreatePetClicked) })
            Spacer(Modifier.height(12.dp))
            Speech(step)
            // Финни стоит на полу: самый большой квадрат, что влезает между репликой и карточкой шага
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    Modifier
                        .size(minOf(maxWidth, maxHeight))
                        .onGloballyPositioned { petHost?.setSlot(PetHostOwner.CREATION, it) }
                        .roomPetSlot(room)
                )
            }
            StepCard(state, onAction)
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** Назад, лапки прогресса и «Пропустить» на уроках. */
@Composable
private fun TopBar(step: CreationStep, onBack: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(48.dp)) {
        if (step != CreationStep.HELLO) BackButton(onBack, Modifier.align(Alignment.CenterStart))
        Row(
            Modifier
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(FinniColors.Cream.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clearAndSetSemantics { contentDescription = "Шаг ${step.ordinal + 1} из ${CreationStep.entries.size}" },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            CreationStep.entries.forEach {
                Image(
                    painterResource(R.drawable.ic_paw), null, Modifier.size(if (it == step) 18.dp else 14.dp),
                    colorFilter = ColorFilter.tint(
                        when {
                            it == step -> FinniColors.Teal
                            it.ordinal < step.ordinal -> FinniColors.TealBright
                            else -> FinniColors.InkMuted.copy(alpha = 0.35f)
                        }
                    ),
                )
            }
        }
        if (step.isLesson) {
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = FinniColors.Cream.copy(alpha = 0.85f),
                    contentColor = FinniColors.Ink,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text("Пропустить", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}


/** Облачко Финни над ним: говорит от себя; на уроках — с бирюзовым заголовком. Хвостик смотрит на питомца. */
@Composable
private fun Speech(step: CreationStep) {
    AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "speech") { current ->
        val (title, text) = current.speech()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .creamCard(RoundedCornerShape(26.dp), elevation = 8.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                title?.let {
                    Text(it, fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal, modifier = Modifier.semantics { heading() })
                }
                Text(text, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink)
            }
            BubbleTail(Modifier.offset(y = (-4).dp), pointsLeft = false)
        }
    }
}

/** Карточка под Финни: что сделать на шаге и кнопка дальше. */
@Composable
private fun StepCard(state: PetCreationUiState.Creation, onAction: (PetCreationAction) -> Unit) {
    val step = state.step
    Column(
        Modifier
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(30.dp), elevation = 10.dp)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.notice?.let {
            Text(
                it.text(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.WarnInk,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FinniColors.WarnTint).padding(10.dp),
            )
        }
        when (step) {
            CreationStep.HELLO -> Unit
            CreationStep.COLOR -> ColorPicker(state.color) { onAction(PetCreationAction.ColorSelected(it)) }
            CreationStep.NAME -> NameField(state, onAction)
            CreationStep.PLAN -> PlanLesson()
            CreationStep.GROW -> GrowLesson()
            CreationStep.DREAM -> DreamLesson()
        }
        TealButton(
            step.button(),
            if (step == CreationStep.DREAM) R.drawable.ic_sun_small else null,
            { onAction(PetCreationAction.NextStep) },
            enabled = !state.isCreating && (step != CreationStep.NAME || state.name.isNotBlank()),
        )
    }
}

// ---------- Раскраска и имя ----------

/** Девять раскрасок кружками в три ряда; выбранная — в бирюзовой рамке с галочкой и подписью. */
@Composable
private fun ColorPicker(selected: PetColor, onSelect: (PetColor) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
        PetColor.entries.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { color ->
                    val isSelected = color == selected
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            // Нажимается весь кружок с рамкой: 52 dp, а не только цветная середина
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(color) })
                            .semantics { contentDescription = "Раскраска: ${color.title}" }
                            .border(if (isSelected) 4.dp else 3.dp, if (isSelected) FinniColors.Teal else Color.White, CircleShape)
                            .padding(if (isSelected) 5.dp else 3.dp)
                            .clip(CircleShape)
                            .background(Color(color.argb)),

                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) DoneBadge(size = 22.dp)
                    }
                }
            }
        }
        Text(selected.title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal)
    }
}

@Composable
private fun NameField(state: PetCreationUiState.Creation, onAction: (PetCreationAction) -> Unit) {
    OutlinedTextField(
        value = state.name,
        onValueChange = { if (it.length <= MAX_NAME) onAction(PetCreationAction.NameChanged(it)) },
        enabled = !state.isCreating,
        singleLine = true,
        label = { Text("Имя питомца") },
        textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAction(PetCreationAction.NextStep) }),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FinniColors.Teal,
            unfocusedBorderColor = FinniColors.Dashed,
            focusedLabelColor = FinniColors.Teal,
            cursorColor = FinniColors.Teal,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val MAX_NAME = 16

// ---------- Уроки ----------

/** Три кучки плана: наклейки направлений с подписями и полоска, как в окне плана недели. */
@Composable
private fun PlanLesson() {
    val parts = listOf(
        Triple(R.drawable.ic_meter_apple, Color(0xFFFFF0E6), "Обязательное" to "еда"),
        Triple(R.drawable.ic_meter_smile, Color(0xFFFFF5C9), "Необязательное" to "радости"),
        Triple(R.drawable.ic_deed_pig, FinniColors.DreamTint, "Копилка" to "на мечту"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            parts.forEach { (icon, tint, words) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    CardSticker(icon, tint, size = 56.dp, iconScale = 0.58f)
                    // «Необязательное» — самое длинное слово: чуть мельче, чтобы влезло целиком на узком экране
                    Text(words.first, fontSize = 12.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink, maxLines = 1, softWrap = false)
                    Text(words.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted)
                }
            }
        }
        // Полоска: как ляжет пример — 20 на еду, 15 на радости, 15 в копилку из 50
        Row(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).clearAndSetSemantics { }) {
            Box(Modifier.weight(20f).height(14.dp).background(FinniColors.SatietyRing))
            Box(Modifier.weight(15f).height(14.dp).background(Color(0xFFFFC83D)))
            Box(Modifier.weight(15f).height(14.dp).background(FinniColors.DreamRing))
        }
    }
}

/** Четыре дела недели наклейками и лапки: каждое дело — шаг роста. */
@Composable
private fun GrowLesson() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Deed.entries.forEach { deed ->
                val (icon, tint) = deed.sticker
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CardSticker(icon, tint, size = 54.dp, iconScale = 0.56f)
                    Text(deed.short, fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
                }
            }
        }
        Row(
            Modifier.clip(CircleShape).background(FinniColors.CardMint).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(4) { Image(painterResource(R.drawable.ic_paw), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(FinniColors.TealBright)) }
            Text("= 4 шага роста", fontSize = 14.sp, fontWeight = FontWeight.Black, color = FinniColors.Teal)
        }
    }
}

/** Мечты копилки: что ждёт впереди. */
@Composable
private fun DreamLesson() {
    val dreams = listOf(R.drawable.ic_bed to "Кроватка", R.drawable.ic_goal_bike to "Велосипед", R.drawable.ic_goal_sea to "Море")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        dreams.forEach { (icon, name) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CardSticker(icon, FinniColors.DreamTint, size = 64.dp, iconScale = 0.62f)
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Black, color = FinniColors.Ink)
            }
        }
    }
}
