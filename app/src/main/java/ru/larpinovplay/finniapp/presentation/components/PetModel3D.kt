    package ru.larpinovplay.finniapp.presentation.components

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
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
 *
 * Фон прозрачный: модель рисуется поверх Compose-содержимого.
 */
@Composable
fun PetModel3D(
    assetName: String,
    modifier: Modifier = Modifier,
    tintArgb: Long? = null,
    tintMaterial: String = "Main",
    idleAnimation: String = "Idle",
    tapAnimation: String = "Wave",
    cameraDistance: Float = 3.5f,
    animationsEnabled: Boolean = true,
    contentDescription: String = "Питомец. Нажми, и он помашет",
) {
    // key: при смене файла (другая стадия роста) SurfaceView и движок создаются заново,
    // иначе AndroidView оставил бы старую вью с прежней моделью.
    key(assetName, tintArgb, cameraDistance) {
        val controller = remember {
            PetModelController(assetName, tintArgb, tintMaterial, idleAnimation, tapAnimation, cameraDistance)
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
            onRelease = { controller.release() },
        )
    }
}

private class PetModelController(
    private val assetName: String,
    private val tintArgb: Long?,
    private val tintMaterial: String,
    private val idleAnimation: String,
    private val tapAnimation: String,
    private val cameraDistance: Float,
) {
    private var modelViewer: ModelViewer? = null

    /** false — питомец стоит в позе покоя без движения (настройка «Анимации», ТЗ 3.6). */
    @Volatile
    var animationsEnabled: Boolean = true
    private val choreographer = Choreographer.getInstance()

    private var idleIndex = -1
    private var tapIndex = -1
    private var currentIndex = -1
    private var animationStartNanos = 0L

    fun createView(context: Context): SurfaceView {
        val surfaceView = SurfaceView(context).apply {
            // Прозрачный фон поверх остального UI
            setZOrderOnTop(true)
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

        val bytes = context.assets.open(assetName).use { it.readBytes() }
        viewer.loadModelGlb(ByteBuffer.wrap(bytes))
        viewer.transformToUnitCube()
        applyTint(viewer)
        resolveAnimations(viewer.animator)

        choreographer.postFrameCallback(frameCallback)
        return surfaceView
    }

    fun playTapAnimation() {
        if (tapIndex < 0 || !animationsEnabled) return
        switchTo(tapIndex, System.nanoTime())
    }

    fun release() {
        choreographer.removeFrameCallback(frameCallback)
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

    private fun applyTint(viewer: ModelViewer) {
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

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val viewer = modelViewer ?: return
            choreographer.postFrameCallback(this)
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
        var elapsed = (frameTimeNanos - animationStartNanos) / 1_000_000_000.0f
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

        init {
            Utils.init()
        }
    }
}
