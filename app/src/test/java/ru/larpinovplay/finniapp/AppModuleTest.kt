package ru.larpinovplay.finniapp

import ru.larpinovplay.finniapp.presentation.screens.adult.AdultViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import ru.larpinovplay.finniapp.app.di.appModule
import ru.larpinovplay.finniapp.data.content.FEEDBACK_ASSET
import ru.larpinovplay.finniapp.data.content.parseFeedback
import ru.larpinovplay.finniapp.data.game.store.GameStore
import ru.larpinovplay.finniapp.data.game.store.InMemoryGameStore
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.content.Content
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
import ru.larpinovplay.finniapp.presentation.screens.home.HomeViewModel
import ru.larpinovplay.finniapp.presentation.screens.petcreation.PetCreationViewModel
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressViewModel
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsViewModel
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsViewModel
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopViewModel
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskPlayViewModel
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksViewModel

/**
 * Проводка Koin: неверный `get()` в конструкторе иначе выяснится только падением при открытии экрана.
 * Поднимаем модуль без Android (свой [koinApplication], глобального Koin не трогаем) и достаём всё.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {

    // assetsModule (нужен Context) подменён файлом с диска: остальное поднимается как в приложении
    private val testAssetsModule = module {
        single { parseFeedback(File("src/main/assets/$FEEDBACK_ASSET").readText()) }
    }

    // storageModule (файлы на диске, нужен Context) подменён хранилищами в памяти
    private val testStorageModule = module {
        single<GameStore> { InMemoryGameStore() }
        single<SettingsRepository> { InMemorySettingsRepository() }
    }

    private val koin: Koin = koinApplication { modules(appModule, testAssetsModule, testStorageModule) }.koin

    /** ViewModel запускает корутины в init на Dispatchers.Main, а его в JVM-тесте нет. */
    @Before
    fun setMainDispatcher() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun repositoriesAndContentResolve() {
        koin.get<GameRepository>()
        koin.get<SettingsRepository>()
        koin.get<Content>()
        koin.get<Feedback>()
    }

    @Test
    fun repositoriesAreShared() {
        assertSame(koin.get<GameRepository>(), koin.get<GameRepository>())
        assertSame(koin.get<SettingsRepository>(), koin.get<SettingsRepository>())
    }

    @Test
    fun petCreationResolvesBeforeGameStarts() {
        koin.get<PetCreationViewModel>()
    }

    /** Остальные экраны открываются только после создания питомца, поэтому и в тесте игра сначала начата. */
    @Test
    fun everyGameScreenViewModelResolves() = runBlocking {
        koin.get<GameRepository>().createPet(Pet.newborn("Финни", PetLook(PetSpecies.BUNNY, PetColor.CORAL)))

        koin.get<HomeViewModel>()
        koin.get<TasksViewModel>()
        koin.get<TaskPlayViewModel> { parametersOf(koin.get<Content>().tasks.first().id) }
        koin.get<ShopViewModel>()
        koin.get<SavingsViewModel>()
        koin.get<ProgressViewModel>()
        koin.get<SettingsViewModel>()
        koin.get<AdultViewModel>()
        Unit
    }
}
