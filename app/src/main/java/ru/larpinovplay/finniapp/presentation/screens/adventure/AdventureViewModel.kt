package ru.larpinovplay.finniapp.presentation.screens.adventure

import ru.larpinovplay.finniapp.presentation.storage.snackbar
import ru.larpinovplay.finniapp.domain.util.result.Result
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.larpinovplay.finniapp.domain.adventure.checkBasket
import ru.larpinovplay.finniapp.domain.adventure.checkPayment
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.presentation.screens.adventure.AdventureUiState.SceneCheck

/**
 * Проходит приключение [adventureId] шаг за шагом. Ошибка не останавливает сюжет: неверную оплату или корзину
 * можно поправить, неверный ответ на вопрос или про сдачу разбираем и идём дальше. Число ошибок влияет только на награду в конце.
 * Реальный баланс не трогается до конца: в игру уходит только итог ([GameRepository.completeAdventure]).
 */
class AdventureViewModel(
    adventureId: String,
    private val game: GameRepository,
    content: Content,
) : ViewModel() {

    /** null — такого приключения нет (устаревший маршрут): экран сразу закрывается. */
    private val _state = MutableStateFlow(content.adventures.firstOrNull { it.id == adventureId }?.let { AdventureUiState(it) })
    val state: StateFlow<AdventureUiState?> = _state.asStateFlow()

    fun onAction(action: AdventureAction) {
        when (action) {
            is AdventureAction.TogglePiece -> togglePiece(action.index)
            AdventureAction.Pay -> pay()
            AdventureAction.Retry -> retry()
            is AdventureAction.ToggleBasketItem -> toggleBasketItem(action.index)
            AdventureAction.CheckBasket -> submitBasket()
            is AdventureAction.ChooseOption -> chooseOption(action.optionId)
            is AdventureAction.ChooseChange -> chooseChange(action.amount)
            AdventureAction.Next -> next()
        }
    }

    /** Переложить купюру или монету [index] кошелька на прилавок или обратно. */
    private fun togglePiece(index: Int) = update {
        if (scene !is AdventureScene.Pay || check != null) return@update this
        val counter = if (index in onCounter) onCounter - index else onCounter + index
        copy(onCounter = counter)
    }

    private fun pay() = update {
        val pay = scene as? AdventureScene.Pay ?: return@update this
        if (onCounter.isEmpty() || check != null) return@update this
        val result = checkPayment(pay.price, onCounter.map { pay.wallet[it] })
        copy(check = SceneCheck.Payment(result), mistakes = if (result.correct) mistakes else mistakes + 1)
    }

    /** После неверной оплаты или корзины: убрать разбор и поправить. */
    private fun retry() = update {
        val wrong = when (val c = check) {
            is SceneCheck.Payment -> !c.result.correct
            is SceneCheck.Basket -> !c.result.correct
            else -> false
        }
        if (wrong) copy(check = null) else this
    }

    private fun toggleBasketItem(index: Int) = update {
        if (scene !is AdventureScene.Basket || check != null) return@update this
        val basket = if (index in inBasket) inBasket - index else inBasket + index
        copy(inBasket = basket)
    }

    private fun submitBasket() = update {
        val basket = scene as? AdventureScene.Basket ?: return@update this
        if (check != null) return@update this
        val result = checkBasket(basket, inBasket)
        copy(check = SceneCheck.Basket(result), mistakes = if (result.correct) mistakes else mistakes + 1)
    }

    private fun chooseOption(optionId: String) = update {
        val choice = scene as? AdventureScene.Choice ?: return@update this
        val option = choice.options.firstOrNull { it.id == optionId } ?: return@update this
        if (check != null) return@update this
        copy(
            check = SceneCheck.Choice(option),
            chosenOptions = chosenOptions + option.id,
            mistakes = if (option.correct) mistakes else mistakes + 1,
        )
    }

    private fun chooseChange(amount: Int) = update {
        val change = scene as? AdventureScene.Change ?: return@update this
        val paid = paid ?: return@update this
        if (check != null) return@update this
        val answer = SceneCheck.Change(amount, paid - change.price, paid, change.price)
        copy(check = answer, mistakes = if (answer.right) mistakes else mistakes + 1)
    }

    private fun next() {
        val current = _state.value ?: return
        if (!current.canGoNext || current.finish != null) return
        val paid = if (current.scene is AdventureScene.Pay) current.counterSum else current.paid
        val index = current.sceneIndex + 1
        val scene = current.adventure.scenes.getOrNull(index)
        if (scene == null) {
            complete(current.copy(paid = paid))
            return
        }
        // Заплатил ровно — про сдачу не спрашиваем, сразу показываем, что её нет
        val check = (scene as? AdventureScene.Change)?.takeIf { paid == it.price }
            ?.let { SceneCheck.Change(chosen = null, correct = 0, paid = it.price, price = it.price) }
        _state.value = current.copy(sceneIndex = index, onCounter = emptyList(), inBasket = emptySet(), paid = paid, check = check)
    }

    /** Итог уже запрошен: второй тап по «Дальше» не должен засчитать приключение ещё раз (и получить null). */
    private var completing = false

    private fun complete(done: AdventureUiState) {
        if (completing) return
        completing = true
        viewModelScope.launch {
            when (val result = game.completeAdventure(done.adventure, done.mistakes)) {
                is Result.Success -> _state.value = done.copy(finish = AdventureUiState.Finish(result.data))
                // Итог не сохранился и не засчитан: «Дальше» можно нажать ещё раз
                is Result.Error -> {
                    completing = false
                    result.error.snackbar { complete(done) }
                }
            }
        }
    }

    private inline fun update(block: AdventureUiState.() -> AdventureUiState) = _state.update { it?.block() }
}
