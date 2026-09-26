package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.adventure.model.Adventure
import ru.larpinovplay.finniapp.domain.adventure.model.AdventureScene

/**
 * Приключения недели по порядку: первое — на первой неделе, следующее — после него.
 * Пока список в коде; как и задания, переедет в assets/content (docs/05-content-model.md).
 */
internal val defaultAdventures: List<Adventure> = listOf(
    Adventure(
        id = "first_shop",
        title = "Первый поход в магазин",
        intro = "Финни впервые идёт в магазин сам, а ты ему помогаешь: нужно заплатить и проверить сдачу.",
        scenes = listOf(
            AdventureScene.Story(
                "Дома закончилась морковка, и Финни проголодался. Мама дала ему кошелёк: " +
                    "одна купюра 50, две купюры по 10 и монеты 5, 2, 2 и 1. Эти деньги — только на покупку, " +
                    "в твою копилку они не попадут.",
            ),
            AdventureScene.Pay(
                text = "Морковка стоит 17. Перетащи на прилавок столько денег, чтобы хватило, но без лишних монет.",
                price = 17,
                wallet = listOf(50, 10, 10, 5, 2, 2, 1),
                hint = "Можно без сдачи: сложи 10, 5 и 2. А можно дать больше, и продавец вернёт сдачу.",
            ),
            AdventureScene.Change(
                text = "Продавец взял деньги и открывает кассу. Сколько сдачи он должен вернуть?",
                price = 17,
                hint = "Считай от 17 вверх до того, сколько ты дал.",
            ),
            AdventureScene.Story(
                "Финни несёт морковку домой и хрустит по дороге. Первая покупка сделана! " +
                    "Запомни: сдачу считают сразу у кассы.",
            ),
        ),
        reward = 5,
        rewardOnMistake = 3,
    ),
    Adventure(
        id = "park_walk",
        title = "Прогулка в парке",
        intro = "В парке столько всего интересного! Но монет с собой немного — придётся решить, на что их хватит.",
        scenes = listOf(
            AdventureScene.Story(
                "Финни пришёл в парк, у него 30 монет. Здесь продают мороженое за 12, катают на карусели за 15, " +
                    "есть воздушный шарик за 8 и сахарная вата за 10.",
            ),
            AdventureScene.Choice(
                text = "Финни хочет мороженое, карусель и шарик. Хватит ли 30 монет на всё сразу?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "park_enough_yes", "Хватит", correct = false,
                        explanation = "Посчитаем: 12 + 15 + 8 = 35. Это на 5 больше, чем 30, — на всё не хватит.",
                    ),
                    AdventureScene.Choice.Option(
                        "park_enough_no", "Не хватит", correct = true,
                        explanation = "Верно: 12 + 15 + 8 = 35, а у Финни 30. Значит, придётся выбирать.",
                    ),
                ),
                hint = "Сложи три цены и сравни с 30.",
            ),
            AdventureScene.Basket(
                text = "Больше всего Финни хочет на карусель. Собери покупки так, чтобы карусель была и хватило 30 монет.",
                budget = 30,
                items = listOf(
                    AdventureScene.Basket.Item("Карусель", 15, required = true),
                    AdventureScene.Basket.Item("Мороженое", 12),
                    AdventureScene.Basket.Item("Воздушный шарик", 8),
                    AdventureScene.Basket.Item("Сахарная вата", 10),
                ),
                hint = "Сначала главное — карусель за 15. Останется 15: что из остального в них влезет?",
            ),
            AdventureScene.Story(
                "Финни катается на карусели и машет тебе лапой. Когда на всё не хватает, " +
                    "сначала выбирают самое важное, а остальное — если останется.",
            ),
        ),
        reward = 5,
        rewardOnMistake = 3,
    ),
    Adventure(
        id = "scratched_paw",
        title = "Лапка",
        intro = "Иногда случается то, чего не ждёшь. Посмотрим, пригодится ли Финни запас.",
        scenes = listOf(
            AdventureScene.Story("Финни снова гуляет, у него 20 монет. У фонтана продают сладости и игрушки."),
            AdventureScene.Choice(
                text = "Как Финни поступить с 20 монетами?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "paw_spend_all", "Потратить все 20 на сладости и игрушку", correct = false,
                        explanation = "Потратить всё можно, но тогда на неожиданное ничего не останется.",
                    ),
                    AdventureScene.Choice.Option(
                        "paw_keep_some", "Потратить 15, а 5 оставить про запас", correct = true,
                        explanation = "Запас — это монеты на случай, если что-то пойдёт не так. И радость есть, и запас.",
                    ),
                    AdventureScene.Choice.Option(
                        "paw_keep_all", "Ничего не тратить, оставить все 20", correct = true,
                        explanation = "Тоже можно: запас большой. Правда, прогулка вышла без сладостей.",
                    ),
                ),
                hint = "Подумай, что будет, если вдруг понадобятся деньги.",
            ),
            AdventureScene.Story("Ой! Финни споткнулся у фонтана и поцарапал лапку. В аптеке пластырь стоит 5 монет."),
            AdventureScene.Story(
                text = "Монет не осталось. Пришлось идти домой и просить пластырь у мамы. " +
                    "Лапку заклеили, но в следующий раз запас пригодится.",
                variants = mapOf(
                    "paw_keep_some" to "Хорошо, что ты оставил 5 монет! Их как раз хватило на пластырь — лапка заклеена.",
                    "paw_keep_all" to "Монеты остались, поэтому пластырь купили сразу. Лапка заклеена, и ещё 15 осталось.",
                ),
            ),
            AdventureScene.Choice(
                text = "Как ты думаешь, зачем нужен запас?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "paw_why_surprise", "Чтобы хватило на неожиданное", correct = true,
                        explanation = "Да! Запас выручает, когда случается то, чего не ждали.",
                    ),
                    AdventureScene.Choice.Option(
                        "paw_why_more", "Чтобы сразу купить побольше", correct = false,
                        explanation = "Нет: запас как раз не тратят сразу. Он лежит на случай, если понадобится.",
                    ),
                    AdventureScene.Choice.Option(
                        "paw_why_never", "Запас вообще не нужен", correct = false,
                        explanation = "Лапка показала, что нужен: без запаса пластырь купить было не на что.",
                    ),
                ),
                hint = "Вспомни, что случилось с лапкой.",
            ),
            AdventureScene.Story(
                "Запас — как маленькая копилка на случай беды. В игре он лежит в твоей копилке: " +
                    "если очень нужно, оттуда можно взять.",
            ),
        ),
        reward = 5,
        rewardOnMistake = 3,
    ),
    Adventure(
        id = "fair",
        title = "Ярмарка",
        intro = "На ярмарке шумно и весело. Здесь легко потратить лишнее, поэтому будь внимателен к ценам и сдаче.",
        scenes = listOf(
            AdventureScene.Story(
                "Финни пришёл на ярмарку. В кошельке у него купюры 50, 10 и 10 и монеты 5, 2 и 1. " +
                    "Эти деньги — только на ярмарку, в твою копилку они не попадут.",
            ),
            AdventureScene.Choice(
                text = "Яблоки продают по 8 монет за штуку или набором: 3 яблока за 20. Финни нужно 3 яблока. Как дешевле?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "fair_single", "По одному", correct = false,
                        explanation = "По одному выйдет 8 + 8 + 8 = 24. Это на 4 дороже набора.",
                    ),
                    AdventureScene.Choice.Option(
                        "fair_bundle", "Набором за 20", correct = true,
                        explanation = "Верно: по одному 8 + 8 + 8 = 24, а набор — 20. Набором дешевле на 4.",
                    ),
                    AdventureScene.Choice.Option(
                        "fair_same", "Одинаково", correct = false,
                        explanation = "Не совсем: по одному 8 + 8 + 8 = 24, а набор стоит 20.",
                    ),
                ),
                hint = "Посчитай, сколько стоят 3 яблока по 8.",
            ),
            AdventureScene.Choice(
                text = "Продавец кричит: «Скидка только сегодня! Шарф за 30 вместо 35!» Но шарф Финни не нужен. Что сделать?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "fair_sale_buy", "Купить, пока скидка", correct = false,
                        explanation = "Скидка сбережёт 5, но потратится 30 на то, что не нужно. Выгодно только нужное.",
                    ),
                    AdventureScene.Choice.Option(
                        "fair_sale_skip", "Не покупать: шарф не нужен", correct = true,
                        explanation = "Верно! Скидка — не причина покупать то, что не нужно. А «только сегодня» часто бывает и завтра.",
                    ),
                    AdventureScene.Choice.Option(
                        "fair_sale_two", "Купить два, раз дёшево", correct = false,
                        explanation = "Два ненужных шарфа — это 60 монет на то, что не пригодится.",
                    ),
                ),
                hint = "Нужен ли Финни шарф вообще?",
            ),
            AdventureScene.Pay(
                text = "Финни берёт набор яблок за 20 и мёд за 6 — всего 26. Перетащи деньги на прилавок.",
                price = 26,
                wallet = listOf(50, 10, 10, 5, 2, 1),
                hint = "Без сдачи: 10 + 10 + 5 + 1. Или дай больше, и продавец вернёт сдачу.",
            ),
            AdventureScene.Change(
                text = "Сколько сдачи должен вернуть продавец?",
                price = 26,
                hint = "Считай от 26 вверх до того, сколько ты дал.",
            ),
            AdventureScene.Choice(
                text = "У соседнего прилавка девочка дала 50 за покупку за 26, а продавец вернул ей 14. Всё верно?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "fair_change_ok", "Верно", correct = false,
                        explanation = "Посчитаем: 50 − 26 = 24, а вернули 14. Продавец ошибся на 10 — сдачу надо проверять.",
                    ),
                    AdventureScene.Choice.Option(
                        "fair_change_wrong", "Неверно: должно быть 24", correct = true,
                        explanation = "Верно! 50 − 26 = 24. Девочка вежливо сказала, продавец извинился и вернул ещё 10 — он просто ошибся.",
                    ),
                ),
                hint = "Сколько будет 50 − 26?",
            ),
            AdventureScene.Story(
                "Финни несёт яблоки и мёд. Сегодня он узнал три вещи: цены сравнивают, " +
                    "из-за скидки не спешат, а сдачу проверяют сразу.",
            ),
        ),
        reward = 5,
        rewardOnMistake = 3,
    ),
    Adventure(
        id = "friend_gift",
        title = "Подарок другу",
        intro = "Большие покупки не делаются за один день. Поможем Финни накопить на подарок.",
        scenes = listOf(
            AdventureScene.Story("У друга Финни скоро день рождения. Финни хочет подарить ему конструктор за 40 монет."),
            AdventureScene.Choice(
                text = "Если откладывать по 10 монет в неделю, через сколько недель хватит на конструктор?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "gift_weeks_2", "Через 2 недели", correct = false,
                        explanation = "За 2 недели будет 10 + 10 = 20. До 40 ещё далеко.",
                    ),
                    AdventureScene.Choice.Option(
                        "gift_weeks_4", "Через 4 недели", correct = true,
                        explanation = "Верно: 10 + 10 + 10 + 10 = 40. Четыре недели.",
                    ),
                    AdventureScene.Choice.Option(
                        "gift_weeks_10", "Через 10 недель", correct = false,
                        explanation = "За 10 недель накопится 100 — намного больше, чем нужно.",
                    ),
                ),
                hint = "Складывай по 10, пока не получится 40.",
            ),
            AdventureScene.Choice(
                text = "Оказалось, день рождения уже через 2 недели! Сколько откладывать каждую неделю, чтобы успеть?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "gift_rate_10", "По 10", correct = false,
                        explanation = "По 10 за 2 недели будет только 20, а нужно 40.",
                    ),
                    AdventureScene.Choice.Option(
                        "gift_rate_20", "По 20", correct = true,
                        explanation = "Верно: 20 + 20 = 40. Как раз успеешь.",
                    ),
                    AdventureScene.Choice.Option(
                        "gift_rate_40", "По 40", correct = false,
                        explanation = "Так тоже хватит, но 80 — вдвое больше, чем нужно. Достаточно по 20.",
                    ),
                ),
                hint = "40 нужно разделить на 2 недели.",
            ),
            AdventureScene.Choice(
                text = "Карманных у Финни 50 в неделю. Откуда взять 20 на подарок?",
                options = listOf(
                    AdventureScene.Choice.Option(
                        "gift_from_optional", "Меньше тратить на необязательное", correct = true,
                        explanation = "Да! Сладости и игрушки могут подождать, а подарок другу — нет.",
                    ),
                    AdventureScene.Choice.Option(
                        "gift_from_food", "Не покупать еду", correct = false,
                        explanation = "Еда — обязательное: без неё Финни будет голодным. Лучше отложить необязательное.",
                    ),
                    AdventureScene.Choice.Option(
                        "gift_from_nothing", "Ничего не менять, как-нибудь накопится", correct = false,
                        explanation = "Само не накопится: если тратить как раньше, через 2 недели денег на подарок не будет.",
                    ),
                ),
                hint = "Что в неделе можно отложить на потом?",
            ),
            AdventureScene.Story(
                "Финни откладывает по 20 и через 2 недели дарит другу конструктор. " +
                    "Большие покупки получаются, когда копишь заранее и по плану.",
            ),
        ),
        reward = 5,
        rewardOnMistake = 3,
    ),
)
