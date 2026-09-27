package ru.larpinovplay.finniapp.presentation.components

import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.ViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.IndirectLight
import android.graphics.BitmapFactory
import com.google.android.filament.Renderer
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.android.TextureHelper
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.Animator
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

/**
 * Показывает glTF-модель питомца из assets и анимирует её.
 *
 * - По кругу проигрывается [idleAnimation]; если его нет, показывается первый кадр приветствия. Idle меняется
 *   вместе с эмоцией питомца: новый включается плавно и не обрывает начатое приветствие или удар.
 * - Сразу после загрузки модели и по нажатию один раз проигрывается [tapAnimation], затем снова [idleAnimation].
 * - Нажатие по голове или по ноге проигрывает клип удара из [hitAnimations] (как в «Моём Говорящем Томе»).
 *   Зона определяется по суставам модели, спроецированным на экран (см. [PetHitAnimations]); если суставов или
 *   клипа нет, играет [tapAnimation].
 * - Если провести по питомцу пальцем, по кругу играет [pettingAnimation] — пока палец двигается; потом клип
 *   доигрывает цикл и питомец возвращается в idle. Нажатием такое касание не считается.
 * - [soundEnabled]: мурчание, пока гладят, и звуки ударов (см. [PetSounds]).
 * - [skin] — раскраска: текстура из assets, которая подменяет текстуру шерсти материала [SKIN_MATERIAL]
 *   (развёртка та же, перекрашена только шерсть); null — текстура из самой модели.
 * - [voiceEnabled]: питомец всё время слушает и повторяет услышанное своим голосом (см. [PetVoice]); пока
 *   ребёнок говорит, прислушивается (голова набок, уши торчком), а повторяя, открывает рот в такт.
 * - [onShadow] каждый кадр получает, где на экране пол под лапами (см. [PetShadow]): тень рисует тот, кто
 *   показывает питомца, под этим видом. Питомец двигается и подпрыгивает — тень идёт за ним и бледнеет.
 * - Вещи гардероба лежат в модели узлами с именами на [ACCESSORY_PREFIX]; видны только перечисленные
 *   в [accessories], остальные скрыты. Смена набора модель не перезагружает.
 * - [tintArgb] перекрашивает материал [tintMaterial] модели в цвет питомца; null — оставить как в файле.
 * - [cameraDistance] — расстояние камеры до модели. Модель вписана в куб со стороной 2,
 *   при вертикальном угле обзора 45° она заполняет ~2 / (0.83 * distance) высоты области:
 *   3.5 → примерно 70 % (модель с широкими ушами и взмахом руки помещается по ширине), 5.0 → примерно 48 %.
 *   Задаётся один раз при создании вида.
 * - Рисуется в квадратный буфер фиксированного размера, поэтому область под вид должна быть квадратной,
 *   иначе картинка растянется.
 *
 * Дорогое (SurfaceView, движок Filament, сцена) создаётся один раз, когда компонент входит в композицию, и не
 * зависит от модели. Смена [assetName] грузит другую модель в тот же вид (null убирает модель), смена [tintArgb]
 * только перекрашивает уже загруженную. Поэтому вид можно создать заранее и даже прогреть любой моделью, а нужную
 * подставить, когда станет известно, какой питомец нужен.
 *
 * - [active] = false ставит отрисовку на паузу, загруженная модель и фаза анимации остаются. Вид при этом не
 *   скрывается и не пересоздаётся: поверхность SurfaceView дорого создавать заново, прятать вид нужно снаружи
 *   (см. [PetHost]). При возврате в true питомец продолжает с того же места.
 *
 * Фон прозрачный: модель рисуется поверх Compose-содержимого.
 */
@Composable
fun PetModel3D(
    assetName: String?,
    modifier: Modifier = Modifier,
    tintArgb: Long? = null,
    modelScale: Float = 1f,
    skin: String? = null,
    tintMaterial: String = "Main",
    idleAnimation: String? = "Idle",
    tapAnimation: String = "Wave",
    hitAnimations: PetHitAnimations = PetHitAnimations(),
    pettingAnimation: String? = null,
    accessories: Set<String> = emptySet(),
    cameraDistance: Float = 3.5f,
    animationsEnabled: Boolean = true,
    soundEnabled: Boolean = true,
    voiceEnabled: Boolean = false,
    onShadow: (PetShadow?) -> Unit = {},
    active: Boolean = true,
    contentDescription: String = "Питомец. Нажми, и он помашет, или погладь его",
    onTap: () -> Unit = {},
    shield: (x: Float, y: Float) -> Boolean = { _, _ -> false },
    onShieldTap: () -> Unit = {},
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentShield by rememberUpdatedState(shield)
    val currentOnShieldTap by rememberUpdatedState(onShieldTap)
    val controller = remember {
        PetModelController(tintMaterial, idleAnimation, tapAnimation, hitAnimations, pettingAnimation, cameraDistance)
    }
    controller.animationsEnabled = animationsEnabled
    controller.soundEnabled = soundEnabled
    controller.voiceEnabled = voiceEnabled
    controller.onShadow = onShadow
    AndroidView(
        modifier = modifier,
        factory = { context ->
            controller.createView(context).apply {
                this.contentDescription = contentDescription
                // Клик не знает координат: запоминаем точку касания, а клик (в том числе от TalkBack) её забирает.
                // Движение пальца дальше порога — это поглаживание, а не нажатие
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                // Касание, начатое над элементом экрана, который лежит под питомцем ([shield], координаты окна),
                // питомцу не достаётся: ни анимации, ни клика — только onShieldTap при отпускании
                val location = IntArray(2)
                var shielded = false
                setOnTouchListener { view, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        view.getLocationInWindow(location)
                        shielded = currentShield(location[0] + event.x, location[1] + event.y)
                    }
                    if (shielded) {
                        when (event.actionMasked) {
                            MotionEvent.ACTION_UP -> { shielded = false; currentOnShieldTap() }
                            MotionEvent.ACTION_CANCEL -> shielded = false
                        }
                        return@setOnTouchListener true
                    }
                    controller.onTouch(event, touchSlop)
                    false
                }
                setOnClickListener { view ->
                    controller.onTap(view.width, view.height)
                    currentOnTap()
                }
            }
        },
        update = {
            controller.setAnimations(idleAnimation, tapAnimation, hitAnimations, pettingAnimation)
            controller.setModel(assetName, tintArgb, modelScale)
            controller.setSkin(skin)
            controller.setAccessories(accessories)
            controller.setActive(active)
        },
        onRelease = { controller.release() },
    )
}

