package ru.larpinovplay.finniapp.presentation.screens.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

class ShopViewModel(
    private val game: GameRepository,
    private val content: Content,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ShopUiState(
            balance = game.requireSnapshot().state.balance,
            foodCovered = game.requireSnapshot().state.foodCovered,
            tab = ShopCategory.MANDATORY,
            items = itemsOf(ShopCategory.MANDATORY),
        )
    )
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    init {
        // Деньги и статус еды приходят из игры; вкладка, диалоги и обратная связь принадлежат экрану
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { snapshot ->
                val g = snapshot.state
                _state.update { it.copy(balance = g.balance, foodCovered = g.foodCovered) }
            }
        }
    }

    fun onAction(action: ShopAction) {
        when (action) {
            is ShopAction.TabSelected -> _state.update { it.copy(tab = action.category, items = itemsOf(action.category)) }
            is ShopAction.BuyClicked -> _state.update { it.copy(pending = action.item) }
            ShopAction.DismissPending -> _state.update { it.copy(pending = null) }
            ShopAction.DismissFeedback -> _state.update { it.copy(feedback = null) }
            is ShopAction.PickCheaper -> _state.update { it.copy(feedback = null, pending = action.item) }
            ShopAction.ConfirmPurchase -> confirmPurchase()
        }
    }

    private fun confirmPurchase() {
        val item = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }   // диалог закрываем сразу: повторный тап не купит дважды
        viewModelScope.launch {
            val feedback = when (val result = game.buy(item)) {
                is PurchaseResult.Success -> PurchaseFeedback.Bought(item, result.balanceAfter)
                is PurchaseResult.NotEnough -> PurchaseFeedback.NotEnough(
                    item = item,
                    missing = result.missing,
                    cheaper = content.shopItems.filter {
                        it.category == item.category && it.price <= game.requireSnapshot().state.balance && it.id != item.id
                    },
                )
            }
            _state.update { it.copy(feedback = feedback) }
        }
    }

    private fun itemsOf(category: ShopCategory) = content.shopItems.filter { it.category == category }
}
