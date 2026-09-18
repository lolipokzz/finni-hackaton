# 05 — Модель учебного контента

Требование **ТЗ 2.5.14 и 3.2**: контент отделён от кода, новое задание добавляется без переработки
логики. Критерий **8.2**: «разделение учебного контента от логики интерфейса».

## Где лежит

```
app/src/main/assets/content/
  config.json          — экономика (см. 04)
  pets.json            — виды, окрасы, аксессуары
  items.json           — товары магазина
  goals.json           — цели накопления
  tasks.json           — задания
  events.json          — события периода (опционально)
  names.json           — случайные игровые имена
  glossary.json        — справочник терминов
  feedback.json        — шаблоны фраз обратной связи
  demo-profile.json    — стартовое состояние демо-режима
```

Загружает всё `ContentRepository` в `:app`, парсит `kotlinx.serialization`, валидирует
`ContentValidator` из `:domain` (при запуске в debug-сборке и в unit-тесте `ContentValidationTest`).
Тест падает, если контент нарушает минимумы ТЗ 2.6 или ссылается на несуществующие id.

Все тексты для ребёнка — в контенте, не в `strings.xml` и не в коде. В `strings.xml` только
названия кнопок и системные подписи.

## Питомцы — `pets.json`

```json
{
  "species": [
    { "id": "cat",    "name": "Котик",    "asset": "pet_cat" },
    { "id": "bunny",  "name": "Зайчик",   "asset": "pet_bunny" },
    { "id": "dragon", "name": "Дракончик","asset": "pet_dragon" }
  ],
  "colors": [
    { "id": "orange", "name": "Рыжий",   "tint": "#F2A65A" },
    { "id": "blue",   "name": "Голубой", "tint": "#7FB3E6" },
    { "id": "green",  "name": "Зелёный", "tint": "#8CCB8C" }
  ],
  "accessories": [
    { "id": "bow",  "name": "Бантик", "asset": "acc_bow" },
    { "id": "hat",  "name": "Шляпа",  "asset": "acc_hat" },
    { "id": "scarf","name": "Шарф",   "asset": "acc_scarf" }
  ]
}
```

Комбинаций 3 × 3 = 9 (**ТЗ 2.6**). Картинки: один силуэт на вид, окрас через `tint`, по 3 выражения
(радостный, спокойный, грустный) и по 3 стадии → `pet_cat_baby_happy` и т.д. Итого 27 изображений
на вид или векторный подход с заменой слоёв. Решение за Android-разработчиком, контент от этого не зависит.

## Товары — `items.json`

```json
[
  { "id": "food_basic",  "name": "Корм",          "price": 15, "category": "MANDATORY", "tags": ["food"],
    "effects": { "satiety": 40, "mood": 5 },  "asset": "item_food",
    "childHint": "Финни нужно есть каждую неделю" },
  { "id": "food_tasty",  "name": "Вкусный корм",  "price": 25, "category": "MANDATORY", "tags": ["food"],
    "effects": { "satiety": 50, "mood": 15 }, "asset": "item_food_tasty",
    "childHint": "Сытнее и вкуснее, но дороже" },
  { "id": "care_soap",   "name": "Мыло и щётка",  "price": 10, "category": "MANDATORY", "tags": ["care"],
    "effects": { "care": 40 },                "asset": "item_soap",
    "childHint": "Чтобы Финни был чистым" },
  { "id": "care_vitamins","name": "Витамины",     "price": 12, "category": "MANDATORY", "tags": ["care"],
    "effects": { "care": 30, "satiety": 10 }, "asset": "item_vitamins",
    "childHint": "Полезно для здоровья" },
  { "id": "toy_ball",    "name": "Мячик",         "price": 20, "category": "OPTIONAL", "tags": ["toy"],
    "effects": { "mood": 15 },                "asset": "item_ball",
    "childHint": "Финни любит играть" },
  { "id": "toy_stickers","name": "Наклейки",      "price": 5,  "category": "OPTIONAL", "tags": ["toy"],
    "effects": { "mood": 5 },                 "asset": "item_stickers",
    "childHint": "Маленькая радость" },
  { "id": "deco_bow",    "name": "Бантик",        "price": 10, "category": "OPTIONAL", "tags": ["deco"],
    "effects": { "mood": 10 },                "asset": "item_bow",
    "childHint": "Для красоты" },
  { "id": "deco_hat",    "name": "Шляпа",         "price": 30, "category": "OPTIONAL", "tags": ["deco"],
    "effects": { "mood": 20 },                "asset": "item_hat",
    "childHint": "Финни будет модным" },
  { "id": "toy_house",   "name": "Домик",         "price": 40, "category": "OPTIONAL", "tags": ["toy"],
    "effects": { "mood": 25, "care": 5 },     "asset": "item_house",
    "childHint": "Большая радость, большая цена" },
  { "id": "med_pills",   "name": "Лекарство",     "price": 20, "category": "MANDATORY", "tags": ["medicine"],
    "effects": { "care": 20, "mood": 10 },    "asset": "item_pills", "onlyWhenNeeded": true,
    "childHint": "Нужно, только если Финни простудился" }
]
```

