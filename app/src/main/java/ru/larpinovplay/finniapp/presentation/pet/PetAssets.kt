package ru.larpinovplay.finniapp.presentation.pet

import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.domain.pet.model.PetSpecies
import ru.larpinovplay.finniapp.presentation.components.PetHitAnimations
import ru.larpinovplay.finniapp.presentation.components.PetSpec

/**
 * Сопоставление вида питомца и стадии роста с 3D-моделью в assets.
 * Модели лежат в папке по виду: assets/<species>/{baby,teen,adult}.glb.
 * Кот временно использует cat.glb на всех стадиях роста.
 * Цвет питомца в файл не входит: материал "Main" перекрашивается программно в PetColor.
 * Возвращает null, если для вида модели пока нет — тогда UI рисует запасной вариант.
 */
fun PetLook.modelAsset(stage: PetGrowthStage): String? {
    val folder = when (species) {
        PetSpecies.BUNNY -> "bunny"
        PetSpecies.CAT -> return "cat/cat.glb"
        PetSpecies.DRAGON -> return null
    }
    val file = when (stage) {
        PetGrowthStage.BABY -> "baby"
        PetGrowthStage.TEEN -> "teen"
        PetGrowthStage.ADULT -> "adult"
    }
    return "$folder/$file.glb"
}

/** Эмоция, которую показывает 3D-питомец. Голод важнее настроения (docs/04-rules-and-formulas.md). */
enum class PetEmotion { HAPPY, CALM, SAD, HUNGRY }

val Pet.emotion: PetEmotion
    get() = when {
        isHungry -> PetEmotion.HUNGRY
        else -> when (mood.level) {
            MoodLevel.HAPPY -> PetEmotion.HAPPY
            MoodLevel.NEUTRAL -> PetEmotion.CALM
            MoodLevel.SAD -> PetEmotion.SAD
        }
    }

/**
 * Имена клипов из GLB: ожидание зациклено и зависит от эмоции (у кота; у остальных один Idle),
 * приветствие запускается при появлении питомца и по нажатию.
 */
val Pet.idleAnimation: String?
    get() = if (look.species != PetSpecies.CAT) "Idle" else when (emotion) {
        PetEmotion.HAPPY -> "IdleHappy"
        PetEmotion.CALM -> "Idle"
        PetEmotion.SAD -> "IdleSad"
        PetEmotion.HUNGRY -> "IdleHungry"
    }

val PetLook.tapAnimation: String
    get() = if (species == PetSpecies.CAT) "Greeting" else "Wave"

/** Поглаживание (водят пальцем по питомцу): клип есть только у кота. */
val PetLook.pettingAnimation: String?
    get() = if (species == PetSpecies.CAT) "Petting" else null

/** Удары по голове и ногам: клипы есть только у кота (HitHead, HitFoot.L/R в cat.glb). */
val PetLook.hitAnimations: PetHitAnimations
    get() = if (species == PetSpecies.CAT) {
        PetHitAnimations(head = "HitHead", footLeft = "HitFoot.L", footRight = "HitFoot.R")
    } else {
        PetHitAnimations()
    }

/** Есть ли у модели этого вида вещи гардероба. Пока одеть можно только кота. */
val PetLook.supportsWardrobe: Boolean
    get() = species == PetSpecies.CAT

/** Узел вещи [itemId] в 3D-модели (cat.glb); null — на этой модели вещь не показывается. */
fun PetLook.accessoryNode(itemId: String): String? =
    if (!supportsWardrobe) null else when (itemId) {
        "cap" -> "Acc_Cap"
        "glasses" -> "Acc_Glasses"
        "bowtie" -> "Acc_BowTie"
        else -> null
    }

/** Узлы надетых вещей: что показать на модели. */
val Pet.accessoryNodes: Set<String>
    get() = outfit.values.mapNotNull(look::accessoryNode).toSet()

/**
 * Модель, которой прогревают отрисовку ещё до создания питомца (см. PetHostState.warmUp): вид питомца и стадия
 * не важны, лишь бы материалы были теми же, что у настоящих моделей. Кот со всеми вещами: у вещей свои материалы
 * (без текстуры, полупрозрачные стёкла), а тело — тот же текстурный материал, что у остальных моделей.
 */
val PetWarmUpSpec: PetSpec = PetLook(PetSpecies.CAT, PetColor.CORAL).let { look ->
    PetSpec(
        assetName = checkNotNull(look.modelAsset(PetGrowthStage.BABY)),
        tintArgb = null,
        animationsEnabled = false,
        soundEnabled = false,
        accessories = listOf("cap", "glasses", "bowtie").mapNotNull(look::accessoryNode).toSet(),
    )
}
