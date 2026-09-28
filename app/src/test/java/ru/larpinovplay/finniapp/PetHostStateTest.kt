package ru.larpinovplay.finniapp

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.larpinovplay.finniapp.presentation.components.PetHostState

/** Касание над элементом под питомцем достаётся элементу; из вложенных — тому, в который целились. */
class PetHostStateTest {

    @Test
    fun smallestShieldUnderFingerWins() {
        val host = PetHostState()
        val taps = mutableListOf<String>()
        host.setShield("note", Rect(0f, 0f, 100f, 100f)) { taps += "note" }
        host.setShield("skip", Rect(60f, 80f, 100f, 100f)) { taps += "skip" }

        host.shieldAt(70f, 90f)?.invoke()   // по кнопке «Пропустить шаг»
        host.shieldAt(10f, 10f)?.invoke()   // по тексту подсказки
        assertEquals(listOf("skip", "note"), taps)

        assertNull(host.shieldAt(200f, 200f))   // мимо — это питомец
        host.removeShield("skip")
        host.shieldAt(70f, 90f)?.invoke()
        assertEquals(listOf("skip", "note", "note"), taps)
    }
}
