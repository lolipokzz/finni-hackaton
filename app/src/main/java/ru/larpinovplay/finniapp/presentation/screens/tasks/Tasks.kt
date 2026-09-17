package ru.larpinovplay.finniapp.presentation.screens.tasks

/**
 * Финансовые задания, ТЗ 2.5.8: игровая ситуация с выбором и последствиями по трём темам.
 * Три типа механик (docs/05-content-model.md): выбор действия, раскладка суммы, список покупок.
 * Пока список в коде; по документации переедет в assets/content/tasks.json.
 */
enum class TaskTopic(val title: String) {
    BUDGET("Планирую"),
    SAVINGS("Коплю"),
    PAYMENTS("Покупаю"),
}

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

sealed interface TaskAnswer {
    data class Choice(val optionId: String) : TaskAnswer
    data class Allocation(val amounts: Map<String, Int>) : TaskAnswer
    data class Selection(val itemIds: Set<String>) : TaskAnswer
}

data class Task(
    val id: String,
    val topic: TaskTopic,
    val title: String,
    val intro: String,
    val reward: Int,
    val rewardOnMistake: Int,
    val explanationSuccess: String,
    val explanationMistake: String,
    val hint: String,
    val payload: TaskPayload,
)

/** Итог ответа: верно ли, сколько монет, что сказать ребёнку. */
data class TaskOutcome(val success: Boolean, val reward: Int, val consequence: String?, val explanation: String)

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

val tasks: List<Task> = listOf(
    Task(
        id = "budget_split_60",
        topic = TaskTopic.BUDGET,
        title = "Раздели 60 монет",
        intro = "У Финни 60 монет на неделю. Еда стоит 20, уход 10. Разложи монеты по кучкам.",
        reward = 20, rewardOnMistake = 7,
        explanationSuccess = "Ты сначала закрыл нужное и что-то отложил. Так делают те, кто умеет планировать.",
        explanationMistake = "На нужное не хватило: еда и уход стоят 30 вместе. Сначала нужное, потом остальное, и хоть немного в копилку.",
        hint = "Подумай: без чего Финни точно нельзя?",
        payload = TaskPayload.Allocate(
            total = 60, step = 5,
            buckets = listOf(
                TaskPayload.Allocate.Bucket("mandatory", "Нужное"),
                TaskPayload.Allocate.Bucket("optional", "Желаемое"),
                TaskPayload.Allocate.Bucket("savings", "Копилка"),
            ),
            mandatoryMin = 30, savingsMin = 5,
        ),
    ),
    Task(
        id = "budget_first_what",
        topic = TaskTopic.BUDGET,
        title = "Что купить первым?",
        intro = "У тебя 30 монет. Финни хочет шляпу за 30, а еда стоит 15. Что сделаешь?",
        reward = 15, rewardOnMistake = 5,
        explanationSuccess = "Нужное — первым. Шляпа никуда не денется, а голодный Финни грустит.",
        explanationMistake = "Шляпа красивая, но Финни остался без еды. На желаемое тратят то, что осталось после нужного.",
        hint = "Что случится с Финни без еды на неделе?",
        payload = TaskPayload.Choice(
            listOf(
                TaskPayload.Choice.Option("hat", "Купить шляпу, еда подождёт", false, "Шляпа на Финни, но живот пустой. Настроение падает."),
                TaskPayload.Choice.Option("food", "Купить еду, а на шляпу копить", true, "Финни сыт, а на шляпу ты накопишь за две недели."),
                TaskPayload.Choice.Option("nothing", "Ничего не покупать, всё отложить", false, "Копилка выросла, но Финни голоден. Нужное нельзя пропускать."),
            )
        ),
    ),
    Task(
        id = "savings_weeks",
        topic = TaskTopic.SAVINGS,
        title = "Когда накопим?",
        intro = "Финни копит на самокат за 60 монет и откладывает по 20 каждую неделю. Через сколько недель хватит?",
        reward = 15, rewardOnMistake = 5,
        explanationSuccess = "60 разделить на 20 — это 3. Три недели по 20 монет, и самокат твой.",
        explanationMistake = "Считаем: 20 + 20 + 20 = 60. Значит, нужно 3 недели. Когда знаешь срок, копить легче.",
        hint = "Складывай по 20, пока не получится 60.",
        payload = TaskPayload.Choice(
            listOf(
                TaskPayload.Choice.Option("w2", "Через 2 недели", false, "20 + 20 = 40. Ещё не хватает 20."),
                TaskPayload.Choice.Option("w3", "Через 3 недели", true, "20 + 20 + 20 = 60. Ровно на самокат!"),
                TaskPayload.Choice.Option("w6", "Через 6 недель", false, "За 6 недель было бы 120 — в два раза больше, чем нужно."),
            )
        ),
    ),
    Task(
        id = "savings_take_or_not",
        topic = TaskTopic.SAVINGS,
        title = "Взять из копилки?",
        intro = "В копилке 40 монет на велосипед за 120. В магазине появились чипсы за 15. Что сделаешь?",
        reward = 15, rewardOnMistake = 5,
        explanationSuccess = "Копилка — для цели. Чипсы можно купить с монет на неделю или подождать.",
        explanationMistake = "Если брать из копилки на мелочи, до велосипеда не добраться. Копилку трогают только в крайнем случае.",
        hint = "Зачем ты откладывал эти 40 монет?",
        payload = TaskPayload.Choice(
            listOf(
                TaskPayload.Choice.Option("take", "Взять 15 из копилки на чипсы", false, "В копилке осталось 25. До велосипеда стало дальше на неделю."),
                TaskPayload.Choice.Option("wait", "Оставить копилку, чипсы подождут", true, "Копилка целая. Чипсы купишь с монет на следующей неделе, если захочешь."),
                TaskPayload.Choice.Option("all", "Забрать всё и купить много чипсов", false, "Чипсы съедены за день, а копилка пустая. Велосипед снова далеко."),
            )
        ),
    ),
    Task(
        id = "payments_shop_50",
        topic = TaskTopic.PAYMENTS,
        title = "Покупки на 50 монет",
        intro = "У тебя 50 монет. Собери покупки на неделю. Нужное обязательно, остальное — если хватит.",
        reward = 20, rewardOnMistake = 7,
        explanationSuccess = "Нужное куплено, в бюджет уложился. Так и планируют покупки.",
        explanationMistake = "Проверь: куплено ли всё нужное и не больше ли сумма, чем 50 монет? Сначала нужное, потом считаем остаток.",
        hint = "Сложи цены нужного, потом смотри, что ещё влезает.",
        payload = TaskPayload.ShopList(
            budget = 50,
            items = listOf(
                TaskPayload.ShopList.Item("food", "Корм", 15, mandatory = true),
                TaskPayload.ShopList.Item("soap", "Мыло", 10, mandatory = true),
                TaskPayload.ShopList.Item("ball", "Мячик", 20, mandatory = false),
                TaskPayload.ShopList.Item("hat", "Шляпа", 30, mandatory = false),
                TaskPayload.ShopList.Item("stickers", "Наклейки", 5, mandatory = false),
            )
        ),
    ),
)
