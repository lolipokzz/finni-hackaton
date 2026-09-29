package ru.larpinovplay.finniapp.domain.pet.model

/** Раскраска питомца; [argb] — цвет образца на экране выбора (и тинт для моделей с перекрашиваемым материалом). */
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
/** Внешность питомца. Питомец в игре один — кот, поэтому выбирается только раскраска. */
data class PetLook(val color: PetColor)
