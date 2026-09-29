package ru.larpinovplay.finniapp.domain.game.model

/** Фаза недели: сначала план, потом покупки и копилка, потом итоги. */
enum class PeriodPhase {
    /** Неделя началась, план ещё не подтверждён. */
    PLANNING,

    /** План подтверждён; неделю можно закрыть, когда позволит [FinishBlock]. */
    ACTIVE,
}
