package ru.larpinovplay.finniapp.domain.adventure.model

/**
 * Приключение недели: короткий сюжет из сцен (справочник контента, только чтение).
 * Приключения идут по порядку списка, одно за неделю. Числа внутри сцен свои и реальный баланс не трогают:
 * в игру уходит только награда — [reward], если прошёл без ошибок, иначе [rewardOnMistake].
 */
data class Adventure(
    val id: String,
    val title: String,
    val intro: String,
    val scenes: List<AdventureScene>,
    val reward: Int,
    val rewardOnMistake: Int,
)

/** Шаг приключения. Новый вид шага — новый класс здесь и виджет на экране приключения. */
sealed interface AdventureScene {

    /**
     * Кусочек сюжета: текст и «Дальше». Если ребёнок раньше выбрал вариант из [variants] (ключ — id варианта
     * в сцене [Choice]), показывается его текст: так сюжет продолжает решение ребёнка («запас выручил»).
     */
    data class Story(val text: String, val variants: Map<String, String> = emptyMap()) : AdventureScene {
        /** Текст с учётом уже выбранных вариантов [chosen]. */
        fun textAfter(chosen: Set<String>): String = variants.entries.firstOrNull { it.key in chosen }?.value ?: text
    }

    /**
     * Заплатить за товар ценой [price] деньгами из кошелька: [wallet] — номиналы купюр и монет, повторы допустимы.
     * Верно — хватает и нет лишней купюры или монеты (см. [checkPayment][ru.larpinovplay.finniapp.domain.adventure.checkPayment]).
     */
    data class Pay(val text: String, val price: Int, val wallet: List<Int>, val hint: String) : AdventureScene

    /** Сколько сдачи должен вернуть продавец за покупку ценой [price]. Сколько дали — берётся из прошлой сцены [Pay]. */
    data class Change(val text: String, val price: Int, val hint: String) : AdventureScene

    /**
     * Вопрос с вариантами. Верных может быть несколько; у каждого варианта своё объяснение, его показывают после
     * ответа, верного или нет. Неверный ответ не останавливает сюжет. id вариантов уникальны во всём приключении:
     * на них ссылается [Story.variants].
     */
    data class Choice(val text: String, val options: List<Option>, val hint: String) : AdventureScene {
        data class Option(val id: String, val text: String, val correct: Boolean, val explanation: String)
    }

    /**
     * Собрать покупки на [budget]: уложиться в бюджет и не забыть обязательное ([Item.required]).
     * Проверка — [checkBasket][ru.larpinovplay.finniapp.domain.adventure.checkBasket].
     */
    data class Basket(val text: String, val budget: Int, val items: List<Item>, val hint: String) : AdventureScene {
        data class Item(val name: String, val price: Int, val required: Boolean = false)
    }
}
