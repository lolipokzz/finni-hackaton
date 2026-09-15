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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.screens.home.HomeAction
import ru.larpinovplay.finniapp.presentation.screens.home.HomeScreen
import ru.larpinovplay.finniapp.presentation.screens.home.HomeSection
import ru.larpinovplay.finniapp.presentation.screens.home.HomeUiState
import ru.larpinovplay.finniapp.presentation.screens.home.SectionStubScreen

@Composable
fun PetRoomScreen(
    modifier: Modifier = Modifier,
    viewModel: PetRoomScreenViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    PetRoomScreenContent(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun PetRoomScreenContent(
    state: PetRoomState,
    onAction: (PetRoomScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            PetRoomState.Loading -> CircularProgressIndicator()
            is PetRoomState.Creation -> PetCreationContent(state, onAction)
            is PetRoomState.Loaded -> HomeRoute(state.pet)
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

/**
 * Главный экран с локальной навигацией по разделам-заглушкам.
 * Когда появится NavHost (docs/07-screens.md#граф-навигации), разделы станут маршрутами.
 */
@Composable
private fun HomeRoute(pet: Pet) {
    var section by remember { mutableStateOf<HomeSection?>(null) }
    val current = section
    if (current != null) {
        SectionStubScreen(section = current, onBack = { section = null })
        return
    }
    HomeScreen(
        state = HomeUiState.sample(pet),
        onAction = { action ->
            when (action) {
                is HomeAction.OpenSection -> section = action.section
                HomeAction.FinishWeek -> Unit      // TODO: команда ClosePeriod, экран итогов (П8)
                HomeAction.PetTapped -> Unit       // TODO: реакция питомца
            }
        }
    )
}
