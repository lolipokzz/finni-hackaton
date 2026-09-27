package ru.larpinovplay.finniapp.domain.pet.model

enum class PetSpecies { CAT, DRAGON, BUNNY }          // 3 силуэта
/**
 * Раскраска питомца; [argb] — цвет образца на экране выбора (и тинт для моделей с перекрашиваемым материалом).
 * Имена констант — часть файла сохранения: CORAL, MINT, SKY были раньше, их не переименовывать.
 * CORAL — обычная раскраска кота (рыжая, как в модели), остальные — перекрашенная шерсть.
 */
enum class PetColor(val argb: Long) {
    CORAL(0xFFD4A064),
    GRAY(0xFF8C8C95),
    BLACK(0xFF3B3641),
    CREAM(0xFFEBCB9C),
    CHOCOLATE(0xFF7B4A31),
    SKY(0xFF7FA9DA),
    MINT(0xFF7FD0A8),
    PINK(0xFFEA8FB7),
    LAVENDER(0xFFA895E3),
}
data class PetLook(val species: PetSpecies, val color: PetColor)
