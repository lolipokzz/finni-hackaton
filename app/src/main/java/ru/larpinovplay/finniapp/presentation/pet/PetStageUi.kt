package ru.larpinovplay.finniapp.presentation.pet

import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage

fun PetGrowthStage.title(): String = when (this) {
    PetGrowthStage.BABY -> "Малыш"
    PetGrowthStage.TEEN -> "Подросток"
    PetGrowthStage.ADULT -> "Взрослый"
}
