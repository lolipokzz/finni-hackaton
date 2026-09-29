package ru.larpinovplay.finniapp.domain.game.model

/**
 * Уровень на карте: закрыт (его неделя не началась или не пройден предыдущий уровень), можно проходить,
 * уже пройден (можно в золотое испытание).
 */
enum class LevelStatus { LOCKED, AVAILABLE, DONE }
