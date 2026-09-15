package ru.larpinovplay.finniapp.domain.pet.model

data class Pet(
    val name: String,
    val look: PetLook,
    val mood: PetMood,                 // обратимо, меняется после каждого решения
    val growthStage: PetGrowthStage,   // необратимо, копится за периоды
    val growthProgress: Int            // 0..100 — сколько накоплено до следующей стадии
)
