package ru.larpinovplay.finniapp.domain.game.model

/** Первое прохождение уровня: на какой неделе, сколько звёзд (1–3) и монет получено. */
data class LevelResult(val levelId: String, val week: Int, val stars: Int, val reward: Int)
