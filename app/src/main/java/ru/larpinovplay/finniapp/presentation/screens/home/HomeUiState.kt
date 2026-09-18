package ru.larpinovplay.finniapp.presentation.screens.home

import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.pet.model.Pet

/**
 * Состояние главного экрана (ТЗ 2.5.3): всё, что ребёнок должен видеть одновременно.
 * Питомец приходит целиком из домена: имя, вид, сытость, настроение и стадия берутся у него, а не копируются.
 * Фразы здесь не хранятся: [MoodExplanation] и [Tip] описывают, что сказать, а текст подбирает экран.
 */
data class HomeUiState(
    val pet: Pet,
    val moodExplanation: MoodExplanation,  // одна фраза под питомцем
    val balance: Int,                  // доступные монеты
    val savings: Int,                  // накоплено в копилке
    val goal: Goal?,                   // null — цель ещё не выбрана
    val week: Int,                     // номер игрового периода
    val activeTask: ActiveTask?,       // первое доступное задание
    val tip: Tip? = null,              // подсказка в облачке рядом с питомцем; null — не показывать
    val animationsEnabled: Boolean = true,
    val suggestedSection: HomeSection? = null, // раздел, куда стоит пойти сейчас; подсвечен в меню
    val demoMode: Boolean = false,
    val info: HomeInfo? = null,                          // открытое окно «что это значит» у монет/сытости/настроения
    val weekSummary: WeekSummary? = null,     // итог только что закрытой недели; null — окно не показывается
) {
    /** Цель копилки в том виде, в каком её показывает главный экран. */
    data class Goal(val name: String, val cost: Int)

    /** Активное задание на карточке главного экрана. */
    data class ActiveTask(val title: String, val reward: Int)

    /** Почему питомец в таком настроении. */
    sealed interface MoodExplanation {
        data object Hungry : MoodExplanation
        data object Grew : MoodExplanation
        data class Purchased(val itemName: String) : MoodExplanation
        data object Waiting : MoodExplanation
    }

    /** Подсказка в облачке. */
    sealed interface Tip {
        data object ChooseGoal : Tip
        data class SaveFor(val goalName: String) : Tip
    }
}