private class PetModelController(
    private val tintMaterial: String,
    private var idleAnimation: String?,
    private var tapAnimation: String,
    private var hitAnimations: PetHitAnimations,
    private var pettingAnimation: String?,
    private val cameraDistance: Float,
) {
    private var modelViewer: ModelViewer? = null
    private lateinit var appContext: Context

    /** false — питомец стоит в позе покоя без движения (настройка «Анимации», ТЗ 3.6). */
    @Volatile
    var animationsEnabled: Boolean = true
    private val choreographer = Choreographer.getInstance()

    private var sounds: PetSounds? = null
    private var voice: PetVoice? = null

    /** Повторять услышанное (настройка, звук и разрешение на микрофон). Микрофон слушает, только пока питомец виден. */
    var voiceEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateListening()
        }

    /** Кости, которые голос поворачивает поверх анимации: рот, голова, уши. */
    private var jawJoint = 0
    private var headJoint = 0
    private var earLeftJoint = 0
    private var earRightJoint = 0

    /** Плавная «прислушивается» 0..1: догоняет [PetVoice.hearing], чтобы голова не дёргалась. */
    private var listenAmount = 0f
    private var view: SurfaceView? = null

    /** Куда отдавать тень питомца (в пикселях вида), см. [PetShadow]. */
    var onShadow: (PetShadow?) -> Unit = {}

    /** Высота пола (мировая Y) и высота костей стоп в позе покоя: от них считается, насколько поднята лапа. */
    private var floorY = Float.NaN
    private var restFootY = Float.NaN

    /** false — питомец молчит (настройка «Звук»). */
    var soundEnabled: Boolean = true
        set(value) {
            field = value
            sounds?.enabled = value
        }

    private var loadedAsset: String? = null
    private var loadedTintArgb: Long? = null
    private var loadedScale = 1f

    /** Какая раскраска нужна и какая сейчас на модели (null — родная текстура модели). */
    private var wantedSkin: String? = null
    private var appliedSkin: String? = null
    private var skinTexture: Texture? = null
    private var accessories: Set<String> = emptySet()

    private var idleIndex = -1
    private var tapIndex = -1
    private var headIndex = -1
    private var footLeftIndex = -1
    private var footRightIndex = -1
    private var pettingIndex = -1

    /** Касание стало поглаживанием: палец ушёл дальше порога. Сбрасывается на следующем касании. */
    private var stroking = false

    /** Когда палец двигался последний раз; клип поглаживания повторяется, пока это было недавно. */
    private var lastStrokeNanos = 0L

    /** Точка последнего касания в координатах вида; NaN — касания не было (клик от клавиатуры или TalkBack). */
    private var touchX = Float.NaN
    private var touchY = Float.NaN
    private var currentIndex = -1
    private var animationStartNanos = 0L

    /** Клип, из которого идёт плавный переход в [currentIndex] (замер на кадре [fadeFromTime]); -1 — перехода нет. */
    private var fadeFromIndex = -1
    private var fadeFromTime = 0f

    private var active = false
    private var pausedAtNanos = System.nanoTime()

    /** Сколько кадров дорисовать на паузе: свежезагруженная модель успевает получить текстуры и шейдеры до показа. */
    private var warmFrames = 0
    private var framePosted = false

    /** Модель загружена, приветствие ждёт первого показанного кадра. */
    private var greetingPending = false

    /** Создаёт вид и движок. Модели пока нет, отрисовка стоит на паузе до [setActive]. */
    fun createView(context: Context): SurfaceView {
        appContext = context.applicationContext
        sounds = PetSounds(appContext).also { it.enabled = soundEnabled }
        voice = PetVoice(appContext).also { v -> v.isBusy = { sounds?.isBusy() == true } }
        val surfaceView = SurfaceView(context).apply {
            // Прозрачный фон поверх остального UI
            setZOrderOnTop(true)
            // Буфер фиксированного размера: вид можно двигать и менять его размер на экране, а система сама
            // масштабирует буфер. Иначе при смене размера Filament продолжает рисовать в старый буфер, и система
            // его отбрасывает (питомец пропадает).
            holder.setFixedSize(SURFACE_SIZE, SURFACE_SIZE)
        }
        val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply { isOpaque = false }
        // transformToUnitCube ставит модель в точку (0, 0, -4); камера смотрит на неё с заданного расстояния
        val manipulator = Manipulator.Builder()
            .targetPosition(0f, 0f, MODEL_Z)
            .orbitHomePosition(0f, 0f, MODEL_Z + cameraDistance)
            .viewport(1, 1)
            .build(Manipulator.Mode.ORBIT)
        val viewer = ModelViewer(surfaceView = surfaceView, uiHelper = uiHelper, manipulator = manipulator)
        modelViewer = viewer

        viewer.view.blendMode = View.BlendMode.TRANSLUCENT
        viewer.scene.skybox = null
        viewer.renderer.clearOptions = Renderer.ClearOptions().apply { clear = true }
        setupLighting(viewer)
        view = surfaceView
        return surfaceView
    }

    /**
     * Ставит в вид модель [assetName] с окрасом [tintArgb]; null убирает модель. Если такая уже стоит, ничего
     * не делает, поэтому вызывать можно при каждой перекомпоновке.
     */
    fun setModel(assetName: String?, tintArgb: Long?, scale: Float = 1f) {
        val viewer = modelViewer ?: return
        if (assetName == loadedAsset && tintArgb == loadedTintArgb && scale == loadedScale) return
        if (assetName != null && assetName == loadedAsset && scale == loadedScale && tintArgb != null) {
            // Та же модель, другой окрас: перекрашиваем материал, перезагружать файл не нужно
            loadedTintArgb = tintArgb
            applyTint(viewer, tintArgb)
            return
        }
        loadedAsset = assetName
        loadedTintArgb = tintArgb
        loadedScale = scale

        dropSkin(viewer)
        if (assetName == null) {
            viewer.destroyModel()
            idleIndex = -1
            tapIndex = -1
            currentIndex = -1
            fadeFromIndex = -1
            greetingPending = false
            return
        }
        val bytes = appContext.assets.open(assetName).use { it.readBytes() }
        viewer.loadModelGlb(ByteBuffer.wrap(bytes))   // прежняя модель уничтожается внутри
        viewer.transformToUnitCube()
        applyScale(viewer, scale)
        applyTint(viewer, tintArgb)
        applyAccessories(viewer)
        applySkin()
        measureFloor(viewer)
        resolveAnimations(viewer.animator)
        switchTo(idleIndex, System.nanoTime())
        // Питомец здоровается при появлении (открытие приложения), дальше — idle. Приветствие стартует с первого
        // реально показанного кадра (см. doFrame), а не сейчас: пока шейдеры модели компилируются, Filament
        // пропускает кадры, и начало приветствия прошло бы вхолостую
        greetingPending = animationsEnabled && tapIndex >= 0
        // Пока на паузе, время анимации не идёт: отсчёт паузы с момента загрузки, чтобы при показе не было скачка
        if (!active) pausedAtNanos = System.nanoTime()
        warmFrames = WARM_FRAMES
        requestFrame()
    }

    /** Раскраска питомца; вызывать можно при каждой перекомпоновке. */
    fun setSkin(skin: String?) {
        wantedSkin = skin
        applySkin()
    }

    /** Ставит на материал шерсти текстуру раскраски, если она ещё не та. */
    private fun applySkin() {
        val viewer = modelViewer ?: return
        if (viewer.asset == null || wantedSkin == appliedSkin) return
        val skin = wantedSkin
        if (skin == null) {
            // Вернуть родную текстуру проще всего, загрузив модель заново (бывает только при смене питомца)
            val asset = loadedAsset
            loadedAsset = null
            setModel(asset, loadedTintArgb, loadedScale)
            return
        }
        val instance = viewer.asset?.instance?.materialInstances?.firstOrNull { it.name == SKIN_MATERIAL } ?: return
        val bitmap = appContext.assets.open(skin).use { BitmapFactory.decodeStream(it) } ?: return
        val engine = viewer.engine
        val levels = 1 + kotlin.math.floor(kotlin.math.log2(maxOf(bitmap.width, bitmap.height).toFloat())).toInt()
        val texture = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .levels(levels)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.SRGB8_A8)   // цвет в sRGB, как текстура цвета в самой модели
            // Загружаем картинку, читаем в шейдере и строим уменьшенные копии (без флага generateMipmaps падает)
            .usage(Texture.Usage.UPLOADABLE or Texture.Usage.SAMPLEABLE or Texture.Usage.GEN_MIPMAPPABLE)
            .build(engine)
        TextureHelper.setBitmap(engine, texture, 0, bitmap)
        texture.generateMipmaps(engine)
        bitmap.recycle()
        instance.setParameter(
            "baseColorMap",
            texture,
            TextureSampler(TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR, TextureSampler.MagFilter.LINEAR, TextureSampler.WrapMode.CLAMP_TO_EDGE),
        )
        skinTexture?.let(engine::destroyTexture)
        skinTexture = texture
        appliedSkin = skin
        requestFrame()
    }

    /** Модель меняется: её материалы уходят, текстура раскраски больше не нужна. */
    private fun dropSkin(viewer: ModelViewer) {
        skinTexture?.let { texture ->
            viewer.destroyModel()
            viewer.engine.destroyTexture(texture)
        }
        skinTexture = null
        appliedSkin = null
    }

    /** Какие вещи показать; вызывать можно при каждой перекомпоновке. */
    fun setAccessories(names: Set<String>) {
        if (names == accessories) return
        accessories = names
        modelViewer?.let(::applyAccessories)
        if (!active) warmFrames = maxOf(warmFrames, 1)   // на паузе дорисовать кадр, чтобы вещь была видна при показе
        requestFrame()
    }

    /** Надетые вещи видны, остальные узлы Acc_* убраны из отрисовки маской слоёв. */
    private fun applyAccessories(viewer: ModelViewer) {
        val asset = viewer.asset ?: return
        val renderables = viewer.engine.renderableManager
        // getEntitiesByPrefix в gltfio 1.71 возвращает пустой список, поэтому перебираем отрисовываемые узлы сами
        asset.renderableEntities.forEach { entity ->
            val name = asset.getName(entity) ?: return@forEach
            if (!name.startsWith(ACCESSORY_PREFIX)) return@forEach
            val instance = renderables.getInstance(entity)
            if (instance == 0) return@forEach
            val visible = name in accessories
            renderables.setLayerMask(instance, ALL_LAYERS, if (visible) ALL_LAYERS else 0)
        }
    }

    /** Контроллер переиспользуется, в том числе при переходе от прогрева к настоящему питомцу. */
    fun setAnimations(idle: String?, tap: String, hits: PetHitAnimations, petting: String?) {
        if (idleAnimation == idle && tapAnimation == tap && hitAnimations == hits && pettingAnimation == petting) return
        idleAnimation = idle
        tapAnimation = tap
        hitAnimations = hits
        pettingAnimation = petting
        val oldIdle = idleIndex
        val animator = modelViewer?.animator ?: return
        resolveAnimations(animator)
        // Сменилась эмоция: если сейчас играет idle, плавно переходим в новый; разовый клип доиграет,
        // а в конце сам вернётся уже в новый idle
        if (idleIndex != oldIdle && (currentIndex == oldIdle || currentIndex < 0)) {
            switchTo(idleIndex, System.nanoTime(), crossFade = true, fromLooping = true)
        }
        if (!active) pausedAtNanos = System.nanoTime()
    }

    fun onTouch(event: MotionEvent, touchSlop: Int) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                stroking = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!stroking && !touchX.isNaN()) {
                    val dx = event.x - touchX
                    val dy = event.y - touchY
                    stroking = dx * dx + dy * dy > touchSlop.toFloat() * touchSlop
                }
                if (stroking) stroke()
            }
        }
    }

    /** Палец водит по питомцу: включаем поглаживание, если оно ещё не играет, и продлеваем его. */
    private fun stroke() {
        if (!animationsEnabled || pettingIndex < 0) return
        val now = System.nanoTime()
        lastStrokeNanos = now
        if (currentIndex != pettingIndex) switchTo(pettingIndex, now, crossFade = true)
    }

    /** Нажатие по виду размером [width]×[height]: удар по голове или ноге, иначе обычная реакция на нажатие. */
    fun onTap(width: Int, height: Int) {
        val x = touchX
        val y = touchY
        touchX = Float.NaN
        touchY = Float.NaN
        if (stroking) {
            // Палец провели, а не нажали: это поглаживание, удара или приветствия не будет
            stroking = false
            return
        }
        if (!animationsEnabled) return
        val index = when (val zone = if (x.isNaN()) null else tapZone(x, y, width, height)) {
            TapZone.HEAD -> headIndex
            TapZone.FOOT_LEFT -> footLeftIndex
            TapZone.FOOT_RIGHT -> footRightIndex
            null -> -1
        }.takeIf { it >= 0 } ?: tapIndex
        // Повторный удар перезапускает клип с начала, как в «Томе»
        if (index >= 0) switchTo(index, System.nanoTime(), crossFade = true)
        when {
            index < 0 -> Unit
            index == headIndex -> sounds?.hitHead()
            index == footLeftIndex || index == footRightIndex -> sounds?.hitFoot()
            else -> sounds?.cancelEffects()
        }
    }

    /**
     * По какой части тела пришлось нажатие. Голова — выше шеи, нога — ниже тазобедренного сустава; какая именно,
     * решает ближайшая по горизонтали стопа. Суставы берутся в текущей позе, поэтому зоны следуют за анимацией.
     */
    private fun tapZone(x: Float, y: Float, width: Int, height: Int): TapZone? {
        val viewer = modelViewer ?: return null
        if (width <= 0 || height <= 0) return null
        val bones = hitAnimations
        val neck = projectJoint(viewer, bones.neckJoint, width, height) ?: return null
        if (y < neck[1]) return TapZone.HEAD
        val left = projectJoint(viewer, bones.footLeftJoint, width, height)
        val right = projectJoint(viewer, bones.footRightJoint, width, height)
        val hipLeft = projectJoint(viewer, bones.hipLeftJoint, width, height)
        val hipRight = projectJoint(viewer, bones.hipRightJoint, width, height)
        if (left == null || right == null || hipLeft == null || hipRight == null) return null
        val hipY = minOf(hipLeft[1], hipRight[1])
        if (y < hipY) return null
        return if (kotlin.math.abs(x - left[0]) <= kotlin.math.abs(x - right[0])) TapZone.FOOT_LEFT else TapZone.FOOT_RIGHT
    }

    /** Экранная точка (x, y в пикселях вида) сустава [name]; null, если такого узла в модели нет. */
    private fun projectJoint(viewer: ModelViewer, name: String, width: Int, height: Int): FloatArray? {
        val world = jointPosition(viewer, name) ?: return null
        return projectPoint(viewer, world[0], world[1], world[2], width, height)
    }

    /** Мировые координаты узла [name]: матрица уже включает transformToUnitCube и текущий кадр анимации. */
    private fun jointPosition(viewer: ModelViewer, name: String): FloatArray? {
        val entity = viewer.asset?.getFirstEntityByName(name)?.takeIf { it != 0 } ?: return null
        val transforms = viewer.engine.transformManager
        val instance = transforms.getInstance(entity).takeIf { it != 0 } ?: return null
        val world = transforms.getWorldTransform(instance, FloatArray(16))
        return floatArrayOf(world[12], world[13], world[14])
    }

    /** Экранная точка (пиксели вида) мировой точки; null, если она за камерой. */
    private fun projectPoint(viewer: ModelViewer, x: Float, y: Float, z: Float, width: Int, height: Int): FloatArray? {
        val point = doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble(), 1.0)
        val eye = multiply(viewer.camera.getViewMatrix(DoubleArray(16)), point)
        val clip = multiply(viewer.camera.getProjectionMatrix(DoubleArray(16)), eye)
        if (clip[3] <= 0.0) return null
        val ndcX = clip[0] / clip[3]
        val ndcY = clip[1] / clip[3]
        return floatArrayOf(((ndcX + 1) / 2 * width).toFloat(), ((1 - ndcY) / 2 * height).toFloat())
    }

    /**
     * Рост питомца ([scale] от вписанного в куб размера). Уменьшаем от точки пола под центром модели: подошвы
     * остаются на полу, а питомец становится ниже. Всё остальное (тень, зоны ударов, голос) считается по костям
     * в мировых координатах и подстраивается само.
     */
    private fun applyScale(viewer: ModelViewer, scale: Float) {
        if (scale == 1f) return
        val asset = viewer.asset ?: return
        val transforms = viewer.engine.transformManager
        val rootInstance = transforms.getInstance(asset.root)
        val root = transforms.getTransform(rootInstance, FloatArray(16))
        val box = asset.boundingBox
        val bottom = box.center[1] - box.halfExtent[1]
        val floor = root[1] * box.center[0] + root[5] * bottom + root[9] * box.center[2] + root[13]
        // Масштаб вокруг (0, floor, MODEL_Z): T · S · T⁻¹ · root, по столбцам
        val pivot = floatArrayOf(0f, floor, MODEL_Z)
        val scaled = FloatArray(16) { i ->
            val col = i / 4
            val row = i % 4
            when {
                row == 3 -> root[i]
                col == 3 -> pivot[row] + (root[i] - pivot[row]) * scale
                else -> root[i] * scale
            }
        }
        transforms.setTransform(rootInstance, scaled)
    }

    /**
     * Пол — низ габаритов модели (подошвы в позе покоя), в мировых координатах после transformToUnitCube.
     * Считается сразу после загрузки, пока кости стоят в позе покоя: тогда же запоминается высота стоп.
     */
    private fun measureFloor(viewer: ModelViewer) {
        val asset = viewer.asset ?: return
        val transforms = viewer.engine.transformManager
        val root = transforms.getWorldTransform(transforms.getInstance(asset.root), FloatArray(16))
        val box = asset.boundingBox
        val bottom = box.center[1] - box.halfExtent[1]
        floorY = root[1] * box.center[0] + root[5] * bottom + root[9] * box.center[2] + root[13]
        restFootY = jointPosition(viewer, hitAnimations.footLeftJoint)?.get(1) ?: Float.NaN
    }

    /**
     * Тень на полу: мягкий эллипс под обеими лапами и тёмное пятно контакта под каждой. Лапа, поднятая над полом,
     * даёт пятно бледнее, а подпрыгнувший питомец — тень меньше и светлее. Без костей стоп (другие модели) —
     * просто пятно под серединой модели.
     */
    private fun updateShadow(viewer: ModelViewer) {
        val v = view
        if (v == null || v.width <= 0 || floorY.isNaN() || viewer.asset == null) return onShadow(null)
        val w = v.width
        val h = v.height
        val left = jointPosition(viewer, hitAnimations.footLeftJoint)
        val right = jointPosition(viewer, hitAnimations.footRightJoint)
        val feet = listOfNotNull(left, right)
        val cx = if (feet.isEmpty()) 0f else feet.map { it[0] }.average().toFloat()
        val cz = if (feet.isEmpty()) 0f else feet.map { it[2] }.average().toFloat()
        val spread = if (left != null && right != null) kotlin.math.abs(left[0] - right[0]) / 2 else 0f
        val lift = if (feet.isEmpty() || restFootY.isNaN()) 0f else
            feet.map { (it[1] - restFootY).coerceAtLeast(0f) }.average().toFloat()

        // Эллипс в мировых размерах: чуть шире лап и неглубокий; перспективу даёт проекция его краёв
        val shrink = 1 + lift * 2.5f
        val halfWidth = (spread + SHADOW_HALF_WIDTH) / shrink
        val center = projectPoint(viewer, cx, floorY, cz, w, h) ?: return onShadow(null)
        val side = projectPoint(viewer, cx + halfWidth, floorY, cz, w, h) ?: return onShadow(null)
        val near = projectPoint(viewer, cx, floorY, cz + SHADOW_HALF_DEPTH / shrink, w, h) ?: return onShadow(null)
        val squash = (near[1] - center[1]) / (side[0] - center[0]).coerceAtLeast(1f)
        val spots = feet.mapNotNull { foot ->
            val footLift = if (restFootY.isNaN()) 0f else (foot[1] - restFootY).coerceAtLeast(0f)
            val c = projectPoint(viewer, foot[0], floorY, foot[2] + FOOT_SPOT_OFFSET, w, h) ?: return@mapNotNull null
            val e = projectPoint(viewer, foot[0] + FOOT_SPOT_RADIUS, floorY, foot[2] + FOOT_SPOT_OFFSET, w, h)
                ?: return@mapNotNull null
            val radius = e[0] - c[0]
            PetShadow.Spot(c[0], c[1], radius, radius * squash, (1f - footLift * 8f).coerceIn(0f, 1f))
        }
        onShadow(
            PetShadow(
                x = center[0],
                y = center[1],
                radiusX = side[0] - center[0],
                radiusY = near[1] - center[1],
                strength = (1f - lift * 3f).coerceIn(0.25f, 1f),
                feet = spots,
            )
        )
    }

    private fun multiply(m: DoubleArray, v: DoubleArray) = DoubleArray(4) { row ->
        m[row] * v[0] + m[4 + row] * v[1] + m[8 + row] * v[2] + m[12 + row] * v[3]
    }

    /**
     * Пауза и продолжение отрисовки. Время паузы вычитается из времени анимации,
     * чтобы после возврата питомец не «перескочил» вперёд.
     */
    fun setActive(value: Boolean) {
        if (value == active) return
        active = value
        if (modelViewer == null) return
        if (value) {
            animationStartNanos += System.nanoTime() - pausedAtNanos
            requestFrame()
        } else {
            pausedAtNanos = System.nanoTime()
            sounds?.pause()
            // Колбэк сам остановится в doFrame, когда кадры на паузе закончатся
        }
        updateListening()
    }

    /** Микрофон включён, только когда питомец на экране и повторять разрешено; иначе молчит и не слушает. */
    fun updateListening() {
        val v = voice ?: return
        if (voiceEnabled && active && modelViewer != null) v.start() else v.stop()
    }

    fun release() {
        choreographer.removeFrameCallback(frameCallback)
        framePosted = false
        sounds?.release()
        sounds = null
        voice?.stop()
        voice = null
        view = null
        onShadow(null)
        // Ресурсы Filament ModelViewer освобождает сам при отсоединении SurfaceView от окна.
        modelViewer = null
    }

    /**
     * Свет. ModelViewer создаёт один направленный источник строго сверху, а Filament учитывает
     * только один направленный источник на сцену, поэтому мы не добавляем второй, а поворачиваем
     * существующий вперёд-вверх и добавляем равномерный рассеянный свет, чтобы ни одна сторона
     * модели не была чёрной.
     */
    private fun setupLighting(viewer: ModelViewer) {
        val lightManager = viewer.engine.lightManager
        viewer.scene.entities
            .filter { lightManager.hasComponent(it) }
            .forEach { entity ->
                val instance = lightManager.getInstance(entity)
                lightManager.setDirection(instance, 0.35f, -0.6f, -0.75f)
                lightManager.setIntensity(instance, 110_000.0f)
            }
        viewer.scene.indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(1.0f, 1.0f, 1.0f))
            .intensity(30_000.0f)
            .build(viewer.engine)
    }

    private fun applyTint(viewer: ModelViewer, tintArgb: Long?) {
        val argb = tintArgb ?: return
        val instance = viewer.asset?.instance?.materialInstances?.firstOrNull { it.name == tintMaterial } ?: return
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        instance.setParameter("baseColorFactor", com.google.android.filament.Colors.RgbaType.SRGB, r, g, b, 1f)
    }

    /** Индексы клипов по именам; текущий клип не меняет. */
    private fun resolveAnimations(animator: Animator?) {
        if (animator == null) return
        val names = (0 until animator.animationCount).map { animator.getAnimationName(it) }
        idleIndex = names.indexOf(idleAnimation)
        tapIndex = names.indexOf(tapAnimation)
        headIndex = hitAnimations.head?.let(names::indexOf) ?: -1
        footLeftIndex = hitAnimations.footLeft?.let(names::indexOf) ?: -1
        footRightIndex = hitAnimations.footRight?.let(names::indexOf) ?: -1
        pettingIndex = pettingAnimation?.let(names::indexOf) ?: -1
        modelViewer?.let { viewer ->
            fun joint(name: String) = viewer.asset?.getFirstEntityByName(name)?.takeIf { it != 0 }
                ?.let { viewer.engine.transformManager.getInstance(it) } ?: 0
            jawJoint = joint(JAW_JOINT)
            headJoint = joint(hitAnimations.headJoint)
            earLeftJoint = joint(EAR_LEFT_JOINT)
            earRightJoint = joint(EAR_RIGHT_JOINT)
        }
    }

    /**
     * Переключает на клип [index] с начала. [crossFade] — плавно перейти из текущего клипа, замерев на его текущем
     * кадре; [fromLooping] — текущий клип зациклен (его время берётся по модулю длины).
     */
    private fun switchTo(
        index: Int,
        nowNanos: Long,
        crossFade: Boolean = false,
        fromLooping: Boolean = currentIndex == idleIndex,
    ) {
        val animator = modelViewer?.animator
        fadeFromIndex = -1
        if (crossFade && animator != null && currentIndex >= 0 && currentIndex != index) {
            val elapsed = ((nowNanos - animationStartNanos) / 1_000_000_000.0f).coerceAtLeast(0f)
            val duration = animator.getAnimationDuration(currentIndex)
            fadeFromIndex = currentIndex
            fadeFromTime = if (fromLooping && duration > 0f) elapsed % duration else elapsed.coerceAtMost(duration)
        }
        currentIndex = index
        animationStartNanos = nowNanos
        // Мурчит, пока играет поглаживание; любой другой клип (или покой) его плавно глушит
        sounds?.setPurring(index >= 0 && index == pettingIndex)
    }

    /** Не больше одного колбэка на кадр: второй postFrameCallback удвоил бы скорость анимации. */
    private fun requestFrame() {
        if (framePosted) return
        framePosted = true
        choreographer.postFrameCallback(frameCallback)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            framePosted = false
            val viewer = modelViewer ?: return
            if (!active) {
                if (warmFrames <= 0) return
                warmFrames--
            }
            requestFrame()
            viewer.animator?.let { animator -> advance(animator, frameTimeNanos) }
            updateShadow(viewer)
            viewer.render(frameTimeNanos)
            if (greetingPending && active) {
                // Первый кадр на экране: дожидаемся, пока GPU его доделает (в том числе скомпилирует шейдеры
                // материалов модели), и только тогда здороваемся — с самого начала клипа
                viewer.engine.flushAndWait()
                greetingPending = false
                if (animationsEnabled && tapIndex >= 0) switchTo(tapIndex, System.nanoTime())
            }
        }
    }

    private fun advance(animator: Animator, frameTimeNanos: Long) {
        if (animator.animationCount == 0) return
        if (!animationsEnabled || currentIndex < 0) {
            // Замираем в первом кадре покоя; время не копится, чтобы после включения не было рывка
            applyRestPose(animator)
            switchTo(idleIndex, frameTimeNanos)
            return
        }
        // Метка кадра бывает чуть раньше момента запуска анимации: отрицательного времени не бывает
        var elapsed = ((frameTimeNanos - animationStartNanos) / 1_000_000_000.0f).coerceAtLeast(0f)
        val duration = animator.getAnimationDuration(currentIndex)
        if (currentIndex == pettingIndex && elapsed >= duration && frameTimeNanos - lastStrokeNanos < PETTING_HOLD_NANOS) {
            // Всё ещё гладят: следующий цикл мурчания без перехода, время идёт непрерывно
            animationStartNanos += (duration * 1_000_000_000L).toLong()
            elapsed -= duration
        }
        if (currentIndex != idleIndex && elapsed >= duration) {
            // Разовая анимация закончилась — возвращаемся в покой (idle эмоции может начинаться не с позы покоя)
            switchTo(idleIndex, frameTimeNanos, crossFade = true, fromLooping = false)
            if (idleIndex < 0) {
                applyRestPose(animator)
                return
            }
            elapsed = 0f
        }
        val time = if (currentIndex == idleIndex && duration > 0f) elapsed % duration else elapsed
        animator.applyAnimation(currentIndex, time)
        if (fadeFromIndex >= 0) {
            val alpha = elapsed / CROSS_FADE_SECONDS
            if (alpha < 1f) animator.applyCrossFade(fadeFromIndex, fadeFromTime, alpha) else fadeFromIndex = -1
        }
        applyVoicePose(frameTimeNanos)
        animator.updateBoneMatrices()
    }

    /**
     * Поверх анимации: пока ребёнок говорит — голова набок и чуть вверх, уши торчком; пока питомец повторяет —
     * рот открывается по громкости. Повороты в осях самих костей: у всех костей модели поза покоя без поворота,
     * ось X — кивок и открытие рта, ось Z — наклон вбок (в Blender это мировые X и -Y).
     */
    private fun applyVoicePose(nowNanos: Long) {
        val v = voice ?: return
        val viewer = modelViewer ?: return
        val target = if (v.hearing) 1f else 0f
        listenAmount += (target - listenAmount) * LISTEN_EASE
        val mouth = v.mouthOpen(nowNanos)
        if (listenAmount < 0.01f && mouth <= 0f) return
        val transforms = viewer.engine.transformManager
        fun turn(joint: Int, degreesX: Float, degreesZ: Float) {
            if (joint == 0 || (degreesX == 0f && degreesZ == 0f)) return
            val local = transforms.getTransform(joint, FloatArray(16))
            transforms.setTransform(joint, multiply4(local, rotationXZ(degreesX, degreesZ)))
        }
        turn(jawJoint, JAW_OPEN_DEGREES * mouth, 0f)
        turn(headJoint, -LISTEN_NOD_DEGREES * listenAmount - TALK_NOD_DEGREES * mouth, -LISTEN_TILT_DEGREES * listenAmount)
        turn(earLeftJoint, -EAR_PERK_X * listenAmount, -EAR_PERK_Z * listenAmount)
        turn(earRightJoint, -EAR_PERK_X * listenAmount, EAR_PERK_Z * listenAmount)
    }

    /** Поворот сначала вокруг Z, затем вокруг X (по столбцам, 4×4). */
    private fun rotationXZ(degreesX: Float, degreesZ: Float): FloatArray {
        val ax = Math.toRadians(degreesX.toDouble())
        val az = Math.toRadians(degreesZ.toDouble())
        val cx = kotlin.math.cos(ax).toFloat()
        val sx = kotlin.math.sin(ax).toFloat()
        val cz = kotlin.math.cos(az).toFloat()
        val sz = kotlin.math.sin(az).toFloat()
        // Rx · Rz
        return floatArrayOf(
            cz, cx * sz, sx * sz, 0f,
            -sz, cx * cz, sx * cz, 0f,
            0f, -sx, cx, 0f,
            0f, 0f, 0f, 1f,
        )
    }

    private fun multiply4(a: FloatArray, b: FloatArray) = FloatArray(16) { i ->
        val col = i / 4
        val row = i % 4
        a[row] * b[col * 4] + a[4 + row] * b[col * 4 + 1] + a[8 + row] * b[col * 4 + 2] + a[12 + row] * b[col * 4 + 3]
    }

    /** Если отдельного idle нет, приветствие не зацикливаем, а оставляем его первый кадр. */
    private fun applyRestPose(animator: Animator) {
        val poseIndex = idleIndex.takeIf { it >= 0 } ?: tapIndex.takeIf { it >= 0 } ?: 0
        animator.applyAnimation(poseIndex, 0f)
        applyVoicePose(System.nanoTime())
        animator.updateBoneMatrices()
    }

    private companion object {
        const val MODEL_Z = -4f
        const val WARM_FRAMES = 3

        /** Длительность плавного перехода между клипами, с. */
        const val CROSS_FADE_SECONDS = 0.3f

        /** Кости, которые двигает голос (голова берётся из [PetHitAnimations.headJoint]). */
        const val JAW_JOINT = "Jaw"

        /** Материал модели кота с текстурой шерсти, её подменяет раскраска. */
        const val SKIN_MATERIAL = "Kitten_Opaque"
        const val EAR_LEFT_JOINT = "Ear.L"
        const val EAR_RIGHT_JOINT = "Ear.R"

        /** Позы голоса, градусы: рот при самом громком звуке, голова «прислушивается», уши торчком. */
        const val JAW_OPEN_DEGREES = 22f
        const val TALK_NOD_DEGREES = 4f
        const val LISTEN_NOD_DEGREES = 6f
        const val LISTEN_TILT_DEGREES = 12f
        const val EAR_PERK_X = 6f
        const val EAR_PERK_Z = 8f

        /** Доля пути к цели за кадр: ~0.25 с на то, чтобы наклонить или выпрямить голову. */
        const val LISTEN_EASE = 0.15f

        /** Размеры тени в мировых единицах модели (питомец вписан в куб со стороной 2). */
        const val SHADOW_HALF_WIDTH = 0.34f
        const val SHADOW_HALF_DEPTH = 0.22f
        const val FOOT_SPOT_RADIUS = 0.15f
        const val FOOT_SPOT_OFFSET = 0.14f   // кость стопы у пятки: пятно вперёд, под подушечки, чтобы его не закрывала лапа

        /** Сколько после последнего движения пальца поглаживание ещё считается продолжающимся, нс. */
        const val PETTING_HOLD_NANOS = 400_000_000L

        /** Сторона квадратного буфера отрисовки, px. Больше экранного размера слота питомца на телефонах. */
        const val SURFACE_SIZE = 1024

        init {
            Utils.init()
        }
    }
}

