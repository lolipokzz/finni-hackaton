package ru.larpinovplay.finniapp.presentation.pet

import ru.larpinovplay.finniapp.domain.pet.model.MoodLevel
import ru.larpinovplay.finniapp.domain.pet.model.Pet
import ru.larpinovplay.finniapp.domain.pet.model.PetColor
import ru.larpinovplay.finniapp.domain.pet.model.PetGrowthStage
import ru.larpinovplay.finniapp.domain.pet.model.PetLook
import ru.larpinovplay.finniapp.presentation.components.PetHitAnimations
import ru.larpinovplay.finniapp.presentation.components.PetSpec

/**
 * 3D-модель кота для стадии роста: assets/cat/{baby,teen,adult}.glb. Питомец в игре один — кот.
 * На каждой стадии своя модель с тем же скелетом и анимациями: малыш крупноголовый и коротколапый,
 * подросток вытянутый, взрослый — исходный. Рост на экране задаёт [modelScale].
 * Цвет питомца в файл не входит: подменяется текстура шерсти ([skinAsset]).
 */
val PetGrowthStage.modelAsset: String
    get() = when (this) {
        PetGrowthStage.BABY -> "cat/baby.glb"
        PetGrowthStage.TEEN -> "cat/teen.glb"
        PetGrowthStage.ADULT -> "cat/adult.glb"
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
    get() = if (color == PetColor.CORAL) null else "cat/skins/${color.name.lowercase()}.webp"

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
 * Имена клипов из GLB кота: ожидание зациклено и зависит от эмоции,
 * приветствие ([PetTapAnimation]) запускается при появлении питомца и по нажатию.
 */
val Pet.idleAnimation: String
    get() = when (emotion) {
        PetEmotion.HAPPY -> "IdleHappy"
        PetEmotion.CALM -> "Idle"
        PetEmotion.BORED -> "IdleSad"   // клип в модели называется так, показываем его как «скучает»
        PetEmotion.HUNGRY -> "IdleHungry"
    }

const val PetTapAnimation = "Greeting"

/** Поглаживание: водят пальцем по питомцу. */
const val PetPettingAnimation = "Petting"

/** Удары по голове и ногам (HitHead, HitFoot.L/R в моделях кота). */
val PetHits = PetHitAnimations(head = "HitHead", footLeft = "HitFoot.L", footRight = "HitFoot.R")

/** Узел вещи [itemId] в 3D-модели кота (одинаковый на всех стадиях); null — эта вещь на модели не показывается. */
fun accessoryNode(itemId: String): String? = when (itemId) {
    "cap" -> "Acc_Cap"
    "glasses" -> "Acc_Glasses"
    "bowtie" -> "Acc_BowTie"
    else -> null
}

/** Узлы надетых вещей: что показать на модели. */
val Pet.accessoryNodes: Set<String>
    get() = outfit.values.mapNotNull(::accessoryNode).toSet()

/**
 * Модель, которой прогревают отрисовку ещё до создания питомца (см. PetHostState.warmUp): стадия не важна,
 * лишь бы материалы были теми же, что у настоящих моделей. Кот со всеми вещами: у вещей свои материалы
 * (без текстуры, полупрозрачные стёкла), а тело — тот же текстурный материал, что у остальных стадий.
 */
val PetWarmUpSpec: PetSpec = PetSpec(
    assetName = PetGrowthStage.BABY.modelAsset,
    tintArgb = null,
    animationsEnabled = false,
    soundEnabled = false,
    accessories = listOf("cap", "glasses", "bowtie").mapNotNull(::accessoryNode).toSet(),
)
