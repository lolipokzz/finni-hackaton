# 09 — Тест-кейсы

Три уровня:

1. **Unit-тесты `:domain`** — автоматические, по одному классу на обработчик. Это основное покрытие
   для **ТЗ 3.4** «логика бюджета, списания, накоплений и прогресса покрыта автотестами».
2. **Тесты контента** — `ContentValidationTest` проверяет минимумы ТЗ 2.6 и целостность ссылок.
3. **Ручной E2E-прогон** обязательного сценария на физическом устройстве перед каждой сдачей,
   с записью результата в таблицу в конце файла.

Все числа — из [04](04-rules-and-formulas.md) со значениями по умолчанию.

## Unit-тесты движка

Формат: ID · предусловие · команда · ожидание. Каждый тест дополнительно прогоняет
`assertInvariants(state)` (см. [02](02-domain-model.md#gamestate--корневой-агрегат)).

### Профиль и период

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-01 | state = null | `CreateProfile("Маша", "Финни", cat/orange)` | balance 100, savings 0, period 1 PLANNING, ledger = [START_BALANCE +100], pet 70/70/70 BABY |
| T-01b | state = null | `CreateProfile("", …)` | `InvalidAmount`/валидационная ошибка, состояние не создано |
| T-02 | период 1 ACTIVE | `ClosePeriod` | period 2 PLANNING, ledger содержит PERIOD_INCOME +60, `budgetAtPlanning` = новый баланс |

### Планирование

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-03 | PLANNING, balance 100 | `UpdatePlan(30, 20, 20)` | plan 30/20/20, remainder 30, не подтверждён |
| T-03b | PLANNING, balance 100 | `UpdatePlan(50, 40, 20)` | `PlanExceedsBudget(110, 100)`, план не изменился |
| T-04 | PLANNING, план 30/20/20 | `ConfirmPlan` | phase ACTIVE, `confirmed = true` |
| T-04b | ACTIVE | `UpdatePlan(...)` | `PlanAlreadyConfirmed` |
| T-04c | PLANNING, план 0/0/0 | `ConfirmPlan` | ошибка `InvalidAmount` (total = 0) |
| T-04d | PLANNING | `BuyItem(food_basic)` | `PlanNotConfirmed` |

### Задания

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-05a | любая фаза, задание `budget_split_60` | `AnswerTask(Allocation(30,20,10))` | success, +20, TASK_REWARD в ledger, `tasksDoneThisPeriod = 1` |
| T-05b | то же | `AnswerTask(Allocation(10,40,10))` | !success, +7, объяснение = `explanationMistake` |
| T-05c | `payments_shop_50` | `Selection(bread, soap, ball)` = 45 | success |
| T-05d | `payments_shop_50` | `Selection(bread, ball, hat)` = 65 или без soap | !success |
| T-05e | CHOICE `budget_first_what` | option b | success; option a → !success |
| T-05f | задание уже success | повторный `AnswerTask` | `TaskAlreadyDone` |
| T-11 | задание !success в периоде 1, сейчас период 1 | `AnswerTask` | `TaskAlreadyDone` (повтор только со следующего периода) |
| T-11b | то же, сейчас период 2 | `AnswerTask` верно | success, полная награда |
| T-17 | `tasksDoneThisPeriod = 2`, demoMode = false | `AnswerTask` | `TaskLimitReached` |
| T-17b | `tasksDoneThisPeriod = 2`, demoMode = true | `AnswerTask` | success |

### Покупки

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-06 | ACTIVE, balance 100, satiety 70 | `BuyItem(food_basic)` | balance 85, satiety 100 (clamp от 110), mood 75, `mandatorySpent 15`, `coveredNeeds = {food}`, feedback содержит «−15» и «+40» |
| T-06b | ACTIVE, balance 100 | `BuyItem(toy_ball)` | balance 80, mood 85, `optionalSpent 20` |
| T-07 | ACTIVE, balance 10, savings 30, есть задание +20 | `BuyItem(toy_ball)` (20) | `InsufficientFunds(missing = 10, options ⊇ {DoTask(+20), WithdrawFromSavings(10), CheaperItem(toy_stickers 5), WaitNextPeriod})`; balance не изменился, ledger не изменился |
| T-07b | ACTIVE, balance 10, savings 0, заданий нет | `BuyItem(toy_ball)` | options = {CheaperItem(toy_stickers), WaitNextPeriod} |
| T-07c | ACTIVE, `mandatoryNeeds` без medicine | каталог для UI | `med_pills` не виден (`onlyWhenNeeded`) |

### Накопления и цель

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-08 | ACTIVE, balance 100, цель goal_park (60) | `Deposit(20)` | balance 80, savings 20, `facts.saved 20`, remaining 40 |
| T-08b | ACTIVE, balance 10 | `Deposit(20)` | `InvalidAmount` |
| T-08c | ACTIVE | `Deposit(0)` | `InvalidAmount` |
| T-09 | history: saved 20, 10, 30 (три периода), savings 20, цель 80 | `eta` | avg 20 → remaining 60 → 3 периода |
| T-09b | history пуста, plan.savings 15, savings 0, цель 60 | `eta` | 4 |
| T-09c | history пуста, plan.savings 0 | `eta` | null |
| T-09d | history: saved 0, 0 | `eta` | периоды с saved ≤ 0 не учитываются → как T-09b/T-09c |
| T-10 | ACTIVE, savings 60, balance 5 | `Withdraw(12)` | savings 48, balance 17, `facts.saved −12`, SAVINGS_WITHDRAW |
| T-10b | savings 60 | `Withdraw(70)` | `InvalidAmount` |
| T-10c | — | `previewWithdraw(12)` | возвращает savingsAfter 48 и etaBefore/etaAfter без изменения состояния |
| T-21 | цель goal_park (60), savings 60 | `ReachGoal` | savings 0, GOAL_REACHED −60, `completedGoals` содержит goal_park, goal = null, mood +30, accessory hat открыт |
| T-21b | savings 50 | `ReachGoal` | `GoalNotReached` |
| T-21c | goal = null | `ReachGoal` | `GoalNotChosen` |
| T-22 | цель A, savings 30 | `ChooseGoal(B)` | goal = B, savings по-прежнему 30 |

### Закрытие периода, состояние и рост

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-12 | план 30/20/20; куплено food 15, care 10, ball 20; deposit 20; pet 100/100/90 | `ClosePeriod` | A ✓ B ✓ C ✓ score 3; satiety 65, care 75, mood clamp(90 − 10 + 20) = 100; growth 3; summary в history |
| T-12b | план 30/20/20; куплено food 15, ball 20, hat 30; deposit 0 | `ClosePeriod` | A ○ (care не закрыт) B ○ (50 > 20) C ○; score 0; mood −25; growth 0; explanation содержит «care» фразу |
| T-12c | план 30/20/0; deposit 5 | `ClosePeriod` | C ✓ (saved 5 > 0 и ≥ 0) |
| T-12d | план 30/20/20; deposit 10 | `ClosePeriod` | C ○ (10 < 20) |
| T-12e | план 30/20/20; deposit 20, withdraw 20 | `ClosePeriod` | saved 0 → C ○ |
| T-13 | pet mood 35 | `moodLabel` | SAD; mood 40 → CALM; 70 → HAPPY |
| T-13b | satiety 29 | flags | содержит HUNGRY; 30 → нет |
| T-14 | growth 4, score 1 | `ClosePeriod` | growth 5, stageBefore BABY, stageAfter TEEN, feedback содержит `stage.up` |
| T-14b | growth 10 ADULT, score 0 | `ClosePeriod` | growth 10, stage ADULT (не понижается) |
| T-15 | satiety 20, есть summary | `moodExplanation` | `mood.hungry` имеет приоритет над summary |
| T-18 | новый профиль | 5 × (план → покупки → `ClosePeriod`) | period.index 6, history.size 5, все инварианты, ни одной ошибки |

### Доход и взрослый

| ID | Предусловие | Действие | Ожидание |
|---|---|---|---|
| T-23 | `dailyBonus.enabled`, lastDate "2026-09-14", demo false | `ClaimDailyBonus("2026-09-15")` | +5 DAILY_BONUS, lastDate обновлён |
| T-23b | lastDate "2026-09-15" | `ClaimDailyBonus("2026-09-15")` | без изменений |
| T-23c | demo true | `ClaimDailyBonus(...)` | без изменений |
| T-24 | — | `AdultBonus(30, "Помог по дому")` | +30 ADULT_BONUS с title |
| T-24b | — | `AdultBonus(100, …)` | `InvalidAmount` |
| T-25 | любое состояние | `ResetToDemo` | новый state: nickname «Демо», demoMode true, balance 100, period 1 PLANNING, history пуста |

### Журнал и инварианты

| ID | Проверка |
|---|---|
| T-20 | После случайной последовательности из 200 валидных команд (property-test с фиксированным seed): `balance == Σ balanceDelta`, `savings == Σ savingsDelta`, оба ≥ 0, все PetState в 0..100, стадия не убывает |
| T-20b | Каждый `Transaction.title` непустой, у PURCHASE/TASK_REWARD/GOAL_REACHED есть `refId` |

## Тесты контента — `ContentValidationTest`

- species × colors ≥ 9; items ≥ 8, есть оба `category`; goals ≥ 3; tasks ≥ 6 и по каждой из 3 тем ≥ 2;
  stages = 3 с возрастающими порогами.
- Все `id` уникальны; `rewardAccessoryId`, `tags` в `mandatoryNeeds`, `refId` событий ссылаются на существующие записи.
- У каждого задания: `reward > rewardOnMistake > 0`, непустые `explanationSuccess/Mistake`;
  CHOICE имеет ≥ 1 `correct: true` и ≥ 1 `false`; ALLOCATE `mandatoryMin + savingsMin ≤ total`;
  SHOP_LIST сумма обязательных ≤ budget.
- Для каждого `FeedbackKey` есть строка в `feedback.json`.
- Тексты для ребёнка не содержат стоп-слов из списка (`плохо`, `виноват`, `неправильно`, `умер`, `болеет`)
  — простая проверка на подстроку, список в тесте.

## Сериализация

| ID | Проверка |
|---|---|
| S-01 | `GameState` → JSON → `GameState` равен исходному (round-trip) для состояния после T-18 |
| S-02 | JSON с `schemaVersion = 0` → репозиторий возвращает `null` и удаляет файл (сброс с сообщением) |

## E2E: обязательный сценарий

Прогоняется вручную на физическом устройстве (Android ≥ 8, ОЗУ ≥ 3 ГБ) в демо-режиме перед каждой сдачей.
Ожидаемые числа при точном следовании шагам.

| Шаг ТЗ | Действие | Ожидаемый результат |
|---|---|---|
| 1 | Установить APK, запустить | Онбординг за ≤ 5 с, 3 карточки, три типа решений |
| 2 | Имя «Демо», далее | Нет полей телефона/e-mail |
| 3 | Котик, рыжий, имя «Финни» | Home: питомец, монеты 100, копилка 0, «Выбери цель», неделя 1, «Составь план» |
| 4 | Открыть Задания | 6 заданий по 3 темам, все доступны |
| 5 | План 30 / 20 / 20, остаток 30, подтвердить | Экран «план и факт», Home показывает «План составлен» |
| 6 | Задание «Раздели 60» → 30/20/10 | Объяснение, «+20», монеты 120. Затем то же задание ошибочно нельзя (выполнено) |
| 7a | Магазин → Корм 15 → купить | Монеты 105, сытость 100, чек-лист «Еда ✓», карточка «что изменилось» |
| 7b | Мячик 20 → купить | Монеты 85, настроение выше, «Желаемое» |
| 7c | Купить Домик 40, Шляпу 30 (монеты 15), затем попытка Мячик 20 | Экран «Не хватает 5 монет» с вариантами; баланс 15 не изменился |
| 8 | Копилка → цель «Поход в парк» 60 → отложить 15 | Копилка 15 / 60, монеты 0, «≈ 3 недели» |
| 9 | Home | Фраза под питомцем, шкалы, план/факт по нажатию |
| 10 | «Завершить неделю» → подтвердить (уход не закрыт — предупреждение) | Итоги: A ○ B ○ (90 > 20) C ○ (15 < 20); настроение −25; «Новая неделя» → монеты +60, план |
| 10′ | Ещё 2–4 недели «правильно»: план 25/10/25, корм, мыло, отложить 25, закрыть | Каждая неделя score 3; на 2-й правильной неделе «Финни подрос!» → Подросток |
| 11 | Свернуть, убить процесс, запустить | Home с теми же значениями |
| 12 | «Для взрослых» → пример → раздел → «Сбросить демо» | Подтверждение → онбординг или Home с монетами 100, неделя 1 |

## Отчёт о проверке на устройстве (заполняется перед сдачей)

| Дата | Устройство, Android, ОЗУ | Сборка | Результат E2E | Время запуска | Замечания |
|---|---|---|---|---|---|
| | | | | | |
