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
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.game.model.budgetDirection
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull

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
            budgets = budgetsOf(game.requireSnapshot().state),
        )
    )
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    init {
        // Деньги, статус еды и план приходят из игры; вкладка, диалоги и обратная связь принадлежат экрану
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { snapshot ->
                val g = snapshot.state
                _state.update { it.copy(balance = g.balance, foodCovered = g.foodCovered, budgets = budgetsOf(g)) }
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
            ShopAction.BuyWithSavings -> buyWithSavings()
        }
    }

    private fun confirmPurchase() {
        val item = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }   // диалог закрываем сразу: повторный тап не купит дважды
        viewModelScope.launch {
            // TODO(хранилище): ошибку сохранения показать пользователю при подключении DataStore
            val result = game.buy(item).dataOrNull() ?: return@launch
            val feedback = when (result) {
                is PurchaseResult.Success -> PurchaseFeedback.Bought(item, result.balanceAfter)
                is PurchaseResult.NotEnough -> {
                    val state = game.requireSnapshot().state
                    PurchaseFeedback.NotEnough(
                        item = item,
                        missing = result.missing,
                        cheaper = content.shopItems.filter {
                            it.category == item.category && it.price <= state.balance && it.id != item.id
                        },
                        savings = state.savings,
                        goal = state.goal,
                    )
                }
            }
            _state.update { it.copy(feedback = feedback) }
        }
    }

    /** Берёт из копилки ровно недостающее (не весь остаток) и сразу покупает. */
    private fun buyWithSavings() {
        val notEnough = _state.value.feedback as? PurchaseFeedback.NotEnough ?: return
        _state.update { it.copy(feedback = null) }   // окно закрываем сразу: повторный тап не снимет дважды
        viewModelScope.launch {
            // TODO(хранилище): ошибку сохранения показать пользователю при подключении DataStore
            val item = notEnough.item
            // Баланс мог измениться, пока было открыто окно: считаем недостающее заново
            val missing = (item.price - game.requireSnapshot().state.balance).coerceAtLeast(0)
            if (missing > 0 && game.withdraw(missing).dataOrNull() != WithdrawResult.Success) return@launch
            val bought = game.buy(item).dataOrNull() as? PurchaseResult.Success ?: return@launch
            _state.update { it.copy(feedback = PurchaseFeedback.Bought(item, bought.balanceAfter, fromSavings = missing)) }
        }
    }

    private fun budgetsOf(game: GameState): Map<ShopCategory, ShopUiState.CategoryBudget> {
        val plan = game.plan ?: return emptyMap()
        return ShopCategory.entries.associateWith {
            ShopUiState.CategoryBudget(planned = plan[it.budgetDirection], spent = game.spentThisWeek(it))
        }
    }

    private fun itemsOf(category: ShopCategory) = content.shopItems.filter { it.category == category }
}
