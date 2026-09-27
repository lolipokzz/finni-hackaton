package ru.larpinovplay.finniapp.presentation.screens.petcreation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.presentation.pet.title
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.navigation.MainNavigation
import ru.larpinovplay.finniapp.presentation.storage.text

/**
 * Экран создания питомца: раскраска кота и имя. Он же первый экран приложения: при запуске ищет сохранённую игру.
 * Игра есть — сразу открывает граф навигации ([MainNavigation]), игры нет — показывает создание, сохранение не
 * прочиталось — предлагает повторить (см. [PetCreationUiState]).
 */
@Composable
fun PetCreationScreen(
    petHost: PetHostState,
    modifier: Modifier = Modifier,
    viewModel: PetCreationViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    PetCreationScreenContent(
        state = state,
        onAction = viewModel::onAction,
        petHost = petHost,
        modifier = modifier
    )
}

@Composable
fun PetCreationScreenContent(
    state: PetCreationUiState,
    onAction: (PetCreationAction) -> Unit,
    petHost: PetHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            PetCreationUiState.Loading -> CircularProgressIndicator()
            is PetCreationUiState.Creation -> PetCreationContent(state, onAction)
            is PetCreationUiState.LoadFailed -> LoadFailedContent(state, onAction)
            is PetCreationUiState.Loaded -> MainNavigation(petHost)
        }
    }
}

@Composable
private fun PetCreationContent(
    state: PetCreationUiState.Creation,
    onAction: (PetCreationAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.notice?.let { Text(it.text(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        if (state.color == null) ColorStep(onAction) else NameStep(state, onAction)
    }
}

@Composable
private fun LoadFailedContent(
    state: PetCreationUiState.LoadFailed,
    onAction: (PetCreationAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(state.error.text(), textAlign = TextAlign.Center)
        Button(onClick = { onAction(PetCreationAction.RetryLoadClicked) }) {
            Text("Повторить")
        }
    }
}

/** Раскраски кота сеткой 3×3: кружок цвета шерсти и название. */
@Composable
private fun ColorStep(onAction: (PetCreationAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Выбери раскраску котика", style = MaterialTheme.typography.titleLarge)
        PetColor.entries.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { color ->
                    ColorOption(color, onClick = { onAction(PetCreationAction.ColorSelected(color)) })
                }
            }
        }
    }
}

@Composable
private fun ColorOption(color: PetColor, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.width(104.dp).semantics { contentDescription = "Раскраска: ${color.title}" },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(color.argb))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(color.title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

private const val COLUMNS = 3

@Composable
private fun NameStep(
    state: PetCreationUiState.Creation,
    onAction: (PetCreationAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Дайте имя питомцу")
        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(PetCreationAction.NameChanged(it)) },
            enabled = !state.isCreating
        )
        Button(
            onClick = { onAction(PetCreationAction.CreatePetClicked) },
            enabled = state.name.isNotBlank() && !state.isCreating
        ) {
            Text("Создать")
        }
    }
}
