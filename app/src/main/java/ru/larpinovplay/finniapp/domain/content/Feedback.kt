package ru.larpinovplay.finniapp.domain.content

/**
 * Шаблоны фраз для ребёнка. Полнота проверяется сразу: если для какого-то [FeedbackKey] нет текста,
 * это ошибка контента, а не пустая строка на экране.
 */
class Feedback(templates: Map<FeedbackKey, String>) {

    private val templates: Map<FeedbackKey, String> = templates.toMap()

    init {
        val missing = FeedbackKey.entries - this.templates.keys
        require(missing.isEmpty()) { "Нет текстов для ключей: ${missing.joinToString { it.id }}" }
    }

    /** Текст по [key], где `{имя}` заменено значением из [args]. */
    fun text(key: FeedbackKey, vararg args: Pair<String, Any>): String =
        args.fold(templates.getValue(key)) { text, (name, value) -> text.replace("{$name}", value.toString()) }
}
