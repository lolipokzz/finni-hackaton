package ru.larpinovplay.finniapp.domain.settings.model

/** Настройки, которые может менять ребёнок. Сброс и удаление профиля — в разделе взрослого. */
data class AppSettings(
    val soundEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val tipsEnabled: Boolean = true,
    /**
     * Питомец слушает микрофон и повторяет услышанное своим голосом (звук остаётся на устройстве). Выключено по
     * умолчанию и включается в настройках: микрофон не нужен для обязательного сценария.
     */
    val voiceRepeatEnabled: Boolean = false,
)
