package ru.larpinovplay.finniapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.larpinovplay.finniapp.data.content.FEEDBACK_ASSET
import ru.larpinovplay.finniapp.data.content.parseFeedback
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.content.FeedbackKey
import java.io.File

class FeedbackContentTest {

    private val shipped = File("src/main/assets/$FEEDBACK_ASSET").readText()

    @Test
    fun shippedFileHasTextForEveryKey() {
        val feedback = parseFeedback(shipped)   // упадёт, если ключа не хватает или он лишний

        FeedbackKey.entries.forEach { key ->
            assertTrue("Пустой текст для ${key.id}", feedback.text(key).isNotBlank())
        }
    }

    @Test
    fun placeholdersAreSubstituted() {
        val feedback = Feedback(FeedbackKey.entries.associateWith { "{n} из {total}" })

        assertEquals("3 из 5", feedback.text(FeedbackKey.PERIOD_SAVED_OK, "n" to 3, "total" to 5))
    }

    @Test
    fun missingKeyIsRejected() {
        expectFailure { Feedback(FeedbackKey.entries.drop(1).associateWith { "текст" }) }
    }

    @Test
    fun unknownKeyInJsonIsRejected() {
        expectFailure { parseFeedback("""{"no.such.key": "текст"}""") }
    }

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("Ожидалась ошибка контента")
        } catch (_: IllegalArgumentException) {
        }
    }
}
