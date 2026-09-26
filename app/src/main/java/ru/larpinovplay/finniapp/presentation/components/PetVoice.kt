package ru.larpinovplay.finniapp.presentation.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * «Повторюшка», как в «Моём Говорящем Томе»: питомец всё время слушает микрофон, ловит фразу и повторяет её
 * своим голосом — выше по тону, но с той же скоростью.
 *
 * - Фраза ловится по громкости: начало — когда звук заметно громче фонового шума, конец — ~0.7 с тишины.
 *   Слишком короткие щелчки отбрасываются, длина ограничена [MAX_PHRASE_SECONDS].
 * - Тон поднимается в [PITCH] раз без ускорения: запись растягивается по времени (WSOLA) и затем
 *   пересэмплируется обратно к исходной длине.
 * - Пока питомец говорит сам или играют его звуки ([isBusy]), микрофон не слушается: иначе он повторял бы себя.
 * - Звук никуда не сохраняется и не отправляется: всё в памяти, фраза живёт до конца проигрывания.
 *
 * Запись и обработка — в своём потоке; наружу — только [hearing] и [mouthOpen], их читает кадр отрисовки.
 */
internal class PetVoice(private val context: Context) {

    /** Питомцу сейчас нельзя слушать (играет мурчание или звук удара). Вызывается из потока записи. */
    @Volatile
    var isBusy: () -> Boolean = { false }

    /** Ребёнок сейчас говорит (идёт фраза): питомец прислушивается. */
    @Volatile
    var hearing = false
        private set

    @Volatile
    private var speech: Speech? = null

    @Volatile
    private var running = false
    private var thread: Thread? = null

    /** Насколько открыт рот (0..1) в момент [nowNanos]: огибающая громкости того, что питомец сейчас говорит. */
    fun mouthOpen(nowNanos: Long): Float {
        val s = speech ?: return 0f
        val index = ((nowNanos - s.startNanos) / 1e9 * ENVELOPE_RATE).toInt()
        return if (index in s.envelope.indices) s.envelope[index] else 0f
    }

