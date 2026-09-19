package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/** Картинка цели по её id из справочника. */
val SavingsGoal.icon: Int
    @DrawableRes get() = when (id) {
        "room" -> R.drawable.ic_goal_room
        "bike" -> R.drawable.ic_goal_bike
        "console" -> R.drawable.ic_goal_console
        "house" -> R.drawable.ic_goal_house
        else -> R.drawable.ic_pig
    }
