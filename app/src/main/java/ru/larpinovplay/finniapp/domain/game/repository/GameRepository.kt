package ru.larpinovplay.finniapp.domain.game.repository

import kotlinx.coroutines.flow.StateFlow
import ru.larpinovplay.finniapp.domain.game.model.DepositResult
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.game.model.PurchaseResult
import ru.larpinovplay.finniapp.domain.game.model.WeekSummary
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.shop.model.ShopItem
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result

/**
 * Единственная точка, через которую экраны читают игру и отдают ей команды.
 * Питомец — часть игры: он лежит в том же [snapshot], что и состояние, и обновляется с ним одной записью,
 * поэтому экран не увидит новые монеты со старой сытостью. Читать снимок можно, менять его умеют только
 * команды ниже (правила каждой описаны в GameEngine). Команды выполняются последовательно.
 *
 * Игра хранится на диске, поэтому у каждой команды два независимых исхода:
 * - что решили правила игры (например, [PurchaseResult.NotEnough]) лежит внутри [Result.Success];
 * - что не удалось сохранить — это [Result.Error] с [StorageError]. Команда применяется только вместе с записью:
 *   при ошибке [snapshot] остаётся прежним, то есть равным тому, что лежит на диске.
 *
 * Правило, отклонённое игрой, ничего не пишет и потому ошибки хранения дать не может.
 */
interface GameRepository {

    /** Игра целиком; null, пока нет игры: сохранения ещё нет или оно не загружено ([load]). */
    val snapshot: StateFlow<GameSnapshot?>

    /**
     * Загружает сохранённую игру и публикует её в [snapshot]. Вызывается один раз при запуске.
     * Success(null) — сохранения нет, нужно создать питомца. При [StorageError.CORRUPTED] и
     * [StorageError.INCOMPATIBLE_VERSION] сохранение уже сброшено: [snapshot] остаётся null, начинать заново.
     */
    suspend fun load(): Result<GameSnapshot?, StorageError>

    /**
     * Начинает игру с питомцем [pet].
     * @throws IllegalStateException если игра уже начата.
     */
    suspend fun createPet(pet: Pet): EmptyResult<StorageError>

    suspend fun buy(item: ShopItem): Result<PurchaseResult, StorageError>

    suspend fun chooseGoal(goal: SavingsGoal): EmptyResult<StorageError>

    suspend fun deposit(amount: Int): Result<DepositResult, StorageError>

    /** Достигнутая цель или null, если цели нет или на неё ещё не накоплено. */
    suspend fun reachGoal(): Result<SavingsGoal?, StorageError>

    /** Итог ответа или null, если задание сейчас недоступно. */
    suspend fun answerTask(task: Task, answer: TaskAnswer): Result<TaskOutcome?, StorageError>

    suspend fun finishWeek(): Result<WeekSummary, StorageError>

    /** Удаляет игру целиком; настройки управляются отдельно. */
    suspend fun resetProfile(): EmptyResult<StorageError>

    /** Заменяет текущую игру чистым тестовым профилем. */
    suspend fun resetToDemo(): EmptyResult<StorageError>
}

/** Игра для экранов, которые открываются только после создания питомца. */
fun GameRepository.requireSnapshot(): GameSnapshot = checkNotNull(snapshot.value) { "Питомец ещё не создан" }
