package ru.larpinovplay.finniapp.domain.settings.model

/** Настройки, которые может менять ребёнок (ТЗ 3.6). Сброс и удаление профиля — в разделе взрослого. */
data class AppSettings(
    val soundEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val tipsEnabled: Boolean = true,
    /**
     * Питомец слушает микрофон и повторяет услышанное своим голосом (звук остаётся на устройстве).
     * Выключено по умолчанию и включается только взрослым: микрофон не нужен для обязательного сценария (ТЗ 3.1 п. 4).
     */
    val voiceRepeatEnabled: Boolean = false,
)