    fun start() {
        if (running || !hasPermission()) return
        running = true
        thread = Thread(::loop, "PetVoice").apply {
            priority = Thread.NORM_PRIORITY + 1
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        hearing = false
        speech = null
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")   // проверено в start()
    private fun loop() {
        val minBuffer = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuffer, FRAME * 8))
        } catch (e: RuntimeException) {
            running = false
            return
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            running = false
            return
        }
        val detector = PhraseDetector()
        val frame = ShortArray(FRAME)
        try {
            recorder.startRecording()
            while (running) {
                val read = recorder.read(frame, 0, FRAME)
                if (read <= 0) continue
                if (speech != null || isBusy()) {
                    // Свой голос и свои звуки не слушаем; фон начнём копить заново
                    detector.reset()
                    hearing = false
                    continue
                }
                val phrase = detector.feed(frame, read)
                hearing = detector.inPhrase
                if (phrase != null) {
                    hearing = false
                    speak(phrase)
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }

    /** Поднимает тон и проигрывает; возвращается, когда питомец договорил. */
    private fun speak(phrase: FloatArray) {
        val voice = VoiceDsp.normalize(VoiceDsp.pitchShift(phrase, PITCH, RATE), RATE)
        val pcm = ShortArray(voice.size) { (voice[it] * Short.MAX_VALUE).toInt().toShort() }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            speech = Speech(VoiceDsp.envelope(voice, RATE, ENVELOPE_RATE), System.nanoTime())
            track.play()
            val endAt = System.nanoTime() + (pcm.size * 1e9 / RATE).toLong() + TAIL_NANOS
            while (running && System.nanoTime() < endAt) Thread.sleep(20)
        } finally {
            track.release()
            speech = null
        }
    }

    private class Speech(val envelope: FloatArray, val startNanos: Long)

    /**
     * Ловит фразу в потоке кадров по 20 мс. Фон — медленно следящий уровень тишины; речь — заметно громче фона.
     * Перед началом фразы держит [PRE_ROLL_FRAMES] кадров, чтобы не обрезать первый звук.
     */
    private class PhraseDetector {
        private var noise = 0.004f
        private var loudRun = 0
        private var quietRun = 0
        private var voicedFrames = 0
        var inPhrase = false
            private set
        private val preRoll = ArrayDeque<FloatArray>()
        private val phrase = ArrayList<FloatArray>()

        fun reset() {
            loudRun = 0
            quietRun = 0
            voicedFrames = 0
            inPhrase = false
            preRoll.clear()
            phrase.clear()
        }

        fun feed(samples: ShortArray, count: Int): FloatArray? {
            val frame = FloatArray(count) { samples[it] / 32768f }
            var sum = 0f
            for (v in frame) sum += v * v
            val rms = sqrt(sum / count)
            val loud = rms > max(noise * START_RATIO, MIN_SPEECH_RMS)

            if (!inPhrase) {
                preRoll.addLast(frame)
                if (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()
                if (!loud) noise = noise * 0.97f + rms * 0.03f          // фон подстраивается только в тишине
                loudRun = if (loud) loudRun + 1 else 0
                if (loudRun >= START_FRAMES) {
                    inPhrase = true
                    phrase.addAll(preRoll)
                    preRoll.clear()
                    voicedFrames = loudRun
                    quietRun = 0
                }
                return null
            }

            phrase += frame
            val stillVoiced = rms > max(noise * END_RATIO, MIN_SPEECH_RMS * 0.6f)
            if (stillVoiced) {
                voicedFrames++
                quietRun = 0
            } else {
                quietRun++
            }
            val tooLong = phrase.size >= MAX_PHRASE_SECONDS * 1000 / FRAME_MS
            if (quietRun < END_FRAMES && !tooLong) return null

            // Фраза кончилась: хвост тишины срезаем, оставляя немного на затухание
            val keep = phrase.size - max(0, quietRun - TAIL_KEEP_FRAMES)
            val result = if (voicedFrames >= MIN_VOICED_FRAMES) join(phrase.subList(0, keep)) else null
            reset()
            return result
        }

        private fun join(frames: List<FloatArray>): FloatArray {
            val out = FloatArray(frames.sumOf { it.size })
            var pos = 0
            for (f in frames) {
                f.copyInto(out, pos)
                pos += f.size
            }
            return out
        }
    }

    private companion object {
        const val RATE = 22050
        const val FRAME_MS = 20
        const val FRAME = RATE * FRAME_MS / 1000

        /** ~+8 полутонов: мультяшный голос, но слова ещё разборчивы. */
        const val PITCH = 1.6f

        const val START_RATIO = 4f          // речь громче фона во столько раз
        const val END_RATIO = 2.5f
        const val MIN_SPEECH_RMS = 0.012f   // тише — не речь, как бы тихо ни было вокруг
        const val START_FRAMES = 3          // 60 мс громко подряд — начало фразы
        const val END_FRAMES = 35           // 0.7 с тихо — конец фразы
        const val MIN_VOICED_FRAMES = 12    // короче 0.24 с — щелчок или стук, не фраза
        const val PRE_ROLL_FRAMES = 12      // 0.24 с до начала, чтобы не съесть первый звук
        const val TAIL_KEEP_FRAMES = 5
        const val MAX_PHRASE_SECONDS = 6

        const val ENVELOPE_RATE = 30        // значений огибающей рта в секунду
        const val TAIL_NANOS = 150_000_000L
    }
}

/** Обработка голоса питомца: чистые функции без Android, поэтому проверяются обычными юнит-тестами. */
internal object VoiceDsp {
    /**
     * Поднимает тон в [factor] раз, не меняя длительности: WSOLA растягивает запись в [factor] раз
     * (кусочками по 30 мс, каждый подбирается так, чтобы стык совпал по форме волны), затем
     * линейное пересэмплирование сжимает её обратно — тон растёт, длина прежняя.
     */
    fun pitchShift(x: FloatArray, factor: Float, rate: Int): FloatArray {
        val stretched = wsola(x, factor, rate)
        val outLength = (stretched.size / factor).toInt()
        return FloatArray(outLength) { i ->
            val pos = i * factor
            val k = pos.toInt()
            val t = pos - k
            val a = stretched[min(k, stretched.size - 1)]
            val b = stretched[min(k + 1, stretched.size - 1)]
            a + (b - a) * t
        }
    }

    fun wsola(x: FloatArray, stretch: Float, rate: Int): FloatArray {
        val n = rate * 30 / 1000                 // длина кусочка
        val hopOut = n / 2
        val hopIn = hopOut / stretch
        val tolerance = rate * 8 / 1000          // насколько можно сдвинуть кусочек ради гладкого стыка
        if (x.size < n * 2) return x
        val window = FloatArray(n) { (0.5 - 0.5 * cos(2 * PI * it / (n - 1))).toFloat() }
        val outLength = (x.size * stretch).toInt() + n
        val out = FloatArray(outLength)
        val weight = FloatArray(outLength)
        var outPos = 0
        var inPos = 0.0
        var previous = 0
        while (true) {
            val nominal = inPos.toInt()
            var best = nominal
            if (outPos > 0) {
                // Кусочек, который шёл бы дальше без разрыва, — его и ищем рядом с nominal
                val natural = previous + hopOut
                var bestScore = Float.NEGATIVE_INFINITY
                val from = max(0, nominal - tolerance)
                val to = min(x.size - n - 1, nominal + tolerance)
                var candidate = from
                while (candidate <= to) {
                    var score = 0f
                    var k = 0
                    while (k < hopOut && natural + k < x.size) {
                        score += x[candidate + k] * x[natural + k]
                        k += 2                            // каждый второй отсчёт: быстрее, разница не слышна
                    }
                    if (score > bestScore) {
                        bestScore = score
                        best = candidate
                    }
                    candidate++
                }
            }
            if (best + n >= x.size || outPos + n >= outLength) break
            for (k in 0 until n) {
                out[outPos + k] += x[best + k] * window[k]
                weight[outPos + k] += window[k]
            }
            previous = best
            outPos += hopOut
            inPos += hopIn
        }
        for (i in 0 until outPos) if (weight[i] > 1e-3f) out[i] /= weight[i]
        return out.copyOf(outPos)
    }

    /** Громкость к общему уровню: тихо сказанное тоже слышно, но не громче ×8 (иначе вылезает шум). */
    fun normalize(x: FloatArray, rate: Int): FloatArray {
        var peak = 1e-6f
        for (v in x) peak = max(peak, abs(v))
        val gain = min(0.9f / peak, 8f)
        // Короткое нарастание и затухание по краям — без щелчков
        val fade = min(rate / 100, x.size / 2)
        return FloatArray(x.size) { i ->
            val edge = min(1f, min(i, x.size - 1 - i).toFloat() / fade)
            x[i] * gain * edge
        }
    }

    /** Огибающая для рта: громкость по 1/30 с, растянутая на 0..1 и слегка сглаженная. */
    fun envelope(x: FloatArray, rate: Int, envelopeRate: Int): FloatArray {
        val step = rate / envelopeRate
        val raw = FloatArray((x.size + step - 1) / step) { i ->
            var sum = 0f
            val start = i * step
            val end = min(x.size, start + step)
            for (k in start until end) sum += x[k] * x[k]
            sqrt(sum / max(1, end - start))
        }
        val peak = raw.maxOrNull()?.takeIf { it > 0f } ?: return raw
        var smooth = 0f
        return FloatArray(raw.size) { i ->
            val level = ((raw[i] / peak - 0.12f) / 0.6f).coerceIn(0f, 1f)
            smooth = if (level > smooth) level else smooth * 0.6f + level * 0.4f   // быстро открыть, мягко закрыть
            smooth
        }
    }
}
