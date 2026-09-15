# 10 — План команды

Состав: 1 Android/Kotlin-разработчик (далее **A**), 1 Java backend-разработчик, впервые в мобильной
разработке (далее **B**). Ключевой принцип разделения: **B владеет `:domain` и контентом, A владеет `:app`.**
Встречаются на контракте `GameEngine.handle` из [06](06-architecture.md#api-игрового-движка-контракт-между-разработчиками).

## Зоны ответственности

| Область | Владелец | Второй |
|---|---|---|
| Модели `GameState` и команды ([02](02-domain-model.md)) | B | A ревьюит, т.к. строит UiState |
| Обработчики движка ([03](03-processes.md), [04](04-rules-and-formulas.md)) | B | — |
| `FeedbackBuilder`, тексты `feedback.json` | B | A проверяет на экране |
| Контент: задания, товары, цели, глоссарий ([05](05-content-model.md)) | B | — |
| `ContentValidator` и все unit-тесты `:domain` ([09](09-test-cases.md)) | B | — |
| Gradle, модули, зависимости, подпись, APK | A | — |
| `ContentRepository`, `GameStateStore`, `GameRepository` | A | B пишет round-trip тест сериализации |
| Compose-экраны, навигация, тема ([07](07-screens.md)) | A | — |
| ViewModel + `query`-функции для UiState | A | B пишет `query` в `:domain` по запросу A |
| Ассеты питомца, товаров, иконка 512×512 | A | генерация/поиск с лицензией, список в `docs/licenses.md` |
| README, документация к сдаче ([08](08-requirements-matrix.md)) | B | A — разделы про сборку |
| Ручной E2E на устройстве, отчёт | A | B — прогон по чек-листу |
| Презентация, карточка RuStore | оба | — |

## Контракт, который фиксируем в первый день

Прежде чем разойтись по модулям, вместе пишем и коммитим **пустые** файлы с сигнатурами:

1. `domain/model/*.kt` — все data class-ы из [02](02-domain-model.md) как есть.
2. `domain/engine/GameEngine.kt` — `handle()` с `TODO()` внутри, `Outcome`, `Command`, `GameError`.
3. `domain/content/Content.kt` — структура `Content`.
4. `domain/query/Queries.kt` — сигнатуры: `taskStatus(state, task)`, `planVsFact(state)`,
   `goalView(state, content)`, `moodExplanation(state, content)`, `recoveryOptions(...)`.

После этого A может делать экраны на фейковых данных (`PreviewData.kt`), а B — реализовывать движок.
Изменение сигнатуры — только через короткое согласование в чате и один PR.

## Этапы

### Этап 0 — каркас (день 1)

- [ ] A: модуль `:domain`, зависимости, `Application` + `AppContainer`, пустой `NavHost`, тема.
- [ ] B: модели, контракт движка, `config.json` с числами из [04](04-rules-and-formulas.md), первые 2 задания.
- [ ] Оба: `./gradlew :domain:test :app:assembleDebug` зелёный в CI или локально.

### Этап 1 — промежуточная сдача (ТЗ 7.1)

Нужно: 3 экрана (CreatePet, Home, BudgetPlan), README со стеком и способом запуска, таблица статусов, APK.

- [ ] B: `ProfileHandler`, `PlanHandler`, `ShopHandler`, `Ledger`, тесты T-01…T-07.
- [ ] B: полный контент по минимумам 2.6, `ContentValidationTest`.
- [ ] A: `GameStateStore` + `GameRepository`, Onboarding, CreateProfile, CreatePet, Home, BudgetPlan, Shop, FeedbackSheet.
- [ ] A: debug-APK, запуск на физическом устройстве.
- [ ] B: README + статусы в [08](08-requirements-matrix.md).

### Этап 2 — полный игровой цикл

- [ ] B: `SavingsHandler`, `TaskHandler` с тремя оценщиками, `PeriodHandler`, `PetRules`, `GoalRules`, тесты T-08…T-22.
- [ ] A: Tasks/TaskPlay/TaskResult (3 виджета), Savings/GoalPick/WithdrawConfirm, PeriodSummary, Progress, Help.
- [ ] Оба: первый полный E2E на устройстве по [09](09-test-cases.md#e2e-обязательный-сценарий), фиксируем расхождения.

### Этап 3 — взрослый, демо, надёжность

- [ ] B: `IncomeHandler`, `ResetToDemo`, property-тест T-20, round-trip S-01/S-02.
- [ ] A: AdultGate, Adult, настройки звука/анимаций, демо-плашка, `DEMO_DEFAULT` сборка.
- [ ] A: проверка 360 dp, шрифт 200 %, Accessibility Scanner, время запуска.
- [ ] Оба: прогон стоп-слов по текстам, чек-лист «Что запрещено» из [01](01-product-overview.md).

### Этап 4 — финальная сдача (ТЗ 7.2)

- [ ] A: подписанный release-APK без минификации, тег `v1.0.0`, `keystore.properties` в `.gitignore`.
- [ ] B: документация по [08](08-requirements-matrix.md#документация-к-сдаче-тз-5--где-что-лежит), DOCX/PDF-версия.
- [ ] Оба: резервное видео демонстрации ≤ 3 мин, презентация 8–12 слайдов, карточка RuStore, `docs/licenses.md`.
- [ ] Оба: заполнить отчёт о проверке на устройстве.

## Definition of Done для задачи

- Поведение описано в `docs/` (или PR правит docs), матрица требований обновлена.
- Для `:domain`: unit-тест на успешный и ошибочный путь, `assertInvariants` проходит.
- Для `:app`: экран проверен на 360×640, при шрифте 200 %, все тапаемые элементы ≥ 48 dp,
  тексты из контента, не из кода.
- `./gradlew :domain:test :app:assembleDebug` зелёный.
- Ни одного числа экономики в коде, ни одного текста для ребёнка в `strings.xml`.

## Git

- Ветки от `master`: `feature/<область>-<кратко>` (`feature/domain-shop`, `feature/ui-savings`).
- Мелкие PR, ревью второго участника обязательно для `:domain/model` и контракта, желательно для остального.
- Коммиты: `domain: ShopHandler + T-06/T-07`, `ui: Savings screen`, `content: 6 tasks`, `docs: ...`.
- Секреты и `*.jks` не коммитим. `.gitignore` проверяем на этапе 0.

## Известные ограничения прототипа и план развития (для ТЗ 5.11)

Заполняется по ходу; стартовый список:

- Один профиль на устройство. План: список профилей в разделе взрослого.
- Ежедневный бонус выключен. План: включить после проверки на детях, что он не давит.
- События периода (простуда) — после MVP.
- Планшеты и альбомная ориентация — не проверяются в MVP.
- Нет локализации, только русский.
- Родительский бонус — минимальная реализация без истории.

## Бэклог в GitHub Issues

Задачи по этапам выше заведены в `scripts/issues.json` (38 issues, 5 milestones, 8 labels).
Создать или обновить их в репозитории:

```bash
GITHUB_TOKEN=<fine-grained PAT с правом Issues: write> python3 scripts/create_github_issues.py
```

Скрипт идемпотентен: повторный запуск не дублирует issues и обновляет labels. Проверить без записи:
`python3 scripts/create_github_issues.py --dry-run`. Чтобы добавить задачу, правим `issues.json` и запускаем снова.
