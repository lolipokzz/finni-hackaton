package ru.larpinovplay.finniapp.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import androidx.core.os.HandlerCompat
import ru.larpinovplay.finniapp.R

/**
 * Звуки 3D-питомца: мурчание, пока его гладят, и реакции на удары. Файлы — res/raw/pet_*.wav: мурчание —
 * живая запись (freesound.org #117612); удары — синтезированный удар и «мяу» из той же записи; «бойнг»
 * синтезирован. Любой файл можно заменить записью с тем же именем без правок кода.
 *
 * SoundPool, а не MediaPlayer: звуки короткие и должны начаться в тот же кадр, что и анимация.
 * [enabled] = false (настройка «Звук») сразу глушит всё и дальше ничего не играет.
 * Всё вызывается с главного потока, как и остальной PetModelController.
 */
internal class PetSounds(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val purr = pool.load(context, R.raw.pet_purr, 1)
    private val hitHead = pool.load(context, R.raw.pet_hit_head, 1)
    private val hitFoot = pool.load(context, R.raw.pet_hit_foot, 1)
    private val boing = pool.load(context, R.raw.pet_boing, 1)

    private val handler = Handler(Looper.getMainLooper())

    /** До какого момента (System.nanoTime) звучат удары: микрофону в это время слушать нечего, кроме них. */
    @Volatile
    private var effectsUntil = 0L

    /** Питомец сейчас сам издаёт звуки (мурчит или реагирует на удар). Читается из потока микрофона. */
    fun isBusy(): Boolean = purrStream != 0 || System.nanoTime() < effectsUntil

    /** Поток мурчания; 0 — не играет. */
    @Volatile
    private var purrStream = 0
    private var purrVolume = 0f
    private var purrTarget = 0f

    var enabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) stopAll()
        }

    /** Удар по голове: глухой удар и взвизг «мяу!» (удар в клипе HitHead приходится на третий кадр). */
    fun hitHead() {
        cancelEffects()
        play(hitHead, HIT_VOLUME)
        effectsUntil = System.nanoTime() + HEAD_EFFECT_NANOS
    }

    /**
     * Удар по ноге: шлепок с «мя-мя-мяу» сразу и негромкий «бойнг» на каждом прыжке. Моменты прыжков — из клипа HitFoot
     * (отрыв за три кадра до верхней точки прыжка, прыжки через 9 кадров).
     */
    fun hitFoot() {
        cancelEffects()
        play(hitFoot, HIT_VOLUME)
        effectsUntil = System.nanoTime() + FOOT_EFFECT_NANOS
        HOP_SECONDS.forEachIndexed { i, seconds ->
            HandlerCompat.postDelayed(handler, {
                // Каждый следующий прыжок чуть ниже и выше по тону — не звучит как повтор одного файла
                play(boing, BOING_VOLUME * (1f - i * 0.12f), rate = 1f + i * 0.06f)
            }, EFFECTS_TOKEN, (seconds * 1000).toLong())
        }
    }

    /** Обычная реакция (приветствие): звуки ударов, которые ещё должны были прозвучать, отменяются. */
    fun cancelEffects() {
        handler.removeCallbacksAndMessages(EFFECTS_TOKEN)
        effectsUntil = 0L
    }

    /** Мурчание плавно нарастает, пока играет клип поглаживания, и плавно стихает, когда он кончился. */
    fun setPurring(purring: Boolean) {
        if (!enabled && purring) return
        purrTarget = if (purring) PURR_VOLUME else 0f
        if (purring && purrStream == 0) {
            purrVolume = 0f
            purrStream = pool.play(purr, 0f, 0f, 1, LOOP_FOREVER, 1f)
        }
        handler.removeCallbacks(purrFade)
        handler.post(purrFade)
    }

    /** Питомец скрыт (ушли с экрана): тишина. */
    fun pause() = stopAll()

    fun release() {
        stopAll()
        pool.release()
    }

    private val purrFade = object : Runnable {
        override fun run() {
            if (purrStream == 0) return
            val step = if (purrTarget > purrVolume) PURR_FADE_IN_STEP else PURR_FADE_OUT_STEP
            purrVolume = if (purrTarget > purrVolume) {
                minOf(purrTarget, purrVolume + step)
            } else {
                maxOf(purrTarget, purrVolume - step)
            }
            pool.setVolume(purrStream, purrVolume, purrVolume)
            when {
                purrVolume != purrTarget -> handler.postDelayed(this, FADE_TICK_MS)
                purrTarget == 0f -> stopPurr()
            }
        }
    }

    private fun stopPurr() {
        handler.removeCallbacks(purrFade)
        if (purrStream != 0) pool.stop(purrStream)
        purrStream = 0
        purrVolume = 0f
        purrTarget = 0f
    }

    private fun stopAll() {
        cancelEffects()
        stopPurr()
        pool.autoPause()
    }

    private fun play(sound: Int, volume: Float, rate: Float = 1f) {
        if (!enabled) return
        pool.autoResume()
        pool.play(sound, volume, volume, 2, 0, rate)
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val LOOP_FOREVER = -1
        val EFFECTS_TOKEN = Any()

        const val PURR_VOLUME = 0.8f
        const val HIT_VOLUME = 1f
        const val BOING_VOLUME = 0.35f   // тише голоса: прыжки лишь подчёркивают «ай-ай-ай»

        /** Сколько звучит удар по голове и удар по ноге вместе с прыжками, с запасом на эхо в комнате. */
        const val HEAD_EFFECT_NANOS = 900_000_000L
        const val FOOT_EFFECT_NANOS = 2_200_000_000L

        const val FADE_TICK_MS = 30L
        const val PURR_FADE_IN_STEP = PURR_VOLUME * FADE_TICK_MS / 250f    // ~0.25 с до полной громкости
        const val PURR_FADE_OUT_STEP = PURR_VOLUME * FADE_TICK_MS / 450f   // ~0.45 с до тишины

        /** Отрыв на прыжках HitFoot: кадры 5, 14, 23, 32 при 24 кадрах в секунду (кадр 1 — это 0 с). */
        val HOP_SECONDS = listOf(4, 13, 22, 31).map { it / 24f }
    }
}
