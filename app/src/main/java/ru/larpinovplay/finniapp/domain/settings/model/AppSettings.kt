package ru.larpinovplay.finniapp.domain.settings.model

/** Настройки, которые может менять ребёнок (ТЗ 3.6). Сброс и удаление профиля — в разделе взрослого. */
data class AppSettings(
    val soundEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val tipsEnabled: Boolean = true,
)
