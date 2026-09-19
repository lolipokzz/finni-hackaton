package ru.larpinovplay.finniapp.domain.task

import ru.larpinovplay.finniapp.domain.task.model.Task
import ru.larpinovplay.finniapp.domain.task.model.TaskAnswer
import ru.larpinovplay.finniapp.domain.task.model.TaskOutcome
import ru.larpinovplay.finniapp.domain.task.model.TaskPayload

/** Оценка ответа по правилам из docs/04-rules-and-formulas.md. */
fun Task.evaluate(answer: TaskAnswer): TaskOutcome {
    val (success, consequence) = when (val p = payload) {
        is TaskPayload.Choice -> {
            val option = (answer as? TaskAnswer.Choice)?.let { a -> p.options.firstOrNull { it.id == a.optionId } }
            (option?.correct == true) to option?.consequence
        }
        is TaskPayload.Allocate -> {
            val a = (answer as? TaskAnswer.Allocation)?.amounts.orEmpty()
            val ok = a.values.sum() == p.total &&
                (a["mandatory"] ?: 0) >= p.mandatoryMin &&
                (a["savings"] ?: 0) >= p.savingsMin
            ok to null
        }
        is TaskPayload.ShopList -> {
            val ids = (answer as? TaskAnswer.Selection)?.itemIds.orEmpty()
            val chosen = p.items.filter { it.id in ids }
            val ok = p.items.filter { it.mandatory }.all { it.id in ids } && chosen.sumOf { it.price } <= p.budget
            ok to null
        }
    }
    return TaskOutcome(
        success = success,
        reward = if (success) reward else rewardOnMistake,
        consequence = consequence,
        explanation = if (success) explanationSuccess else explanationMistake,
    )
}
