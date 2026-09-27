package ru.larpinovplay.finniapp.presentation.screens.home

import ru.larpinovplay.finniapp.domain.game.model.BudgetPlan
import ru.larpinovplay.finniapp.domain.game.model.FinishBlock
import ru.larpinovplay.finniapp.domain.game.model.WeekDeeds
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.pet.model.Pet

/**
 * Состояние главного экрана (ТЗ 2.5.3): всё, что ребёнок должен видеть одновременно.
 * Питомец приходит целиком из домена: имя, вид, сытость, настроение и стадия берутся у него, а не копируются.
 * Фразы здесь не хранятся: [Speech] описывает, что сказать, а текст подбирает экран.
 */
data class HomeUiState(
    val pet: Pet,
    val balance: Int,                  // доступные монеты
    val savings: Int,                  // накоплено в копилке
    val goal: Goal?,                   // null — цель ещё не выбрана
    val week: Int,                     // номер игрового периода
    val speech: Speech? = null,        // что Финни говорит в облачке и куда зовёт; null — облачка нет
    val tasksBadge: Int = 0,           // сколько заданий и приключений ждёт: значок на «Заданиях»
    val animationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val voiceRepeatEnabled: Boolean = true,
    val demoMode: Boolean = false,
    val info: HomeInfo? = null,                          // открытое окно «что это значит» у монет/сытости/настроения
    val weekSummary: WeekSummary? = null,     // итог только что закрытой недели; null — окно не показывается
    val finishBlock: FinishBlock? = null,     // почему неделю пока нельзя закончить; null — можно
    val planDraft: PlanDraft? = null,         // окно плана недели; null — план уже подтверждён
    val deeds: WeekDeeds = WeekDeeds(fed = false, notBored = false, savingsOnPlan = false, spendingOnPlan = false),   // дела недели сейчас
    val weekSatiety: Int = 0,                 // сколько сытости куплено за неделю: для подсказки «Финни сыт»
    val deedsOpen: Boolean = false,           // открыто окно «Дела недели» (там же — конец недели)
) {
    /**
     * Черновик плана в окне начала недели: монеты недели ([income] + [carried]) и как ребёнок их раскладывает.
     * [need] — сколько минимум стоит еда на неделю: подсказка для строки «Обязательное».
     */
    data class PlanDraft(
        val week: Int,
        val income: Int,
        val carried: Int,
        val plan: BudgetPlan,
        val need: Int,
    ) {
        val budget: Int get() = income + carried
        val unallocated: Int get() = budget - plan.total
        val mandatoryLow: Boolean get() = plan.mandatory < need
    }

    /** Цель копилки в том виде, в каком её показывает главный экран. */
    data class Goal(val name: String, val cost: Int)

    /**
     * Реплика Финни: одно самое важное дело сейчас. Порядок выбора — в [HomeViewModel];
     * куда ведёт кнопка реплики, решает экран (HomeUiText.kt).
     */
    enum class Speech { WEEK_READY, ON_TRIP, HUNGRY, ADVENTURE, CHOOSE_GOAL, BORED, NEW_TASK, TOMORROW }
}