`onlyWhenNeeded: true` — товар показывается только когда его тег есть в `period.mandatoryNeeds`
(через событие). Минимум 8 позиций двух типов выполнен с запасом.

## Цели — `goals.json`

```json
[
  { "id": "goal_house", "name": "Большой домик", "cost": 80,  "asset": "goal_house", "rewardAccessoryId": "bow" },
  { "id": "goal_bike",  "name": "Велосипед",     "cost": 120, "asset": "goal_bike",  "rewardAccessoryId": "scarf" },
  { "id": "goal_park",  "name": "Поход в парк",  "cost": 60,  "asset": "goal_park",  "rewardAccessoryId": "hat" }
]
```

Цены подобраны так, чтобы при пополнении 15–20 в период цель достигалась за 3–6 периодов.

## Задания — `tasks.json`

Общая часть у всех типов:

```json
{
  "id": "budget_split_60",
  "topic": "BUDGET",                 // BUDGET | SAVINGS | PAYMENTS
  "title": "Раздели 60 монет",
  "intro": "У Финни 60 монет на неделю. Еда стоит 20, уход 10. Разложи монеты по кучкам.",
  "type": "ALLOCATE",
  "reward": 20,
  "rewardOnMistake": 7,
  "explanationSuccess": "Ты сначала закрыл нужное и что-то отложил. Так делают те, кто умеет планировать.",
  "explanationMistake": "На нужное не хватило: еда и уход стоят 30. Сначала нужное, потом остальное.",
  "hint": "Подумай: без чего Финни точно нельзя?",
  "payload": { ... }                 // зависит от type
}
```

Обязательные поля проверяет `ContentValidator`. Рекомендации к текстам: `intro` ≤ 30 слов,
объяснения ≤ 25 слов, числа ≤ 200, только + и −.

### Тип `CHOICE` — ситуация с вариантами

```json
"payload": {
  "options": [
    { "id": "a", "text": "Купить шляпу за 30, еда подождёт",
      "correct": false, "consequence": "Шляпа красивая, но Финни остался голодным." },
    { "id": "b", "text": "Купить еду за 15, а на шляпу копить",
      "correct": true,  "consequence": "Финни сыт, а шляпа появится через две недели." },
    { "id": "c", "text": "Ничего не покупать и всё отложить",
      "correct": false, "consequence": "Копилка выросла, но Финни голоден. Нужное — первым." }
  ]
}
```

Ответ: `TaskAnswer.Choice(optionId)`. Несколько вариантов могут быть `correct: true`.

### Тип `ALLOCATE` — раздели сумму

```json
"payload": {
  "total": 60,
  "step": 5,
  "buckets": [
    { "id": "mandatory", "label": "Нужное" },
    { "id": "optional",  "label": "Желаемое" },
    { "id": "savings",   "label": "Копилка" }
  ],
  "rule": { "mandatoryMin": 30, "savingsMin": 5 }
}
```

Ответ: `TaskAnswer.Allocation(mapOf("mandatory" to 30, "optional" to 20, "savings" to 10))`.
Оценщик: сумма равна `total`, `mandatory ≥ mandatoryMin`, `savings ≥ savingsMin`.

### Тип `SHOP_LIST` — собери покупки

```json
"payload": {
  "budget": 50,
  "items": [
    { "id": "bread",  "name": "Корм",      "price": 15, "mandatory": true },
    { "id": "soap",   "name": "Мыло",      "price": 10, "mandatory": true },
    { "id": "ball",   "name": "Мячик",     "price": 20, "mandatory": false },
    { "id": "hat",    "name": "Шляпа",     "price": 30, "mandatory": false },
    { "id": "sticker","name": "Наклейки",  "price": 5,  "mandatory": false }
  ]
}
```

Ответ: `TaskAnswer.Selection(setOf("bread", "soap", "ball"))`.
Оценщик: все `mandatory` выбраны и сумма ≤ `budget`. Экран показывает текущую сумму и остаток по мере выбора.

### Стартовый набор из 6 заданий (по 2 на тему)

