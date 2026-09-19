package ru.larpinovplay.finniapp.domain.content

/**
 * Фразы для ребёнка, которых домен не пишет сам: он называет, что произошло, а текст берётся из контента
 * (docs/05-content-model.md, feedback.json). [id] — ключ в файле, в шаблонах допустимы плейсхолдеры `{n}`, `{item}`…
 */
enum class FeedbackKey(val id: String) {
    // Причины движений в журнале монет
    LEDGER_START("ledger.start"),
    LEDGER_DEPOSIT("ledger.deposit"),
    LEDGER_GOAL("ledger.goal"),
    LEDGER_TASK("ledger.task"),
    LEDGER_WEEK_INCOME("ledger.week_income"),

    // Объяснение итога недели
    PERIOD_FOOD_OK("period.a_ok"),
    PERIOD_FOOD_FAIL("period.a_fail"),
    PERIOD_SAVED_OK("period.c_ok"),
    PERIOD_SAVED_FAIL("period.c_fail"),
    STAGE_UP("stage.up"),

    // Фраза под питомцем и подсказка в облачке
    MOOD_HUNGRY("mood.hungry"),
    MOOD_GREW("mood.grew"),
    MOOD_PURCHASE("mood.purchase"),
    MOOD_DEFAULT("mood.default"),
    TIP_CHOOSE_GOAL("tip.choose_goal"),
    TIP_SAVE_FOR("tip.save_for"),

    DEPOSIT_REJECTED("deposit.rejected"),

    // Темы заданий
    TOPIC_BUDGET("topic.budget"),
    TOPIC_SAVINGS("topic.savings"),
    TOPIC_PAYMENTS("topic.payments"),
}
