package ru.larpinovplay.finniapp.presentation.components

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.IndirectLight
import com.google.android.filament.Renderer
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
 * - По кругу проигрывается [idleAnimation].
 * - По нажатию один раз проигрывается [tapAnimation], затем снова [idleAnimation].
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
    tintMaterial: String = "Main",
    idleAnimation: String = "Idle",
    tapAnimation: String = "Wave",
    cameraDistance: Float = 3.5f,
    animationsEnabled: Boolean = true,
    active: Boolean = true,
    contentDescription: String = "Питомец. Нажми, и он помашет",
) {
    val controller = remember {
        PetModelController(tintMaterial, idleAnimation, tapAnimation, cameraDistance)
    }
    controller.animationsEnabled = animationsEnabled
    AndroidView(
        modifier = modifier,
        factory = { context ->
            controller.createView(context).apply {
                this.contentDescription = contentDescription
                setOnClickListener { controller.playTapAnimation() }
            }
        },
        update = {
            controller.setAnimations(idleAnimation, tapAnimation)
            controller.setModel(assetName, tintArgb)
            controller.setActive(active)
        },
        onRelease = { controller.release() },
    )
}

private class PetModelController(
    private val tintMaterial: String,
    private var idleAnimation: String,
    private var tapAnimation: String,
    private val cameraDistance: Float,
) {
    private var modelViewer: ModelViewer? = null
    private lateinit var appContext: Context

    /** false — питомец стоит в позе покоя без движения (настройка «Анимации», ТЗ 3.6). */
    @Volatile
    var animationsEnabled: Boolean = true
    private val choreographer = Choreographer.getInstance()

    private var loadedAsset: String? = null
    private var loadedTintArgb: Long? = null

    private var idleIndex = -1
    private var tapIndex = -1
    private var currentIndex = -1
    private var animationStartNanos = 0L

    private var active = false
    private var pausedAtNanos = System.nanoTime()

    /** Сколько кадров дорисовать на паузе: свежезагруженная модель успевает получить текстуры и шейдеры до показа. */
    private var warmFrames = 0
    private var framePosted = false

    /** Создаёт вид и движок. Модели пока нет, отрисовка стоит на паузе до [setActive]. */
    fun createView(context: Context): SurfaceView {
        appContext = context.applicationContext
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
        return surfaceView
    }

    /**
     * Ставит в вид модель [assetName] с окрасом [tintArgb]; null убирает модель. Если такая уже стоит, ничего
     * не делает, поэтому вызывать можно при каждой перекомпоновке.
     */
    fun setModel(assetName: String?, tintArgb: Long?) {
        val viewer = modelViewer ?: return
        if (assetName == loadedAsset && tintArgb == loadedTintArgb) return
        if (assetName != null && assetName == loadedAsset && tintArgb != null) {
            // Та же модель, другой окрас: перекрашиваем материал, перезагружать файл не нужно
            loadedTintArgb = tintArgb
            applyTint(viewer, tintArgb)
            return
        }
        loadedAsset = assetName
        loadedTintArgb = tintArgb

        if (assetName == null) {
            viewer.destroyModel()
            idleIndex = -1
            tapIndex = -1
            currentIndex = -1
            return
        }
        val bytes = appContext.assets.open(assetName).use { it.readBytes() }
        viewer.loadModelGlb(ByteBuffer.wrap(bytes))   // прежняя модель уничтожается внутри
        viewer.transformToUnitCube()
        applyTint(viewer, tintArgb)
        resolveAnimations(viewer.animator)
        // Пока на паузе, время анимации не идёт: отсчёт паузы с момента загрузки, чтобы при показе не было скачка
        if (!active) pausedAtNanos = System.nanoTime()
        warmFrames = WARM_FRAMES
        requestFrame()
    }

    /** Контроллер переиспользуется, в том числе при переходе от прогрева к настоящему питомцу. */
    fun setAnimations(idle: String, tap: String) {
        if (idleAnimation == idle && tapAnimation == tap) return
        idleAnimation = idle
        tapAnimation = tap
        resolveAnimations(modelViewer?.animator)
        if (!active) pausedAtNanos = System.nanoTime()
    }

    fun playTapAnimation() {
        if (tapIndex < 0 || !animationsEnabled) return
        switchTo(tapIndex, System.nanoTime())
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
            // Колбэк сам остановится в doFrame, когда кадры на паузе закончатся
        }
    }

    fun release() {
        choreographer.removeFrameCallback(frameCallback)
        framePosted = false
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

    private fun resolveAnimations(animator: Animator?) {
        if (animator == null) return
        val names = (0 until animator.animationCount).map { animator.getAnimationName(it) }
        idleIndex = names.indexOf(idleAnimation).takeIf { it >= 0 } ?: 0
        tapIndex = names.indexOf(tapAnimation)
        switchTo(idleIndex, System.nanoTime())
    }

    private fun switchTo(index: Int, nowNanos: Long) {
        currentIndex = index
        animationStartNanos = nowNanos
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
            viewer.render(frameTimeNanos)
        }
    }

    private fun advance(animator: Animator, frameTimeNanos: Long) {
        if (animator.animationCount == 0 || currentIndex < 0) return
        if (!animationsEnabled) {
            // Замираем в первом кадре покоя; время не копится, чтобы после включения не было рывка
            animator.applyAnimation(idleIndex, 0f)
            animator.updateBoneMatrices()
            animationStartNanos = frameTimeNanos
            return
        }
        // Метка кадра бывает чуть раньше момента запуска анимации: отрицательного времени не бывает
        var elapsed = ((frameTimeNanos - animationStartNanos) / 1_000_000_000.0f).coerceAtLeast(0f)
        val duration = animator.getAnimationDuration(currentIndex)
        if (currentIndex != idleIndex && elapsed >= duration) {
            // Разовая анимация закончилась — возвращаемся в покой
            switchTo(idleIndex, frameTimeNanos)
            elapsed = 0f
        }
        val time = if (currentIndex == idleIndex && duration > 0f) elapsed % duration else elapsed
        animator.applyAnimation(currentIndex, time)
        animator.updateBoneMatrices()
    }

    private companion object {
        const val MODEL_Z = -4f
        const val WARM_FRAMES = 3

        /** Сторона квадратного буфера отрисовки, px. Больше экранного размера слота питомца на телефонах. */
        const val SURFACE_SIZE = 1024

        init {
            Utils.init()
        }
    }
}
