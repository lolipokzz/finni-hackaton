package ru.larpinovplay.finniapp.domain.pet.repository

import ru.larpinovplay.finniapp.domain.pet.model.Pet

interface PetRepository {

    suspend fun getPet(): Pet?

    suspend fun createPet(pet: Pet): Pet
}