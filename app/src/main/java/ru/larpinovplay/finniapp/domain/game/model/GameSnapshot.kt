package ru.larpinovplay.finniapp.domain.game.model

import ru.larpinovplay.finniapp.domain.pet.model.Pet

/** Игра целиком: то, что меняет одна команда вместе (покупка списывает монеты и кормит питомца) и хранится одной записью. */
data class GameSnapshot(val state: GameState, val pet: Pet)

/** Результат команды движка: новое [game] и то, что нужно показать ребёнку ([result]). */
data class Transition<out R>(val game: GameSnapshot, val result: R)
