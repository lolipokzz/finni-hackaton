package ru.larpinovplay.finniapp.data.game.store

import ru.larpinovplay.finniapp.domain.game.model.TutorialStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.larpinovplay.finniapp.domain.game.model.PeriodPhase
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.shop.model.WearableSlot

/**
 * Формат файла сохранения. Это копия доменных моделей, а не они сами: домен не знает о JSON, а формат файла
 * меняется отдельно от игры. Новое поле с значением по умолчанию читается из старых файлов без смены версии.
 * Переименование или удаление поля, смена смысла, переименование констант enum — это уже новая
 * [GAME_SAVE_VERSION]: старые сохранения при загрузке сбрасываются.
 */
// 2 — звёзды недели заменены делами недели, шаги роста считаются иначе: старые итоги и рост не переносятся
// 3 — задания стали уровнями карты: результаты отдельных заданий и лимит недели не переносятся
internal const val GAME_SAVE_VERSION = 3

@Serializable
internal data class GameSaveFile(
    val version: Int = GAME_SAVE_VERSION,
    /** null — игра ещё не начата. */
    val game: GameSnapshotDto? = null,
)

/** Только версия: читается первой, чтобы отличить чужую версию от повреждённого файла. */
@Serializable
internal data class GameSaveVersion(val version: Int)

@Serializable
internal data class GameSnapshotDto(
    val state: GameStateDto,
    val pet: PetDto,
)

@Serializable
internal data class GameStateDto(
    val demoMode: Boolean = false,
    val balance: Int = 0,
    val savings: Int = 0,
    val week: Int = 1,
    /** Сохранение без фазы — из версии без плана: начинаем неделю с плана. */
    val phase: PeriodPhase = PeriodPhase.PLANNING,
    val plan: BudgetPlanDto? = null,
    /** День начала недели как `LocalDate.toEpochDay()`; null — день неизвестен. */
    val periodStartedOn: Long? = null,
    val ledger: List<LedgerEntryDto> = emptyList(),
    val purchases: List<ShopItemDto> = emptyList(),
    val wardrobe: List<ShopItemDto> = emptyList(),
    val goal: SavingsGoalDto? = null,
    val completedGoals: List<SavingsGoalDto> = emptyList(),
    val trip: TripDto? = null,
    val depositsThisWeek: List<Int> = emptyList(),
    val depositsByWeek: List<Int> = emptyList(),
    val withdrawalsThisWeek: List<Int> = emptyList(),
    val levelResults: List<LevelResultDto> = emptyList(),
    val goldLevels: Set<String> = emptySet(),
    val adventureResults: List<AdventureResultDto> = emptyList(),
    val history: List<WeekSummaryDto> = emptyList(),
    val tutorial: Boolean = false,   // в старых сохранениях поля нет: обучения там уже не будет
    val tutorialSkipped: Set<TutorialStep> = emptySet(),
)

@Serializable
internal data class PetDto(
    val name: String,
    val color: PetColor,
    val satiety: Int,
    val mood: Int,
    val growthPoints: Int,
    val outfit: Map<WearableSlot, String> = emptyMap(),
)

@Serializable
internal data class LedgerEntryDto(
    val week: Int,
    val reason: LedgerReasonDto,
    val balanceDelta: Int,
    val savingsDelta: Int = 0,
)

/** Имена в [SerialName] — часть формата файла: их нельзя менять вслед за именами классов. */
@Serializable
internal sealed interface LedgerReasonDto {
    @Serializable @SerialName("start_coins")
    data object StartCoins : LedgerReasonDto

    @Serializable @SerialName("purchase")
    data class Purchase(val itemName: String) : LedgerReasonDto

    @Serializable @SerialName("deposit")
    data object Deposit : LedgerReasonDto

    @Serializable @SerialName("planned_deposit")
    data object PlannedDeposit : LedgerReasonDto

    @Serializable @SerialName("withdraw")
    data object Withdraw : LedgerReasonDto

    @Serializable @SerialName("savings_bonus")
    data object SavingsBonus : LedgerReasonDto

    @Serializable @SerialName("goal_reached")
    data class GoalReached(val goalName: String) : LedgerReasonDto

    @Serializable @SerialName("task_reward")
    data class TaskReward(val taskTitle: String) : LedgerReasonDto

    @Serializable @SerialName("adventure_reward")
    data class AdventureReward(val adventureTitle: String) : LedgerReasonDto

    @Serializable @SerialName("week_income")
    data object WeekIncome : LedgerReasonDto
}

@Serializable
internal data class ShopItemDto(
    val id: String,
    val name: String,
    val price: Int,
    val category: ShopCategory,
    val satiety: Int = 0,
    val mood: Int = 0,
    val hint: String = "",
    val slot: WearableSlot? = null,
)

@Serializable
internal data class SavingsGoalDto(
    val id: String,
    val name: String,
    val cost: Int,
    val hint: String = "",
    val trip: Boolean = false,
)

@Serializable
internal data class TripDto(val goalId: String, val week: Int)

@Serializable
internal data class LevelResultDto(
    val levelId: String,
    val week: Int,
    val stars: Int,
    val reward: Int,
)

@Serializable
internal data class AdventureResultDto(
    val adventureId: String,
    val week: Int,
    val perfect: Boolean,
    val reward: Int,
)

@Serializable
internal data class BudgetPlanDto(
    val mandatory: Int = 0,
    val optional: Int = 0,
    val savings: Int = 0,
)

@Serializable
internal data class WeekSummaryDto(
    val week: Int,
    val plan: BudgetPlanDto = BudgetPlanDto(),
    val deeds: WeekDeedsDto,
    val spentMandatory: Int,
    val spentOptional: Int,
    val saved: Int,
    val withdrawn: Int = 0,
    val savingsBonus: Int = 0,
    val moodDelta: Int,
    val lastingMood: Int = 0,        // в старых сохранениях поля нет
    val stageBefore: PetGrowthStage,
    val stageAfter: PetGrowthStage,
    val stepsToNextStage: Int? = null,
    val nextIncome: Int = 0,
)

@Serializable
internal data class WeekDeedsDto(
    val fed: Boolean,
    val notBored: Boolean,
    val savingsOnPlan: Boolean,
    val spendingOnPlan: Boolean,
)
