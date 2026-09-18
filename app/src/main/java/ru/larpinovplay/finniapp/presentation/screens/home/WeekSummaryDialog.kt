package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.presentation.game.foodText
import ru.larpinovplay.finniapp.presentation.game.grewText
import ru.larpinovplay.finniapp.presentation.game.savedText
import ru.larpinovplay.finniapp.presentation.theme.FinniColors

/** Итоги недели после «Завершить неделю» (ТЗ 2.5.9, 2.5.10): что изменилось и почему. */
@Composable
fun WeekSummaryDialog(summary: WeekSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Неделя ${summary.week} закончена", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                val lines = buildList {
                    add(summary.foodCovered to summary.foodText())
                    add(summary.savedSomething to summary.savedText())
                    if (summary.grew) add(true to summary.grewText())
                }
                lines.forEach { (ok, line) ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(if (ok) "✓" else "○", style = MaterialTheme.typography.titleMedium, color = if (ok) FinniColors.Mood else FinniColors.NavyMuted)
                        Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Настроение", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.weight(1f))
                    Text(if (summary.moodDelta >= 0) "+${summary.moodDelta}" else "${summary.moodDelta}", fontWeight = FontWeight.Bold)
                }
                Row {
                    Text("Новая неделя", style = MaterialTheme.typography.bodyMedium, color = FinniColors.NavyMuted, modifier = Modifier.weight(1f))
                    Text("+60 карманных монет", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Новая неделя", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
