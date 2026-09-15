package ru.larpinovplay.finniapp.domain.pet.model

enum class PetSpecies { CAT, DRAGON, BUNNY }          // 3 силуэта
enum class PetColor(val argb: Long) {                  // 3 тинта, применяются программно
    CORAL(0xFFFF6F61), MINT(0xFF6FCF97), SKY(0xFF56CCF2)
}
data class PetLook(val species: PetSpecies, val color: PetColor)
