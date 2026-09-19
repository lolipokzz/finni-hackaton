package ru.larpinovplay.finniapp.app.di

import org.koin.core.module.dsl.viewModel
import ru.larpinovplay.finniapp.presentation.screens.adult.AdultViewModel
import org.koin.dsl.module
import ru.larpinovplay.finniapp.data.content.defaultContent
import ru.larpinovplay.finniapp.data.game.InMemoryGameRepository
import ru.larpinovplay.finniapp.data.settings.InMemorySettingsRepository
import ru.larpinovplay.finniapp.domain.game.repository.GameRepository
import ru.larpinovplay.finniapp.domain.settings.repository.SettingsRepository
import ru.larpinovplay.finniapp.presentation.screens.home.HomeViewModel
import ru.larpinovplay.finniapp.presentation.screens.petroom.PetRoomScreenViewModel
import ru.larpinovplay.finniapp.presentation.screens.progress.ProgressViewModel
import ru.larpinovplay.finniapp.presentation.screens.savings.SavingsViewModel
import ru.larpinovplay.finniapp.presentation.screens.settings.SettingsViewModel
import ru.larpinovplay.finniapp.presentation.screens.shop.ShopViewModel
import ru.larpinovplay.finniapp.presentation.screens.tasks.TaskPlayViewModel
import ru.larpinovplay.finniapp.presentation.screens.tasks.TasksViewModel

val appModule = module {
    // Репозитории и справочники — общие для экранов и живут, пока жив процесс.
    // Пока в памяти: заменятся на DataStore и загрузку контента из assets (docs/06-architecture.md).
    single<GameRepository> { InMemoryGameRepository() }
    single { defaultContent() }
    single<SettingsRepository> { InMemorySettingsRepository() }

    viewModel { PetRoomScreenViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { TasksViewModel(get(), get()) }
    viewModel { (taskId: String) -> TaskPlayViewModel(taskId, get(), get()) }
    viewModel { ShopViewModel(get(), get()) }
    viewModel { SavingsViewModel(get(), get()) }
    viewModel { ProgressViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { AdultViewModel(get(), get(), get()) }
}
