package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.domain.game.model.Deed
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.presentation.game.icon
import ru.larpinovplay.finniapp.presentation.game.title
import ru.larpinovplay.finniapp.presentation.game.todoText
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/**
 * Четыре значка дел недели под номером недели. Сделанное — яркое и с галочкой, несделанное — бледное.
 * Это не цифры плана, а короткий список того, к чему стремиться: каждое дело — шаг роста Финни.
 */
@Composable
fun DeedsStrip(deeds: WeekDeeds, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = FinniColors.Card,
        modifier = modifier.semantics { contentDescription = "Дела недели: сделано ${deeds.steps} из ${WeekDeeds.MAX_STEPS}" },
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Deed.entries.forEach { deed ->
                val done = deeds[deed]
                Box(Modifier.size(32.dp)) {
                    Image(painterResource(deed.icon), null, Modifier.size(28.dp).alpha(if (done) 1f else 0.35f))
                    if (done) {
                        Surface(shape = CircleShape, color = FinniColors.Mood, modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✓", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Дела недели подробно: что уже сделано и что сделать, чтобы Финни подрос. */
@Composable
fun DeedsDialog(deeds: WeekDeeds, weekSatiety: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Дела недели: ${deeds.steps} из ${WeekDeeds.MAX_STEPS}", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Каждое дело — шаг роста Финни. Шаги не пропадают, а когда их хватит, Финни подрастёт и карманных станет больше.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinniColors.NavyMuted,
                )
                Deed.entries.forEach { deed ->
                    val done = deeds[deed]
                    Row(verticalAlignment = Alignment.Top) {
                        Image(painterResource(deed.icon), null, Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                (if (done) "✓ " else "○ ") + deed.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (done) FinniColors.Mood else FinniColors.Navy,
                            )
                            Text(
                                if (done) "Сделано" else deed.todoText(weekSatiety),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Понятно", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
