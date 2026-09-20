package ru.larpinovplay.finniapp

import androidx.datastore.core.Serializer
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.store.GameSaveFile
import ru.larpinovplay.finniapp.data.game.store.GameSaveSerializer
import ru.larpinovplay.finniapp.data.game.store.GameStore
import ru.larpinovplay.finniapp.data.settings.SettingsSaveFile
import ru.larpinovplay.finniapp.data.settings.SettingsSaveSerializer
import ru.larpinovplay.finniapp.domain.game.engine.GameEngine
import ru.larpinovplay.finniapp.domain.game.model.GameSnapshot
import ru.larpinovplay.finniapp.domain.goal.model.SavingsGoal
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.shop.model.ShopCategory
import ru.larpinovplay.finniapp.domain.storage.StorageError
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload
import ru.larpinovplay.finniapp.domain.util.result.EmptyDataSuccess
import ru.larpinovplay.finniapp.domain.util.result.EmptyResult
import ru.larpinovplay.finniapp.domain.util.result.Result
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Примеры игр для тестов хранения. */
internal object SampleGames {

    val newborn: Pet = Pet.newborn("Финни", PetLook(PetSpecies.BUNNY, PetColor.CORAL))

    /**
     * Игра после нескольких недель: в ней есть все виды записей журнала, покупки, цель, достигнутая цель,
     * результат задания и итог недели. Собрана настоящим движком, чтобы формат проверялся на живых данных.
     */
    fun rich(): GameSnapshot {
        val content = defaultContent()
        val food = content.shopItems.first { it.category == ShopCategory.MANDATORY }
        val treat = content.shopItems.first { it.category == ShopCategory.OPTIONAL }
        val cheapGoal = SavingsGoal(id = "test-ball", name = "Мяч", cost = 10, hint = "Копить недолго")
        val choice = content.tasks.first { it.payload is TaskPayload.Choice }
        val correct = (choice.payload as TaskPayload.Choice).options.first { it.correct }

        var game = GameSnapshot(GameEngine.newGame(startBalance = 100), newborn)
        game = GameEngine.buy(game, food).game
        game = GameEngine.buy(game, treat).game
        game = GameEngine.answerTask(game, choice, TaskAnswer.Choice(correct.id)).game
        game = GameEngine.chooseGoal(game, cheapGoal).game
        game = GameEngine.deposit(game, 20).game
        game = GameEngine.reachGoal(game).game
        game = GameEngine.chooseGoal(game, content.goals.first()).game
        game = GameEngine.deposit(game, 5).game
        game = GameEngine.finishWeek(game).game
        game = GameEngine.buy(game, food).game
        return game
    }

    /** Ещё одно, отличное от [rich], состояние: на неделю дальше. */
    fun richer(): GameSnapshot = GameEngine.finishWeek(rich()).game
}

/** Хранилище для тестов репозитория: помнит записи и умеет по команде отвечать ошибками. */
internal class FakeGameStore(var saved: GameSnapshot? = null) : GameStore {

    var loadFailure: StorageError? = null
    var saveFailure: StorageError? = null
    val saves = mutableListOf<GameSnapshot>()

    override suspend fun load(): Result<GameSnapshot?, StorageError> =
        loadFailure?.let { Result.Error(it) } ?: Result.Success(saved)

    override suspend fun save(snapshot: GameSnapshot): EmptyResult<StorageError> {
        saveFailure?.let { return Result.Error(it) }
        saved = snapshot
        saves += snapshot
        return EmptyDataSuccess
    }
}

/** Сериализатор файла игры, который по команде притворяется сбоем диска. */
internal class FlakyGameSerializer : Serializer<GameSaveFile> {
    @Volatile var failReads = false
    @Volatile var failWrites = false

    override val defaultValue: GameSaveFile get() = GameSaveSerializer.defaultValue

    override suspend fun readFrom(input: InputStream): GameSaveFile {
        if (failReads) throw IOException("диск не читается")
        return GameSaveSerializer.readFrom(input)
    }

    override suspend fun writeTo(t: GameSaveFile, output: OutputStream) {
        if (failWrites) throw IOException("нет места на диске")
        GameSaveSerializer.writeTo(t, output)
    }
}

/** То же для файла настроек. */
internal class FlakySettingsSerializer : Serializer<SettingsSaveFile> {
    @Volatile var failReads = false
    @Volatile var failWrites = false

    override val defaultValue: SettingsSaveFile get() = SettingsSaveSerializer.defaultValue

    override suspend fun readFrom(input: InputStream): SettingsSaveFile {
        if (failReads) throw IOException("диск не читается")
        return SettingsSaveSerializer.readFrom(input)
    }

    override suspend fun writeTo(t: SettingsSaveFile, output: OutputStream) {
        if (failWrites) throw IOException("нет места на диске")
        SettingsSaveSerializer.writeTo(t, output)
    }
}
