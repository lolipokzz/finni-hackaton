package ru.larpinovplay.finniapp.data.content

import kotlinx.serialization.json.Json
import ru.larpinovplay.finniapp.domain.content.Feedback
import ru.larpinovplay.finniapp.domain.content.FeedbackKey

/** Где лежит файл с фразами обратной связи (docs/05-content-model.md). */
const val FEEDBACK_ASSET = "content/feedback.json"

/** Разбирает feedback.json: плоский объект «ключ → шаблон». Неизвестный или пропущенный ключ — ошибка контента. */
fun parseFeedback(json: String): Feedback {
    val raw = Json.decodeFromString<Map<String, String>>(json)
    val byId = FeedbackKey.entries.associateBy { it.id }
    val unknown = raw.keys - byId.keys
    require(unknown.isEmpty()) { "Неизвестные ключи в $FEEDBACK_ASSET: ${unknown.joinToString()}" }
    return Feedback(raw.entries.associate { (id, template) -> byId.getValue(id) to template })
}
