package ru.larpinovplay.finniapp.presentation.screens.shop

import ru.larpinovplay.finniapp.presentation.storage.orSnackbar
import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import ru.larpinovplay.finniapp.domain.game.model.tutorialStep
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
import ru.larpinovplay.finniapp.domain.game.model.weekSatiety
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.WithdrawResult
import ru.larpinovplay.finniapp.domain.game.model.budgetDirection
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.game.repository.requireSnapshot
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.util.result.dataOrNull

class ShopViewModel(
    private val game: GameRepository,
    private val content: Content,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ShopUiState(
            balance = game.requireSnapshot().state.balance,
            weekSatiety = game.requireSnapshot().state.weekSatiety,
            tab = ShopCategory.MANDATORY,
            items = itemsOf(ShopCategory.MANDATORY),
            owned = ownedIds(),
            budgets = budgetsOf(game.requireSnapshot().state),
            coach = game.requireSnapshot().state.tutorialStep == TutorialStep.SHOP,
        )
    )
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    init {
        // Деньги, статус еды и план приходят из игры; вкладка, диалоги и обратная связь принадлежат экрану
        viewModelScope.launch {
            game.snapshot.filterNotNull().collect { snapshot ->
                val g = snapshot.state
                _state.update { it.copy(balance = g.balance, weekSatiety = g.weekSatiety, owned = ownedIds(), budgets = budgetsOf(g), coach = g.tutorialStep == TutorialStep.SHOP) }
            }
        }
    }

    fun onAction(action: ShopAction) {
        when (action) {
            is ShopAction.TabSelected -> selectTab(action.category)
            is ShopAction.BuyClicked -> askToBuy(action.item)
            ShopAction.DismissPending -> dismissPending()
            ShopAction.DismissFeedback -> dismissFeedback()
            is ShopAction.PickCheaper -> pickCheaper(action.item)
            ShopAction.ConfirmPurchase -> confirmPurchase()
            ShopAction.BuyWithSavings -> buyWithSavings()
            ShopAction.SkipTutorialStep -> skipTutorialStep()
        }
    }

    private fun selectTab(category: ShopCategory) = _state.update { it.copy(tab = category, items = itemsOf(category)) }

    /** Окно подтверждения покупки. */
    private fun askToBuy(item: ShopItem) = _state.update { it.copy(pending = item) }

    private fun dismissPending() = _state.update { it.copy(pending = null) }

    private fun dismissFeedback() = _state.update { it.copy(feedback = null) }

    /** «Не хватает монет» → выбрал вещь подешевле: сразу её окно подтверждения. */
    private fun pickCheaper(item: ShopItem) = _state.update { it.copy(feedback = null, pending = item) }

    private fun skipTutorialStep() {
        viewModelScope.launch { game.skipTutorialStep(TutorialStep.SHOP).orSnackbar { skipTutorialStep() } }
    }

    /** Окно подтверждения сменяется итогом одним обновлением: между ними не мелькнёт магазин (и подсветка обучения). */
    private fun confirmPurchase() {
        _state.value.pending?.let(::buy)
    }

    private fun buy(item: ShopItem) {
        viewModelScope.launch {
            val result = game.buy(item).orSnackbar { buy(item) }
            val feedback = when (result) {
                null -> null
                is PurchaseResult.Success -> PurchaseFeedback.Bought(item, result.balanceAfter)
                is PurchaseResult.NotEnough -> {
                    val state = game.requireSnapshot().state
                    PurchaseFeedback.NotEnough(
                        item = item,
                        missing = result.missing,
                        cheaper = itemsOf(item.category).filter {
                            it.price <= state.balance && it.id != item.id && it.id !in ownedIds()
                        },
                        savings = state.savings,
                        goal = state.goal,
                        goalRemainingAfter = state.goalRemaining(state.savings - result.missing),
                    )
                }
                // Кнопка у купленной одежды выключена; сюда попадём только при двойном нажатии
                PurchaseResult.AlreadyOwned -> null
            }
            _state.update { it.copy(pending = null, feedback = feedback) }
        }
    }

    private fun itemsOf(category: ShopCategory): List<ShopItem> = content.shopItems.filter { it.category == category }

    private fun ownedIds(): Set<String> = game.requireSnapshot().state.wardrobe.mapTo(mutableSetOf()) { it.id }

    /** Берёт из копилки ровно недостающее (не весь остаток) и сразу покупает. */
    private fun buyWithSavings() {
        (_state.value.feedback as? PurchaseFeedback.NotEnough)?.let(::buyWithSavings)
    }

    private fun buyWithSavings(notEnough: PurchaseFeedback.NotEnough) {
        // Окно «Не хватает» сменится итогом покупки сразу, без мелькания магазина между ними
        viewModelScope.launch {
            val item = notEnough.item
            // Баланс мог измениться, пока было открыто окно: считаем недостающее заново
            val missing = (item.price - game.requireSnapshot().state.balance).coerceAtLeast(0)
            val withdrawn = missing == 0 || game.withdraw(missing).orSnackbar { buyWithSavings(notEnough) } == WithdrawResult.Success
            val bought = if (withdrawn) game.buy(item).orSnackbar { buyWithSavings(notEnough) } as? PurchaseResult.Success else null
            _state.update { it.copy(feedback = bought?.let { b -> PurchaseFeedback.Bought(item, b.balanceAfter, fromSavings = missing) }) }
        }
    }

    private fun budgetsOf(game: GameState): Map<ShopCategory, ShopUiState.CategoryBudget> {
        val plan = game.plan ?: return emptyMap()
        return ShopCategory.entries.associateWith {
            ShopUiState.CategoryBudget(planned = plan[it.budgetDirection], spent = game.spentThisWeek(it))
        }
    }
}
