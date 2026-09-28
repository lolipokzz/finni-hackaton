package ru.larpinovplay.finniapp.presentation.screens.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.presentation.pet.accessoryNodes
import ru.larpinovplay.finniapp.presentation.pet.accessoryNode

/** Гардероб: купленная одежда питомца, по одной вещи на место. Всё состояние — из игры. */
class WardrobeViewModel(private val game: GameRepository) : ViewModel() {

    val state: StateFlow<WardrobeUiState> = game.snapshot.filterNotNull()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(game.requireSnapshot()))

    /** Надетую вещь снимает, ненадетую надевает (прежняя вещь с того же места снимается сама). */
    fun onToggle(item: ShopItem) {
        val slot = item.slot ?: return
        val worn = game.requireSnapshot().pet.outfit[slot] == item.id
        viewModelScope.launch {
            if (worn) game.takeOff(slot) else game.wear(item)
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
