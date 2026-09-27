package ru.larpinovplay.finniapp.presentation.screens.savings

import androidx.annotation.DrawableRes
import ru.larpinovplay.finniapp.R
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal

/** Картинка цели по её id из справочника. */
val SavingsGoal.icon: Int
    @DrawableRes get() = when (id) {
        "room" -> R.drawable.ic_goal_room
        "bed" -> R.drawable.ic_bed
        "bike" -> R.drawable.ic_goal_bike
        "sea" -> R.drawable.ic_goal_sea
        else -> R.drawable.ic_pig
    }
