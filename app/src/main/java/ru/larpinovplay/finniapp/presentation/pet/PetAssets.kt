package ru.larpinovplay.finniapp.presentation.pet

import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies

/**
 * Сопоставление внешнего вида питомца с 3D-моделью в assets.
 * Возвращает null, если для комбинации модели пока нет — тогда UI рисует запасной вариант.
 */
fun PetLook.modelAsset(): String? = when {
    species == PetSpecies.BUNNY && color == PetColor.CORAL -> "pet.glb"
    else -> null
}
