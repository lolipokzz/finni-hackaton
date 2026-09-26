package ru.larpinovplay.finniapp.presentation.pet

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage

fun PetGrowthStage.title(): String = when (this) {
    PetGrowthStage.BABY -> "Малыш"
    PetGrowthStage.TEEN -> "Подросток"
    PetGrowthStage.ADULT -> "Взрослый"
}

/** Кем станет питомец на следующей стадии: «…станет подростком». Для последней стадии — пусто. */
fun nextStageTitle(stage: PetGrowthStage): String = when (stage) {
    PetGrowthStage.BABY -> "подростком"
    PetGrowthStage.TEEN -> "взрослым"
    PetGrowthStage.ADULT -> ""
}
