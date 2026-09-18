package ru.larpinovplay.finniapp.domain.task.model

/** Три типа механик заданий (docs/05-content-model.md): выбор действия, раскладка суммы, список покупок. */
sealed interface TaskPayload {
    data class Choice(val options: List<Option>) : TaskPayload {
        data class Option(val id: String, val text: String, val correct: Boolean, val consequence: String)
    }

    data class Allocate(
        val total: Int,
        val step: Int,
        val buckets: List<Bucket>,
        val mandatoryMin: Int,
        val savingsMin: Int,
    ) : TaskPayload {
        data class Bucket(val id: String, val label: String)
    }

    data class ShopList(val budget: Int, val items: List<Item>) : TaskPayload {
        data class Item(val id: String, val name: String, val price: Int, val mandatory: Boolean)
    }
}
