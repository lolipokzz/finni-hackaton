package ru.larpinovplay.finniapp.data.pet

import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.repository.PetRepository

class TestPetRepositoryImpl : PetRepository {

    override suspend fun getPet(): Pet? = null

    override suspend fun createPet(pet: Pet): Pet = pet
}