/**
 * Клипы ударов по частям тела и узлы модели, по которым определяется, куда пришлось нажатие.
 * Лево и право — стороны самого питомца (он стоит к зрителю лицом, так что его левая нога справа на экране).
 * null — для этой части тела удара нет, нажатие обрабатывается как обычное.
 */
data class PetHitAnimations(
    val head: String? = null,
    val footLeft: String? = null,
    val footRight: String? = null,
    val neckJoint: String = "Neck",
    val headJoint: String = "Head",
    val hipLeftJoint: String = "Thigh.L",
    val hipRightJoint: String = "Thigh.R",
    val footLeftJoint: String = "Foot.L",
    val footRightJoint: String = "Foot.R",
)

private enum class TapZone { HEAD, FOOT_LEFT, FOOT_RIGHT }

/** Узлы модели с этим префиксом — вещи гардероба (Acc_Cap, Acc_Glasses, Acc_BowTie в моделях кота). */
const val ACCESSORY_PREFIX = "Acc_"

private const val ALL_LAYERS = 0xFF

/**
 * Где на экране пол под питомцем, в пикселях вида: центр и полуоси общей тени, её плотность (0..1; меньше,
 * когда питомец в прыжке) и пятна контакта под каждой лапой. Рисуется под видом питомца (см. PetHost).
 */
data class PetShadow(
    val x: Float,
    val y: Float,
    val radiusX: Float,
    val radiusY: Float,
    val strength: Float,
    val feet: List<Spot>,
) {
    data class Spot(val x: Float, val y: Float, val radiusX: Float, val radiusY: Float, val strength: Float)
}
