package ru.larpinovplay.finniapp.presentation.screens.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.presentation.components.StatRow
import ru.larpinovplay.finniapp.presentation.components.TealButton
import ru.larpinovplay.finniapp.presentation.components.creamCard
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Содержимое диалога итога. Само окно (Dialog) создаёт навигация: маршрут TaskResult
 * помечен как диалог, поэтому здесь только карточка, без AlertDialog.
 */
@Composable
fun TaskResultCard(result: TaskOutcomeUi, onDismiss: () -> Unit, modifier: Modifier = Modifier, retryNow: Boolean = false) {
    Column(
        modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .creamCard(RoundedCornerShape(34.dp), elevation = 20.dp)
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(if (result.success) FinniColors.Teal else FinniColors.WarnTint)
                .border(4.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (result.success) Image(painterResource(R.drawable.ic_check), null, Modifier.size(34.dp))
            else Text("!", fontSize = 34.sp, fontWeight = FontWeight.Black, color = FinniColors.WarnInk)
        }
        Text(
            if (result.success) "Верно!" else "Почти получилось",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FinniColors.Ink, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        result.consequence?.let {
            Text(it, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, color = FinniColors.Ink, textAlign = TextAlign.Center)
        }
        Text(result.explanation, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center)
        StatRow("Монеты", "+${result.reward}", coin = true)
        if (!result.success) {
            Text(
                // В демо после ошибки задание можно решить сразу ещё раз (ТЗ 2.5.8), в игре — на следующей неделе
                if (retryNow) "Можно сразу попробовать ещё раз" else "Это задание можно попробовать снова на следующей неделе",
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = FinniColors.InkMuted, textAlign = TextAlign.Center,
            )
        }
        TealButton("Понятно", R.drawable.ic_check, onDismiss)
    }
}
