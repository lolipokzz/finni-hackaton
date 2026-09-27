package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.presentation.components.CatEarFrame
import ru.larpinovplay.finniapp.presentation.components.CatEarFrameContentTop
import ru.larpinovplay.finniapp.presentation.components.BubbleTail
import ru.larpinovplay.finniapp.presentation.components.CardSticker
import ru.larpinovplay.finniapp.presentation.components.DashedDivider
import ru.larpinovplay.finniapp.presentation.components.Paws
import ru.larpinovplay.finniapp.presentation.components.PebbleButton
import ru.larpinovplay.finniapp.presentation.components.PillButton
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.growth
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.game.todoText
import ru.larpinovplay.finniapp.presentation.pet.nextStageTitle
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/*
 * Дела недели — карточка-мордочка с ушками (CatEarFrame) над солнышком. Четыре наклейки дел, что осталось сделать (каждое — с кнопкой туда,
 * где это делается), лапки роста и конец недели: кнопка, если можно, или спокойное «почему пока нельзя».
 */

internal val Deed.short: String
    get() = when (this) {
        Deed.FED -> "Еда"
        Deed.NOT_BORED -> "Радость"
        Deed.SAVINGS_ON_PLAN -> "Копилка"
        Deed.SPENDING_ON_PLAN -> "Траты"
    }

internal val Deed.sticker: Pair<Int, Color>
    get() = when (this) {
        Deed.FED -> R.drawable.ic_meter_apple to Color(0xFFFFF0E6)
        Deed.NOT_BORED -> R.drawable.ic_meter_smile to Color(0xFFFFF5C9)
        Deed.SAVINGS_ON_PLAN -> R.drawable.ic_deed_pig to Color(0xFFFFE6F0)
        Deed.SPENDING_ON_PLAN -> R.drawable.ic_deed_plan to Color(0xFFE6EEFF)
    }

/** Куда вести, чтобы сделать дело; null — делать ничего не нужно, только не нарушать план. */
private val Deed.go: Pair<String, HomeSection>?
    get() = when (this) {
        Deed.FED, Deed.NOT_BORED -> "В магазин" to HomeSection.SHOP
        Deed.SAVINGS_ON_PLAN -> "В копилку" to HomeSection.SAVINGS
        Deed.SPENDING_ON_PLAN -> null
    }

@Composable
fun DeedsCard(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val dismiss = { onAction(HomeAction.DismissDeeds) }
    val goTo = { section: HomeSection -> dismiss(); onAction(HomeAction.OpenSection(section)) }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            // Карточка стоит над солнышком и хвостиком показывает на него
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 112.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DeedsCardBody(state, goTo, dismiss, onFinish = { dismiss(); onAction(HomeAction.FinishWeek) })
                BubbleTail(Modifier.offset(y = (-4).dp), pointsLeft = false)
            }
        }
    }
}

@Composable
private fun DeedsCardBody(state: HomeUiState, goTo: (HomeSection) -> Unit, onClose: () -> Unit, onFinish: () -> Unit) {
    val deeds = state.deeds
    val all = deeds.steps == WeekDeeds.MAX_STEPS
    val feedback = LocalFeedback.current
    CatEarFrame(Modifier.fillMaxWidth()) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = CatEarFrameContentTop, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Заголовок — на мордочке, под полосками на лбу
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (all) "Все дела сделаны!" else "Дела недели",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Неделя ${state.week} · сделано ${deeds.steps} из ${WeekDeeds.MAX_STEPS}",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Teal,
                )
            }
            PebbleButton(R.drawable.ic_close, "Закрыть", onClose)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Deed.entries.forEach { DeedSticker(it, deeds[it]) }
        }

        val todo = Deed.entries.filter { !deeds[it] }
        if (todo.isNotEmpty() || state.finishBlock == FinishBlock.ADVENTURE_NOT_PLAYED) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Осталось сделать", fontSize = 15.sp, fontWeight = FontWeight.Black, color = FinniColors.InkMuted)
                todo.forEach { deed ->
                    TodoRow(deed.sticker.first, deed.todoText(state.weekSatiety), Color(0xFFFFF0E6), Color(0xFF7A2E10), deed.go, goTo)
                }
                if (state.finishBlock == FinishBlock.ADVENTURE_NOT_PLAYED) {
                    TodoRow(
                        R.drawable.ic_adventure, feedback.text(FeedbackKey.FINISH_NO_ADVENTURE),
                        Color(0xFFEEF0FF), Color(0xFF2A2F6B), "Играть" to HomeSection.TASKS, goTo,
                        buttonColor = Color(0xFFC9D1FF), buttonInk = Color(0xFF22285E),
                    )
                }
            }
        }

        state.pet.growth(deeds.steps)?.let { growth ->
            val stage = nextStageTitle(state.pet.growthStage)
            val text = when {
                growth.left == 0 -> "+${growth.fresh} за неделю — и Финни станет $stage!"
                growth.fresh > 0 -> "+${growth.fresh} за неделю. До роста осталось лапок: ${growth.left}"
                else -> "До роста осталось лапок: ${growth.left}"
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (all) FinniColors.CardMint else Color.Transparent)
                    .padding(if (all) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Paws(growth, size = 18.dp)
                Text(text, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.InkMuted)
            }
        }

        when (state.finishBlock) {
            null -> TealButton("Завершить неделю", R.drawable.ic_sun_small, onFinish)
            FinishBlock.SAME_DAY -> FinishNote(feedback.text(FeedbackKey.FINISH_SAME_DAY))
            FinishBlock.PLAN_NOT_CONFIRMED -> FinishNote(feedback.text(FeedbackKey.FINISH_NO_PLAN))
            FinishBlock.ADVENTURE_NOT_PLAYED -> Unit   // уже в «осталось сделать», с кнопкой
        }
    }
    }
}

/** Наклейка дела: сделанное — в белой обводке с галочкой, несделанное — пунктиром, как место для наклейки. */
@Composable
private fun DeedSticker(deed: Deed, done: Boolean) {
    val (icon, tint) = deed.sticker
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = "${deed.short}: " + if (done) "сделано" else "ещё не сделано" },
    ) {
        Box {
            if (done) {
                CardSticker(icon, tint)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(FinniColors.Teal)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_check), null, Modifier.size(13.dp)) }
            } else {
                Box(Modifier.size(64.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(64.dp)) {
                        val w = 3.dp.toPx()
                        drawCircle(
                            FinniColors.DeedPending,
                            radius = size.minDimension / 2 - w / 2,
                            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))),
                        )
                    }
                    Image(painterResource(icon), null, Modifier.size(34.dp))
                }
            }
        }
        Text(deed.short, fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (done) FinniColors.Ink else Color(0xFFB4471B))
    }
}

@Composable
private fun TodoRow(
    icon: Int,
    text: String,
    background: Color,
    ink: Color,
    go: Pair<String, HomeSection>?,
    goTo: (HomeSection) -> Unit,
    buttonColor: Color = FinniColors.ActionPeach,
    buttonInk: Color = FinniColors.ActionPeachInk,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(painterResource(icon), null, Modifier.size(28.dp))
        Text(text, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, color = ink, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
        go?.let { (label, section) -> PillButton(label, onClick = { goTo(section) }, color = buttonColor, ink = buttonInk) }
    }
}

/** Почему неделю пока нельзя закончить — спокойно, без кнопки: делать тут нечего, только подождать. */
@Composable
private fun FinishNote(text: String) {
    Column {
        DashedDivider()
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_moon), null, Modifier.size(22.dp))
            Text(text, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.InkMuted, modifier = Modifier.weight(1f))
        }
    }
}
