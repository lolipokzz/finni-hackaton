package ru.larpinovplay.finniapp.presentation.screens.home

import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetLook

/**
 * Состояние главного экрана (ТЗ 2.5.3): всё, что ребёнок должен видеть одновременно.
 *
 * Пока заполняется подставными значениями через [sample]. Когда появится игровой движок
 * (docs/06-architecture.md), ViewModel будет собирать это состояние из GameState.
 */
data class HomeUiState(
    val petName: String,
    val petLook: PetLook,
    val stats: PetStats,
    val moodExplanation: String,       // одна фраза под питомцем: «Голоден: на этой неделе не было еды»
    val balance: Int,                  // доступные монеты
    val savings: Int,                  // накоплено в копилке
    val goal: GoalUi?,                 // null — цель ещё не выбрана
    val week: Int,                     // номер игрового периода
    val planConfirmed: Boolean,        // фаза периода: false = PLANNING, true = ACTIVE
    val activeTask: TaskUi?,           // первое доступное задание
    val needs: List<NeedUi>,           // чек-лист обязательного на неделю
    val tip: String? = null,           // подсказка в облачке рядом с питомцем
    val suggestedSection: HomeSection? = null, // раздел, куда стоит пойти сейчас; подсвечен в меню
    val demoMode: Boolean = false,
) {
    companion object {
        /** Подставные данные для вёрстки и превью. */
        fun sample(pet: Pet): HomeUiState = HomeUiState(
            petName = pet.name,
            petLook = pet.look,
            stats = PetStats(satiety = 70, care = 45, mood = pet.mood.value),
            moodExplanation = "Ждёт твоих решений",
            balance = 100,
            savings = 15,
            goal = GoalUi(name = "Поход в парк", cost = 60),
            week = 1,
            planConfirmed = false,
            activeTask = TaskUi(title = "Раздели 60 монет", reward = 20),
            needs = listOf(NeedUi("Еда", covered = false), NeedUi("Уход", covered = false)),
            tip = "Давай научимся копить!",
            suggestedSection = HomeSection.PLAN,
        )
    }
}

/** Показатели состояния питомца, 0..100. Подписи и иконки — в StatBar, не только цвет. */
data class PetStats(val satiety: Int, val care: Int, val mood: Int)

data class GoalUi(val name: String, val cost: Int)

data class TaskUi(val title: String, val reward: Int)

data class NeedUi(val label: String, val covered: Boolean)

/** Разделы, доступные с главного экрана (ТЗ 2.5.3, второй пункт). */
enum class HomeSection(val title: String) {
    PLAN("План"),
    TASKS("Задания"),
    SHOP("Магазин"),
    SAVINGS("Копилка"),
    PROGRESS("Прогресс"),
    HELP("Подсказка"),
    ADULT("Для взрослых"),
}

sealed interface HomeAction {
    data class OpenSection(val section: HomeSection) : HomeAction
    data object FinishWeek : HomeAction
    data object PetTapped : HomeAction
}