| id | Тема | Тип | Суть | Награда / за ошибку |
|---|---|---|---|---|
| budget_split_60 | BUDGET | ALLOCATE | Раздели 60: еда 20, уход 10, отложи хоть немного | 20 / 7 |
| budget_first_what | BUDGET | CHOICE | Шляпа или еда, когда монет мало | 15 / 5 |
| savings_weeks | SAVINGS | CHOICE | Цель 60, откладываешь 20 в неделю — через сколько недель? (варианты 2/3/6) | 15 / 5 |
| savings_take_or_not | SAVINGS | CHOICE | Взять из копилки на игрушку или подождать | 15 / 5 |
| payments_shop_50 | PAYMENTS | SHOP_LIST | Собери покупки на 50, нужное обязательно | 20 / 7 |
| payments_two_prices | PAYMENTS | CHOICE | Корм 15 или тот же корм 25 «с подарком-наклейкой за 5» | 15 / 5 |

Полные тексты пишет backend-разработчик прямо в JSON. Каждое задание проходится в тесте
и в правильном, и в ошибочном варианте (**ТЗ 2.6**).

### Как добавить новое задание

1. Добавить объект в `tasks.json` с уникальным `id` одного из трёх типов.
2. Запустить `ContentValidationTest` — он проверит поля и минимумы.
3. Всё. Код не трогается. Если нужен новый **тип** задания — это уже новая механика: добавляется
   `TaskType`, оценщик в `:domain` и экран в `:app`.

## События периода — `events.json` (опционально, после MVP)

Единственный разрешённый ТЗ случай «негативного» сценария — заранее спланированные
непредвиденные расходы (**ТЗ 2**).

```json
[
  { "id": "cold", "periodIndex": 3, "title": "Финни простудился",
    "text": "Ему нужно лекарство за 20. Хорошо, что у тебя есть план!",
    "extraMandatoryNeeds": ["medicine"] }
]
```

Событие добавляет тег в `mandatoryNeeds` периода и делает видимым товар с `onlyWhenNeeded`.
Питомец не болеет визуально «страшно»: просто иконка платка и текст. Если лекарство не куплено —
критерий A не выполнен, и только.

## Справочник — `glossary.json`

```json
[
  { "term": "Бюджет", "text": "Сколько монет у тебя есть и на что ты их разложишь." },
  { "term": "Нужное", "text": "Без этого Финни плохо: еда, уход." },
  { "term": "Желаемое", "text": "Приятно, но можно подождать: игрушки, украшения." },
  { "term": "Копилка", "text": "Монеты, которые ты откладываешь на цель. Тратить их нельзя, пока не решишь сам." },
  { "term": "Цель", "text": "То, на что ты копишь. У цели есть цена." },
  { "term": "План и факт", "text": "План — как ты хотел потратить. Факт — как потратил на самом деле." }
]
```

## Тексты обратной связи — `feedback.json`

Шаблоны с плейсхолдерами `{n}`, `{item}`, `{goal}`, `{task}`. Ключи фиксированы и перечислены в
`FeedbackKey` (enum в `:domain`), тест проверяет, что для каждого ключа есть текст.

```json
{
  "ledger.start": "Стартовые монеты",
  "ledger.deposit": "В копилку",
  "ledger.goal": "Цель: {goal}",
  "ledger.task": "Задание: {task}",
  "ledger.week_income": "Карманные деньги",

  "period.a_ok": "Ты купил еду — Финни сыт",
  "period.a_fail": "Еды не было. На новой неделе купи её первой",
  "period.c_ok": "Ты отложил {n} — до цели ближе",
  "period.c_fail": "Копилка не пополнилась. Попробуй отложить хотя бы 5",
  "stage.up": "Финни подрос! Это потому, что ты хорошо решаешь",

  "mood.hungry": "Голоден: купи еду в магазине",
  "mood.grew": "Подрос! Продолжай в том же духе",
  "mood.purchase": "Рад покупке: {item}",
  "mood.default": "Ждёт твоих решений",

  "tip.choose_goal": "Выбери цель в копилке!",
  "tip.save_for": "Отложи немного на «{goal}»",

  "deposit.rejected": "Не хватает монет: на балансе {n}",

  "topic.budget": "Планирую",
  "topic.savings": "Коплю",
  "topic.payments": "Покупаю"
}
```

Правила текстов: без «ты виноват», «плохо», «неправильно». Ошибка описывается через питомца и
следующий шаг, не через оценку ребёнка (**ТЗ 3.5, 8.1**).

## Демо-профиль — `demo-profile.json`

```json
{
  "nickname": "Демо",
  "pet": { "name": "Финни", "speciesId": "cat", "colorId": "orange" }
}
```

Остальное движок строит как при обычном `CreateProfile`, плюс `settings.demoMode = true`.
