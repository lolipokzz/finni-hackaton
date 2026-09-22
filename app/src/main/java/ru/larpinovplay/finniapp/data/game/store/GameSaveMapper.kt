package ru.larpinovplay.finniapp.data.game.store

import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.GameState
import ru.larpinovplay.finniapp.domain.game.model.LedgerEntry
import ru.larpinovplay.finniapp.domain.game.model.LedgerReason
import ru.larpinovplay.finniapp.domain.game.model.TaskResult
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetMood
import ru.larpinovplay.finniapp.domain.pet.model.PetSatiety
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem

internal fun GameSnapshot.toDto() = GameSnapshotDto(state = state.toDto(), pet = pet.toDto())

internal fun GameSnapshotDto.toDomain() = GameSnapshot(state = state.toDomain(), pet = pet.toDomain())

private fun GameState.toDto() = GameStateDto(
    balance = balance,
    savings = savings,
    week = week,
    ledger = ledger.map { it.toDto() },
    purchases = purchases.map { it.toDto() },
    goal = goal?.toDto(),
    completedGoals = completedGoals.map { it.toDto() },
    depositsThisWeek = depositsThisWeek,
    depositsByWeek = depositsByWeek,
    taskResults = taskResults.map { it.toDto() },
    tasksDoneThisWeek = tasksDoneThisWeek,
    history = history.map { it.toDto() },
)

private fun GameStateDto.toDomain() = GameState(
    balance = balance,
    savings = savings,
    week = week,
    ledger = ledger.map { it.toDomain() },
    purchases = purchases.map { it.toDomain() },
    goal = goal?.toDomain(),
    completedGoals = completedGoals.map { it.toDomain() },
    depositsThisWeek = depositsThisWeek,
    depositsByWeek = depositsByWeek,
    taskResults = taskResults.map { it.toDomain() },
    tasksDoneThisWeek = tasksDoneThisWeek,
    history = history.map { it.toDomain() },
)

private fun Pet.toDto() = PetDto(
    name = name,
    species = look.species,
    color = look.color,
    satiety = satiety.value,
    mood = mood.value,
    growthPoints = growthPoints,
)

private fun PetDto.toDomain() = Pet(
    name = name,
    look = PetLook(species, color),
    satiety = PetSatiety(satiety),
    mood = PetMood(mood),
    growthPoints = growthPoints,
)

private fun LedgerEntry.toDto() = LedgerEntryDto(
    week = week,
    reason = reason.toDto(),
    balanceDelta = balanceDelta,
    savingsDelta = savingsDelta,
)

private fun LedgerEntryDto.toDomain() = LedgerEntry(
    week = week,
    reason = reason.toDomain(),
    balanceDelta = balanceDelta,
    savingsDelta = savingsDelta,
)

private fun LedgerReason.toDto(): LedgerReasonDto = when (this) {
    LedgerReason.StartCoins -> LedgerReasonDto.StartCoins
    is LedgerReason.Purchase -> LedgerReasonDto.Purchase(itemName)
    LedgerReason.Deposit -> LedgerReasonDto.Deposit
    is LedgerReason.GoalReached -> LedgerReasonDto.GoalReached(goalName)
    is LedgerReason.TaskReward -> LedgerReasonDto.TaskReward(taskTitle)
    LedgerReason.WeekIncome -> LedgerReasonDto.WeekIncome
}

private fun LedgerReasonDto.toDomain(): LedgerReason = when (this) {
    LedgerReasonDto.StartCoins -> LedgerReason.StartCoins
    is LedgerReasonDto.Purchase -> LedgerReason.Purchase(itemName)
    LedgerReasonDto.Deposit -> LedgerReason.Deposit
    is LedgerReasonDto.GoalReached -> LedgerReason.GoalReached(goalName)
    is LedgerReasonDto.TaskReward -> LedgerReason.TaskReward(taskTitle)
    LedgerReasonDto.WeekIncome -> LedgerReason.WeekIncome
}

private fun ShopItem.toDto() = ShopItemDto(id, name, price, category, satiety, mood, hint)

private fun ShopItemDto.toDomain() = ShopItem(id, name, price, category, satiety, mood, hint)

private fun SavingsGoal.toDto() = SavingsGoalDto(id, name, cost, hint)

private fun SavingsGoalDto.toDomain() = SavingsGoal(id, name, cost, hint)

private fun TaskResult.toDto() = TaskResultDto(taskId, week, success, reward)

private fun TaskResultDto.toDomain() = TaskResult(taskId, week, success, reward)

private fun WeekSummary.toDto() = WeekSummaryDto(
    week = week,
    foodCovered = foodCovered,
    savedSomething = savedSomething,
    spentMandatory = spentMandatory,
    spentOptional = spentOptional,
    saved = saved,
    score = score,
    moodDelta = moodDelta,
    stageBefore = stageBefore,
    stageAfter = stageAfter,
)

private fun WeekSummaryDto.toDomain() = WeekSummary(
    week = week,
    foodCovered = foodCovered,
    savedSomething = savedSomething,
    spentMandatory = spentMandatory,
    spentOptional = spentOptional,
    saved = saved,
    score = score,
    moodDelta = moodDelta,
    stageBefore = stageBefore,
    stageAfter = stageAfter,
)
