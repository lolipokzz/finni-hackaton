package ru.larpinovplay.finniapp.presentation.screens.home

import androidx.compose.runtime.Composable
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import ru.larpinovplay.finniapp.presentation.feedback.LocalFeedback
import ru.larpinovplay.finniapp.presentation.screens.home.HomeUiState.Speech

/** Что Финни говорит в облачке. */
@Composable
fun Speech.text(): String = LocalFeedback.current.text(
    when (this) {
        Speech.PLAN_WEEK -> FeedbackKey.SAY_PLAN_WEEK
        Speech.WEEK_READY -> FeedbackKey.SAY_WEEK_READY
        Speech.DEEDS_LEFT -> FeedbackKey.SAY_DEEDS_LEFT
        Speech.ON_TRIP -> FeedbackKey.SAY_ON_TRIP
        Speech.HUNGRY -> FeedbackKey.SAY_HUNGRY
        Speech.ADVENTURE -> FeedbackKey.SAY_ADVENTURE
        Speech.CHOOSE_GOAL -> FeedbackKey.SAY_CHOOSE_GOAL
        Speech.BORED -> FeedbackKey.SAY_BORED
        Speech.NEW_TASK -> FeedbackKey.SAY_NEW_TASK
        Speech.TOMORROW -> FeedbackKey.SAY_TOMORROW
    }
)

/** Кнопка под репликой: подпись и действие; null — на сегодня всё, звать некуда. */
val Speech.button: Pair<String, HomeAction>?
    get() = when (this) {
        Speech.PLAN_WEEK -> "Составим план" to HomeAction.OpenPlan
        Speech.WEEK_READY -> "Смотрим!" to HomeAction.ShowDeeds
        Speech.DEEDS_LEFT -> "Дела недели" to HomeAction.ShowDeeds
        Speech.HUNGRY -> "Сходим в магазин" to HomeAction.OpenSection(HomeSection.SHOP)
        Speech.ADVENTURE -> "Вперёд!" to HomeAction.OpenSection(HomeSection.TASKS)
        Speech.CHOOSE_GOAL -> "В копилку" to HomeAction.OpenSection(HomeSection.SAVINGS)
        Speech.BORED -> "Выберем радость" to HomeAction.OpenSection(HomeSection.SHOP)
        Speech.NEW_TASK -> "К заданиям" to HomeAction.OpenSection(HomeSection.TASKS)
        Speech.ON_TRIP -> null
        Speech.TOMORROW -> null
    }
