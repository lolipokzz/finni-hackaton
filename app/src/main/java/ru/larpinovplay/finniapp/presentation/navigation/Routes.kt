package ru.larpinovplay.finniapp.presentation.navigation

import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ru.larpinovplay.finniapp.presentation.screens.home.HomeSection

/**
 * Граф навигации (docs/07-screens.md#граф-навигации), после того как питомец создан:
 *
 * ```
 * Home ─┬─ Tasks ─┬─ TaskPlay ── TaskResult (диалог) ── назад на Tasks
 *       │         └─ AdventurePlay ── назад на Tasks
 *       ├─ Shop ── (нехватка монет) ── Tasks
 *       │    └─ (купил одежду) ── Wardrobe
 *       ├─ Wardrobe ── (пусто) ── Shop
 *       ├─ Savings
 *       ├─ Progress
 *       ├─ Settings
 *       └─ Adult
 * ```
 *
 * Каждый маршрут — ключ back stack'а: сериализуемый, поэтому стек переживает поворот экрана
 * и смерть процесса. «Назад» (кнопка на экране и системная) снимает верхний ключ.
 * Точка входа — [Home]: знакомство с Финни (раскраска, имя, короткое обучение) идёт до графа, см. PetCreationScreen.
 */
@Serializable
data object Home : NavKey

@Serializable
data object Tasks : NavKey

@Serializable
data class TaskPlay(val taskId: String) : NavKey

/** Итог задания. Показывается диалогом поверх [Tasks], поэтому несёт результат в самом ключе. */
@Serializable
data class TaskResult(val taskId: String, val outcome: TaskOutcome) : NavKey

/** Приключение недели: сюжет по шагам, итог показывается на том же экране. */
@Serializable
data class AdventurePlay(val adventureId: String) : NavKey

@Serializable
data object Shop : NavKey

@Serializable
data object Wardrobe : NavKey

@Serializable
data object Savings : NavKey

@Serializable
data object Progress : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object Adult : NavKey

/** Куда ведёт кнопка раздела на главном экране. */
fun HomeSection.toRoute(): NavKey = when (this) {
    HomeSection.TASKS -> Tasks
    HomeSection.SHOP -> Shop
    HomeSection.WARDROBE -> Wardrobe
    HomeSection.SAVINGS -> Savings
    HomeSection.PROGRESS -> Progress
    HomeSection.SETTINGS -> Settings
    HomeSection.ADULT -> Adult
}
