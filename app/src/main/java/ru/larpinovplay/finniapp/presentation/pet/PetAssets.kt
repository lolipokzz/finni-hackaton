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
 * У кота на каждой стадии своя модель с тем же скелетом и анимациями: малыш крупноголовый и коротколапый,
 * подросток вытянутый, взрослый — исходный. Рост на экране задаёт [modelScale].
 * Цвет питомца в файл не входит: у кота подменяется текстура шерсти ([skinAsset]), у остальных материал "Main"
 * перекрашивается программно в PetColor.
 * Возвращает null, если для вида модели пока нет — тогда UI рисует запасной вариант.
 */
fun PetLook.modelAsset(stage: PetGrowthStage): String? {
    val folder = when (species) {
        PetSpecies.BUNNY -> "bunny"
        PetSpecies.CAT -> "cat"
        PetSpecies.DRAGON -> return null
    }
    val file = when (stage) {
        PetGrowthStage.BABY -> "baby"
        PetGrowthStage.TEEN -> "teen"
        PetGrowthStage.ADULT -> "adult"
    }
    return "$folder/$file.glb"
}

/** Название раскраски для ребёнка. */
val PetColor.title: String
    get() = when (this) {
        PetColor.CORAL -> "Обычный"
        PetColor.GRAY -> "Серый"
        PetColor.BLACK -> "Чёрный"
        PetColor.CREAM -> "Кремовый"
        PetColor.CHOCOLATE -> "Шоколадный"
        PetColor.SKY -> "Голубой"
        PetColor.MINT -> "Мятный"
        PetColor.PINK -> "Розовый"
        PetColor.LAVENDER -> "Лавандовый"
    }

/**
 * Текстура шерсти кота для раскраски (assets/cat/skins): та же развёртка, что в модели, перекрашена только
 * рыжая шерсть с полосками — футболка, глаза, нос остаются. null — обычная раскраска, текстура из модели.
 */
val PetLook.skinAsset: String?
    get() = if (species != PetSpecies.CAT || color == PetColor.CORAL) null else "cat/skins/${color.name.lowercase()}.webp"

/**
 * Рост питомца на экране относительно взрослого: модель каждой стадии сама вписывается в одинаковый кадр,
 * а малыш и подросток должны быть меньше. Питомец уменьшается от пола, поэтому стоит на нём.
 */
val PetGrowthStage.modelScale: Float
    get() = when (this) {
        PetGrowthStage.BABY -> 0.68f
        PetGrowthStage.TEEN -> 0.85f
        PetGrowthStage.ADULT -> 1f
    }

/** Эмоция, которую показывает 3D-питомец. Голод важнее настроения (docs/04-rules-and-formulas.md). */
enum class PetEmotion { HAPPY, CALM, BORED, HUNGRY }

val Pet.emotion: PetEmotion
    get() = when {
        isHungry -> PetEmotion.HUNGRY
        else -> when (mood.level) {
            MoodLevel.HAPPY -> PetEmotion.HAPPY
            MoodLevel.NEUTRAL -> PetEmotion.CALM
            MoodLevel.BORED -> PetEmotion.BORED
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
        PetEmotion.BORED -> "IdleSad"   // клип в модели называется так, показываем его как «скучает»
        PetEmotion.HUNGRY -> "IdleHungry"
    }

val PetLook.tapAnimation: String
    get() = if (species == PetSpecies.CAT) "Greeting" else "Wave"

/** Поглаживание (водят пальцем по питомцу): клип есть только у кота. */
val PetLook.pettingAnimation: String?
    get() = if (species == PetSpecies.CAT) "Petting" else null

/** Удары по голове и ногам: клипы есть только у кота (HitHead, HitFoot.L/R в его моделях). */
val PetLook.hitAnimations: PetHitAnimations
    get() = if (species == PetSpecies.CAT) {
        PetHitAnimations(head = "HitHead", footLeft = "HitFoot.L", footRight = "HitFoot.R")
    } else {
        PetHitAnimations()
    }

/** Есть ли у модели этого вида вещи гардероба. Пока одеть можно только кота. */
val PetLook.supportsWardrobe: Boolean
    get() = species == PetSpecies.CAT

/** Узел вещи [itemId] в 3D-модели кота (одинаковый на всех стадиях); null — на этой модели вещь не показывается. */
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
