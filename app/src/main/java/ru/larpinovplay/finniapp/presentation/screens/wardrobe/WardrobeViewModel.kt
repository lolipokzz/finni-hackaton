package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import ru.larpinovplay.finniapp.presentation.storage.orSnackbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.presentation.pet.accessoryNodes
import ru.larpinovplay.finniapp.presentation.pet.accessoryNode

/** Гардероб: купленная одежда питомца, по одной вещи на место. Всё состояние — из игры. */
class WardrobeViewModel(private val game: GameRepository) : ViewModel() {

    private val _state = MutableStateFlow(toUiState(game.requireSnapshot()))
    val state: StateFlow<WardrobeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { _state.value = toUiState(it) }
        }
    }

    fun onAction(action: WardrobeAction) {
        when (action) {
            is WardrobeAction.Toggle -> toggle(action.item)
        }
    }

    /** Надетую вещь снимает, ненадетую надевает (прежняя вещь с того же места снимается сама). */
    private fun toggle(item: ShopItem) {
        val slot = item.slot ?: return
        val worn = game.requireSnapshot().pet.outfit[slot] == item.id
        viewModelScope.launch {
            (if (worn) game.takeOff(slot) else game.wear(item)).orSnackbar { toggle(item) }
        }
    }

    private fun toUiState(snapshot: GameSnapshot): WardrobeUiState {
        val pet = snapshot.pet
        val items = snapshot.state.wardrobe
            .filter { accessoryNode(it.id) != null }
            .sortedBy { it.slot }
            .map { WardrobeItem(it, worn = it.slot?.let(pet.outfit::get) == it.id) }
        return WardrobeUiState(
            items = items,
            accessories = pet.accessoryNodes,
        )
    }
}
