package ru.larpinovplay.finniapp.app.di

import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.larpinovplay.finniapp.data.game.store.DataStoreGameStore
import ru.larpinovplay.finniapp.data.game.store.GameStore
import ru.larpinovplay.finniapp.data.settings.DataStoreSettingsRepository
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository

/** Область, в которой DataStore выполняет ввод-вывод. Живёт, пока жив процесс. */
private val StorageScope = named("storageScope")

/**
 * Сохранения на диске. Требуют Context (путь к файлам), поэтому вынесены из [appModule], чтобы тот поднимался в
 * JVM-тесте: там на их место ставятся хранилища в памяти. На каждый файл в процессе допустим один DataStore,
 * поэтому всё здесь `single`.
 */
val storageModule = module {
    single(StorageScope) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    single<GameStore> {
        DataStoreGameStore.create(get(StorageScope)) { androidContext().dataStoreFile("game.json") }
    }

    single<SettingsRepository> {
        DataStoreSettingsRepository.create(get(StorageScope)) { androidContext().dataStoreFile("settings.json") }
    }
}
