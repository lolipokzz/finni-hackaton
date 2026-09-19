package ru.larpinovplay.finniapp.presentation.screens.petroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.PetHostState
import ru.larpinovplay.finniapp.presentation.navigation.MainNavigation

@Composable
fun PetRoomScreen(
    petHost: PetHostState,
    modifier: Modifier = Modifier,
    viewModel: PetRoomScreenViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    PetRoomScreenContent(
        state = state,
        onAction = viewModel::onAction,
        petHost = petHost,
        modifier = modifier
    )
}

@Composable
fun PetRoomScreenContent(
    state: PetRoomState,
    onAction: (PetRoomScreenAction) -> Unit,
    petHost: PetHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            PetRoomState.Loading -> CircularProgressIndicator()
            is PetRoomState.Creation -> PetCreationContent(state, onAction)
            is PetRoomState.Loaded -> MainNavigation(petHost)
        }
    }
}

@Composable
private fun PetCreationContent(
    state: PetRoomState.Creation,
    onAction: (PetRoomScreenAction) -> Unit,
) {
    when {
        state.species == null -> SpeciesStep(onAction)
        state.color == null -> ColorStep(onAction)
        else -> NameStep(state, onAction)
    }
}

@Composable
private fun SpeciesStep(onAction: (PetRoomScreenAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выберите вид питомца")
        PetSpecies.entries.forEach { species ->
            Button(onClick = { onAction(PetRoomScreenAction.SpeciesSelected(species)) }) {
                Text(species.name)
            }
        }
    }
}

@Composable
private fun ColorStep(onAction: (PetRoomScreenAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Выберите цвет питомца")
        PetColor.entries.forEach { color ->
            Button(onClick = { onAction(PetRoomScreenAction.ColorSelected(color)) }) {
                Text(color.name)
            }
        }
    }
}

@Composable
private fun NameStep(
    state: PetRoomState.Creation,
    onAction: (PetRoomScreenAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Дайте имя питомцу")
        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(PetRoomScreenAction.NameChanged(it)) },
            enabled = !state.isCreating
        )
        Button(
            onClick = { onAction(PetRoomScreenAction.CreatePetClicked) },
            enabled = state.name.isNotBlank() && !state.isCreating
        ) {
            Text("Создать")
        }
    }
}
