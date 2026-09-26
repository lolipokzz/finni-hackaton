package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback

/** Почему неделю пока нельзя закончить: объяснение вместо молча неработающей кнопки. */
@Composable
fun FinishNoticeDialog(reason: FinishBlock, onDismiss: () -> Unit) {
    val key = when (reason) {
        FinishBlock.PLAN_NOT_CONFIRMED -> FeedbackKey.FINISH_NO_PLAN
        FinishBlock.SAME_DAY -> FeedbackKey.FINISH_SAME_DAY
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        text = { Text(LocalFeedback.current.text(key), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(48.dp)) {
                Text("Хорошо", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
