package ru.larpinovplay.finniapp.presentation.pet

import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.PetSpec

/**
 * Сопоставление вида питомца и стадии роста с 3D-моделью в assets.
 * Модели лежат в папке по виду: assets/<species>/{baby,teen,adult}.glb.
 * Цвет питомца в файл не входит: материал "Main" перекрашивается программно в PetColor.
 * Возвращает null, если для вида модели пока нет — тогда UI рисует запасной вариант.
 */
fun PetLook.modelAsset(stage: PetGrowthStage): String? {
    val folder = when (species) {
        PetSpecies.BUNNY -> "bunny"
        PetSpecies.CAT, PetSpecies.DRAGON -> return null
    }
    val file = when (stage) {
        PetGrowthStage.BABY -> "baby"
        PetGrowthStage.TEEN -> "teen"
        PetGrowthStage.ADULT -> "adult"
    }
    return "$folder/$file.glb"
}

/**
 * Модель, которой прогревают отрисовку ещё до создания питомца (см. PetHostState.warmUp): вид питомца и стадия
 * не важны, лишь бы материалы были теми же, что у настоящих моделей.
 */
val PetWarmUpSpec: PetSpec = PetSpec(
    assetName = checkNotNull(PetLook(PetSpecies.BUNNY, PetColor.CORAL).modelAsset(PetGrowthStage.BABY)),
    tintArgb = null,
    animationsEnabled = false,
)
