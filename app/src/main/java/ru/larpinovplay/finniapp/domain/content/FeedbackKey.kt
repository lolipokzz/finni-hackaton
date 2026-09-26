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
    PERIOD_PLAN_OK("period.b_ok"),
    PERIOD_PLAN_OPTIONAL_OVER("period.b_optional_over"),
    PERIOD_PLAN_SAVINGS_UNDER("period.b_savings_under"),
    STAGE_UP("stage.up"),

    // Почему неделю пока нельзя закончить
    FINISH_NO_PLAN("finish.no_plan"),
    FINISH_SAME_DAY("finish.same_day"),

    // Окно плана недели
    PLAN_NEED("plan.need"),
    PLAN_NEED_LOW("plan.need_low"),
    PLAN_UNALLOCATED("plan.unallocated"),
    PLAN_DONE("plan.done"),

    // Фраза под питомцем и подсказка в облачке
    MOOD_HUNGRY("mood.hungry"),
    MOOD_GREW("mood.grew"),
    MOOD_PURCHASE("mood.purchase"),
    MOOD_DEFAULT("mood.default"),
    TIP_CHOOSE_GOAL("tip.choose_goal"),
    TIP_SAVE_FOR("tip.save_for"),

    DEPOSIT_REJECTED("deposit.rejected"),

    // Сбои сохранения (StorageError)
    STORAGE_READ_FAILED("storage.read_failed"),
    STORAGE_WRITE_FAILED("storage.write_failed"),
    STORAGE_CORRUPTED("storage.corrupted"),
    STORAGE_INCOMPATIBLE("storage.incompatible"),

    // Темы заданий
    TOPIC_BUDGET("topic.budget"),
    TOPIC_SAVINGS("topic.savings"),
    TOPIC_PAYMENTS("topic.payments"),
}
