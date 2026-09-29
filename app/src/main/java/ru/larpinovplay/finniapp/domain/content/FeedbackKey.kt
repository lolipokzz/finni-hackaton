package ru.larpinovplay.finniapp.domain.content

/**
 * Фразы для ребёнка, которых домен не пишет сам: он называет, что произошло, а текст берётся из контента. [id] — ключ в
 * файле, в шаблонах допустимы плейсхолдеры `{n}`, `{item}`…
 */
enum class FeedbackKey(val id: String) {
    // Причины движений в журнале монет
    LEDGER_START("ledger.start"),
    LEDGER_DEPOSIT("ledger.deposit"),
    LEDGER_PLANNED_DEPOSIT("ledger.planned_deposit"),
    LEDGER_WITHDRAW("ledger.withdraw"),
    LEDGER_SAVINGS_BONUS("ledger.savings_bonus"),
    LEDGER_GOAL("ledger.goal"),
    LEDGER_TASK("ledger.task"),
    LEDGER_ADVENTURE("ledger.adventure"),
    LEDGER_WEEK_INCOME("ledger.week_income"),
    LEDGER_DEMO_COINS("ledger.demo_coins"),

    // Итоги недели: что вышло с каждым делом
    PERIOD_FOOD_OK("period.a_ok"),
    PERIOD_FOOD_FAIL("period.a_fail"),
    PERIOD_SAVED_OK("period.c_ok"),
    PERIOD_SAVED_FAIL("period.c_fail"),
    PERIOD_BORED_OK("period.bored_ok"),
    PERIOD_BORED_FAIL("period.bored_fail"),
    PERIOD_PLAN_OK("period.b_ok"),
    PERIOD_PLAN_OPTIONAL_OVER("period.b_optional_over"),
    PERIOD_PLAN_SAVINGS_UNDER("period.b_savings_under"),
    PERIOD_MOOD("period.mood"),
    PERIOD_MOOD_LASTING("period.mood_lasting"),
    STAGE_UP("stage.up"),

    // Дела недели: что сделать (окно на главном экране)
    DEED_FED_TODO("deed.fed_todo"),
    DEED_NOT_BORED_TODO("deed.not_bored_todo"),
    DEED_SAVINGS_TODO("deed.savings_todo"),
    DEED_SPENDING_TODO("deed.spending_todo"),

    // Почему неделю пока нельзя закончить
    FINISH_NO_PLAN("finish.no_plan"),
    FINISH_NO_LEVELS("finish.no_levels"),
    SHOP_NO_PLAN("shop.no_plan"),
    FINISH_NO_ADVENTURE("finish.no_adventure"),
    FINISH_SAME_DAY("finish.same_day"),

    // Окно плана недели
    PLAN_NEED("plan.need"),
    PLAN_NEED_LOW("plan.need_low"),
    PLAN_UNALLOCATED("plan.unallocated"),
    PLAN_DONE("plan.done"),
    PLAN_SAVINGS_NOW("plan.savings_now"),

    // Окно «Забрать из копилки»
    WITHDRAW_GOAL_FURTHER("withdraw.goal_further"),
    WITHDRAW_WEEKS("withdraw.weeks"),
    WITHDRAW_NO_GOAL("withdraw.no_goal"),

    // Реплика Финни на главном экране: одно самое важное дело сейчас
    SAY_PLAN_WEEK("say.plan_week"),
    SAY_WEEK_READY("say.week_ready"),
    SAY_HUNGRY("say.hungry"),
    SAY_ADVENTURE("say.adventure"),
    SAY_CHOOSE_GOAL("say.choose_goal"),
    SAY_BORED("say.bored"),
    SAY_NEW_TASK("say.new_task"),
    SAY_TOMORROW("say.tomorrow"),
    SAY_DEEDS_LEFT("say.deeds_left"),
    SAY_ON_TRIP("say.on_trip"),

    DEPOSIT_REJECTED("deposit.rejected"),

    // Сбои сохранения (StorageError)
    STORAGE_READ_FAILED("storage.read_failed"),
    STORAGE_WRITE_FAILED("storage.write_failed"),
    STORAGE_CORRUPTED("storage.corrupted"),
    STORAGE_INCOMPATIBLE("storage.incompatible"),
    STORAGE_NO_SPACE("storage.no_space"),
    STORAGE_NO_ACCESS("storage.no_access"),

    // Приключение недели: разбор каждого шага
    ADVENTURE_PAY_EXACT("adventure.pay_exact"),
    ADVENTURE_PAY_CHANGE("adventure.pay_change"),
    ADVENTURE_PAY_NOT_ENOUGH("adventure.pay_not_enough"),
    ADVENTURE_PAY_EXTRA("adventure.pay_extra"),
    ADVENTURE_CHANGE_OK("adventure.change_ok"),
    ADVENTURE_CHANGE_WRONG("adventure.change_wrong"),
    ADVENTURE_CHANGE_NONE("adventure.change_none"),
    ADVENTURE_BASKET_FITS("adventure.basket_fits"),
    ADVENTURE_BASKET_OVER("adventure.basket_over"),
    ADVENTURE_BASKET_MISSING("adventure.basket_missing"),
    ADVENTURE_DONE_PERFECT("adventure.done_perfect"),
    ADVENTURE_DONE_MISTAKES("adventure.done_mistakes"),
    ADVENTURE_REPLAY("adventure.replay"),

    // Карта заданий: итог уровня и золотого испытания, почему уровень закрыт
    LEVEL_DONE_PERFECT("level.done_perfect"),
    LEVEL_DONE_MISTAKES("level.done_mistakes"),
    LEVEL_REPLAY("level.replay"),
    CHALLENGE_GOLD("challenge.gold"),
    CHALLENGE_MISTAKES("challenge.mistakes"),
    CHALLENGE_TIME_UP("challenge.time_up"),
    LEVEL_LOCKED_TOMORROW("level.locked_tomorrow"),
    LEVEL_LOCKED_NEXT_WEEK("level.locked_next_week"),
    LEVEL_LOCKED_LATER("level.locked_later"),
    LEVEL_LOCKED_ORDER("level.locked_order"),
    ADVENTURE_LOCKED_LEVELS("adventure.locked_levels"),

    // Темы заданий
    TOPIC_BUDGET("topic.budget"),
    TOPIC_SAVINGS("topic.savings"),
    TOPIC_PAYMENTS("topic.payments"),
}
