package ru.larpinovplay.finniapp.app.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.larpinovplay.finniapp.data.pet.TestPetRepositoryImpl
import ru.larpinovplay.finniapp.domain.pet.repository.PetRepository
import ru.larpinovplay.finniapp.presentation.screens.petroom.PetRoomScreenViewModel

val appModule = module {
    single<PetRepository> { TestPetRepositoryImpl() }
    viewModel { PetRoomScreenViewModel(get()) }
}
