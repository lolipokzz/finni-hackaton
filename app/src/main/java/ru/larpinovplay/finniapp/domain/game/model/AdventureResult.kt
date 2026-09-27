package ru.larpinovplay.finniapp.domain.game.model

/** Пройденное приключение: на какой неделе, без ошибок ли и сколько монет пришло. */
data class AdventureResult(val adventureId: String, val week: Int, val perfect: Boolean, val reward: Int)
