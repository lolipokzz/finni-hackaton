package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.larpinovplay.finniapp.presentation.components.VoiceDsp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Голос питомца: тон выше, длительность та же (как в «Говорящем Томе»), рот открывается по громкости. */
class VoiceDspTest {

    private val rate = 22050

    private fun tone(freq: Double, seconds: Double) =
        FloatArray((rate * seconds).toInt()) { (0.5 * sin(2 * PI * freq * it / rate)).toFloat() }

    /** Частота по числу пересечений нуля в середине сигнала (края отрезаем: там затухания). */
    private fun frequency(x: FloatArray): Double {
        val from = x.size / 4
        val to = x.size * 3 / 4
        var crossings = 0
        for (i in from + 1 until to) if ((x[i - 1] < 0f) != (x[i] < 0f)) crossings++
        return crossings / 2.0 / ((to - from).toDouble() / rate)
    }

    @Test
    fun pitchGoesUpWithoutSpeedingUp() {
        val input = tone(200.0, 1.5)

        val output = VoiceDsp.pitchShift(input, 1.6f, rate)

        assertEquals(320.0, frequency(output), 12.0)
        assertTrue("длительность почти та же: ${output.size} vs ${input.size}", abs(output.size - input.size) < rate * 0.05)
    }

    @Test
    fun envelopeFollowsLoudness() {
        // Тишина, громко, тишина: рот закрыт, открыт, закрыт
        val quiet = FloatArray(rate / 3)
        val speech = tone(300.0, 0.4)
        val envelope = VoiceDsp.envelope(quiet + speech + quiet, rate, 30)

        assertEquals(0f, envelope[3], 0.01f)
        assertTrue(envelope[15] > 0.8f)
        assertEquals(0f, envelope[envelope.size - 2], 0.05f)
    }

    @Test
    fun normalizeRaisesQuietSpeechButNotBeyondLimit() {
        val whisper = FloatArray(rate) { (0.01 * sin(2 * PI * 250 * it / rate)).toFloat() }

        val loud = VoiceDsp.normalize(whisper, rate)

        val peak = loud.maxOf { abs(it) }
        assertEquals(0.08f, peak, 0.01f)   // ×8 — предел усиления, дальше вылез бы шум
    }
}
