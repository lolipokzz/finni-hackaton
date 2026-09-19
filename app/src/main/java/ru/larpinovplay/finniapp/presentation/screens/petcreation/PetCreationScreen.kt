package ru.larpinovplay.finniapp.presentation.screens.petcreation

import androidx.compose.foundation.layout.Arrangement
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
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.navigation.MainNavigation
import ru.larpinovplay.finniapp.presentation.storage.text

/**
 * Экран создания питомца: вид, цвет, имя. Он же первый экран приложения: при запуске ищет сохранённую игру.
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
        when {
            state.species == null -> SpeciesStep(onAction)
            state.color == null -> ColorStep(onAction)
            else -> NameStep(state, onAction)
        }
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

@Composable
private fun SpeciesStep(onAction: (PetCreationAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выберите вид питомца")
        PetSpecies.entries.forEach { species ->
            Button(onClick = { onAction(PetCreationAction.SpeciesSelected(species)) }) {
                Text(species.name)
            }
        }
    }
}

@Composable
private fun ColorStep(onAction: (PetCreationAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выберите цвет питомца")
        PetColor.entries.forEach { color ->
            Button(onClick = { onAction(PetCreationAction.ColorSelected(color)) }) {
                Text(color.name)
            }
        }
    }
}

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
