package ru.larpinovplay.finniapp.data.game.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory

/**
 * Формат файла сохранения. Это копия доменных моделей, а не они сами: домен не знает о JSON, а формат файла
 * меняется отдельно от игры. Новое поле с значением по умолчанию читается из старых файлов без смены версии.
 * Переименование или удаление поля, смена смысла, переименование констант enum — это уже новая
 * [GAME_SAVE_VERSION]: старые сохранения при загрузке сбрасываются.
 */
internal const val GAME_SAVE_VERSION = 1

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
    val balance: Int = 0,
    val savings: Int = 0,
    val week: Int = 1,
    val ledger: List<LedgerEntryDto> = emptyList(),
    val purchases: List<ShopItemDto> = emptyList(),
    val goal: SavingsGoalDto? = null,
    val completedGoals: List<SavingsGoalDto> = emptyList(),
    val depositsThisWeek: List<Int> = emptyList(),
    val depositsByWeek: List<Int> = emptyList(),
    val taskResults: List<TaskResultDto> = emptyList(),
    val tasksDoneThisWeek: Int = 0,
    val history: List<WeekSummaryDto> = emptyList(),
)

@Serializable
internal data class PetDto(
    val name: String,
    val species: PetSpecies,
    val color: PetColor,
    val satiety: Int,
    val mood: Int,
    val growthPoints: Int,
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

    @Serializable @SerialName("goal_reached")
    data class GoalReached(val goalName: String) : LedgerReasonDto

    @Serializable @SerialName("task_reward")
    data class TaskReward(val taskTitle: String) : LedgerReasonDto

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
)

@Serializable
internal data class SavingsGoalDto(
    val id: String,
    val name: String,
    val cost: Int,
    val hint: String = "",
)

@Serializable
internal data class TaskResultDto(
    val taskId: String,
    val week: Int,
    val success: Boolean,
    val reward: Int,
)

@Serializable
internal data class WeekSummaryDto(
    val week: Int,
    val foodCovered: Boolean,
    val savedSomething: Boolean,
    val spentMandatory: Int,
    val spentOptional: Int,
    val saved: Int,
    val score: Int,
    val moodDelta: Int,
    val stageBefore: PetGrowthStage,
    val stageAfter: PetGrowthStage,
)
