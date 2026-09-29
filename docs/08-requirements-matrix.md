# 08 — Матрица соответствия требованиям ТЗ

Статусы: `реализовано` · `в работе` · `не начато` · `не требуется`. Обновляется в каждом PR, который закрывает пункт.
Актуально на 29.09.2026 (`master`).

Колонка «Где» указывает экран (`presentation/screens/…`), логику (`domain/…`, `data/…`) и unit-тест
(`app/src/test/…`, класс и метод). Все тесты запускаются командой `./gradlew :app:testDebugUnitTest`
(177 тестов). Ручные сценарии — в [09](09-test-cases.md).

## Функциональные требования (ТЗ 2.5)

| Пункт | Требование | Где (экран / модуль / тест) | Статус |
|---|---|---|---|
| 2.5.1 | Знакомство с целью игры и тремя типами решений | Экран создания питомца: шаги `HELLO → COLOR → NAME → PLAN → GROW → DREAM` (`PetCreationScreen`, `CreationStep`); шаг PLAN — три решения: обязательное, необязательное, копилка. Тест `PetCreationViewModelTest.introductionWalksThroughStepsAndCreatesPetAtTheEnd` | реализовано |
| 2.5.1 | Гостевой режим, только игровое имя и персонаж | Нет регистрации и сети: профиль — раскраска и имя питомца (`PetCreationViewModel`, `GameRepository.createPet`). Тесты `GameRepositoryImplTest.creatingPetStartsGameAndSavesIt`, `PetCreationViewModelTest.noSaveStartsCreation` | реализовано |
| 2.5.1 | Подсказка доступна в любой момент | «Настройки» → «Как играть» (три карточки знакомства) и «Словарик» (`SettingsScreen`); подсказки по нажатию на показатели главного экрана (`HomeInfoDialog`); реплики Финни подсказывают следующий шаг (`HomeViewModelTest.finniAsksForTheMostImportantThing`) | реализовано |
| 2.5.2 | Настройка внешнего вида питомца | 9 раскрасок кота (`PetColor`, `assets/cat/skins`), шаг COLOR (`PetCreationScreen`). Тест `ContentMinimumsTest.atLeastNinePetLooksAndThreeStages` | реализовано |
| 2.5.2 | Игровое имя питомца | Шаг NAME (`PetCreationScreen`), имя по умолчанию «Финни» | реализовано |
| 2.5.3 | Питомец, баланс, копилка, цель, показатели, задание — на одном экране | `HomeScreen`: 3D-питомец, монеты, сытость и настроение, карточка копилки (сумма и цель), карточка активного задания. Тесты `HomeViewModelTest.activeTaskIsAdventureOfWeekFirst`, `afterAdventureActiveTaskIsNextTask` | реализовано |
| 2.5.3 | С главного экрана: план, задания, покупки, копилка, прогресс, взрослый | `HomeScreen`: нижнее меню (Задания, Магазин, Гардероб, Копилка), кнопки «План недели» и «Прогресс», шестерёнка → «Настройки» → «Для взрослых» (два нажатия) | реализовано |
| 2.5.4 | Только внутриигровая валюта | Монеты — только число в `GameState`; нет платёжных SDK, разрешения `INTERNET` и покупок в приложении | реализовано |
| 2.5.4 | Валюта за задания и/или периодический доход | Карманные монеты каждую неделю, награды за задания и приключения (`GameEngine.finishWeek`, `answerTask`, `completeAdventure`). Тесты `GameEngineTest.goodWeekMakesAllFourDeedsPaysSavingsBonusAndStartsNewWeek`, `answeringTaskPaysRewardOnceAndLimitsTasksPerWeek`, `AdventureViewModelTest.perfectRunPaysFullRewardAndUnlocksWeekEnd` | реализовано |
| 2.5.4 | У каждого начисления источник и сумма | Журнал монет с причиной каждой записи (`LedgerEntry`, ключи `ledger.*` в `feedback.json`), экран «Прогресс» → «Монеты этой недели». Тесты `GameEngineTest.startBalanceIsPostedToLedger`, `GameSaveMapperTest.sampleGameCoversEveryKindOfLedgerEntry` | реализовано |
| 2.5.5 | Распределение минимум по 3 направлениям до периода | Окно «План недели» (`WeekPlanDialog`): обязательное, необязательное, копилка; неделю нельзя закончить без плана. Тесты `GameEngineTest.newGameStartsWithPlanOnFirstDay`, `weekCannotFinishWithoutPlan`, `HomeViewModelTest.firstWeekAsksToSplitStartCoins` | реализовано |
| 2.5.5 | Контроль суммы ≤ бюджета, показ остатка | `WeekPlanDialog` показывает «Осталось разложить», кнопка активна только при нуле. Тесты `GameEngineTest.planMustSplitWholeBalanceOnceAndWithoutMinus`, `HomeViewModelTest.plusStopsAtBudgetAndMinusStopsAtZero`, `sliderSnapsToStepAndStopsAtWhatIsLeft` | реализовано |
| 2.5.5 | Изменение до подтверждения, после — сравнение план/факт | Ползунки меняются до «Начать неделю»; план и факт — в магазине (остаток по строкам), в итогах недели (`WeekSummaryDialog`) и на экране «Прогресс». Тесты `HomeViewModelTest.confirmingPlanClosesWindowAndStartsWeek`, `ShopViewModelTest.budgetsFollowPlanAndPurchases`, `GameEngineTest.buyingMoreOptionalThanPlannedCostsOnlyThatDeed` | реализовано |
| 2.5.6 | Товары двух типов с ценой | 11 товаров: 3 обязательных (еда), 8 необязательных (`data/content/ShopContent.kt`, `ShopScreen`). Тест `ContentMinimumsTest.atLeastEightPurchasesOfBothTypes` | реализовано |
| 2.5.6 | Перед покупкой: цена, категория, влияние | Окно подтверждения покупки (`ShopScreen.PurchaseConfirmDialog`): категория и цена, «Финни получит» (сытость и настроение), сколько останется монет и по плану | реализовано |
| 2.5.6 | Подтверждение, списание, история периода | Покупка только из окна подтверждения; списание и запись в журнал (`GameEngine.buy`). Тесты `GameEngineTest.buyingFoodSpendsCoinsFeedsPetAndCoversFood`, `GameRepositoryImplTest.buyingChangesGameAndFeedsPetInOneSnapshot` | реализовано |
| 2.5.6 | Нет отрицательного баланса; объяснение и варианты | Окно «Не хватает монет» (`NotEnoughDialog`): сколько не хватает и что можно сделать — взять недостающее из копилки, заработать на задании, выбрать товар дешевле, подождать следующую неделю. Тесты `GameEngineTest.buyingWithoutEnoughCoinsChangesNothing`, `ShopViewModelTest.missingCoinsCanBeTakenFromSavingsExactly`, `savingsAreNotOfferedWhenTheyDoNotCoverTheGap` | реализовано |
| 2.5.7 | Цели с понятной стоимостью | 4 цели: кроватка 60, велосипед 90, море 110, ремонт 120 (`data/content/GoalContent.kt`, `SavingsScreen`). Тест `ContentMinimumsTest.atLeastThreeGoals` | реализовано |
| 2.5.7 | Цель: стоимость, накоплено, осталось | Кольцо цели в «Копилке»: «накоплено из стоимости», «осталось». Тест `GameEngineTest.goalRemainingNeverGoesBelowZero` | реализовано |
| 2.5.7 | Регулярное пополнение | Строка «Копилка» в плане недели уходит в копилку сразу, плюс ручное пополнение (`SavingsScreen`). Тесты `GameEngineTest.confirmedPlanSendsSavingsLineToPiggyBankRightAway`, `depositMovesCoinsToSavingsAndRejectsMoreThanBalance`, `SavingsViewModelTest.depositMovesChosenAmountAndKeepsChoice` | реализовано |
| 2.5.7 | Понятный расчёт срока по среднему пополнению | «Примерно N нед.»: остаток, делённый на недельное пополнение по плану, с округлением вверх (`GameState.weeksToGoal`, `SavingsScreen`). Тест `GameEngineTest.weeksToGoalIsCeilOfRemainingOverWeeklyDeposit` | реализовано |
| 2.5.7 | Снятие только после подтверждения с предпросмотром | Окно снятия (`WithdrawDialog`): сколько останется и на сколько отодвинется цель. Тесты `SavingsViewModelTest.withdrawWindowShowsHowGoalMovesAway`, `confirmingMovesCoinsToWalletAndClosesWindow` | реализовано |
| 2.5.8 | Задания минимум по 3 темам | 6 заданий, по 2 в темах «бюджет», «сбережения», «платежи» (`data/content/TaskContent.kt`), плюс 5 приключений недели. Тест `ContentMinimumsTest.atLeastSixTasksCoveringAllThreeTopics` | реализовано |
| 2.5.8 | Игровая ситуация с выбором, не только тест | Механики «раздели сумму» и «собери покупки» (`TaskPlayScreen`), приключения с оплатой купюрами, сдачей и корзиной (`AdventureScreen`, `PayScene`, `BasketScene`). Тесты `AdventureRulesTest`, `AdventureViewModelTest.wrongAnswerIsExplainedAndBasketCanBeFixed` | реализовано |
| 2.5.8 | Объяснение после любого ответа | `TaskResultCard` после любого ответа, объяснения в приключениях. Тесты `ContentMinimumsTest.choiceTasksHaveOneRightAnswerAndExplainEveryOption`, `AdventureViewModelTest.mistakesAreExplainedAndOnlyLowerTheReward` | реализовано |
| 2.5.8 | Сложность для 7–11 лет | Числа до 120, только сложение и вычитание, короткие фразы ([05](05-content-model.md#задания--taskcontentkt)) | реализовано |
| 2.5.8 | В демо все задания доступны сразу | `GameState.taskStatus`: в демо нет лимита в неделю, после ошибки можно сразу повторить. Тесты `GameEngineTest.demoLiftsWeeklyLimitAndLetsRetryAtOnce`, `outsideDemoWeeklyLimitAndRetryWaitStay` | реализовано |
| 2.5.9 | Видно изменение баланса, копилки, показателя питомца | После покупки — окно с эффектом (`BoughtDialog`), после задания — награда (`TaskResultCard`), в конце недели — итоги (`WeekSummaryDialog`), показатели на главном экране | реализовано |
| 2.5.9 | Причинно-следственная связь и следующий шаг | Тексты `feedback.json` (`period.*`, `plan.*`, `finish.*`), реплики Финни над головой. Тесты `FeedbackContentTest.shippedFileHasTextForEveryKey`, `HomeViewModelTest.finniAsksForTheMostImportantThing`, `finishingTooEarlyExplainsWhy` | реализовано |
| 2.5.9 | Путь восстановления после ошибки | Взять недостающее из копилки или подождать, повтор задания на следующей неделе, скорректировать следующий план. Тесты `ShopViewModelTest.missingCoinsCanBeTakenFromSavingsExactly`, `GameEngineTest.extraDepositCoversWithdrawalWithinTheWeek`, `HomeViewModelTest.nextDayShowsSummaryThenPlanStartingFromLastPlan` | реализовано |
| 2.5.10 | ≥ 3 состояний/стадий | 3 стадии: малыш, подросток, взрослый (`PetGrowthStage`, `assets/cat/{baby,teen,adult}.glb`), эмоции по сытости и настроению. Тесты `PetTest.stageIsDerivedFromGrowthPoints`, `moodLevelThresholdsAreFortyAndSeventy` | реализовано |
| 2.5.10 | Развитие по совокупности периодов | Шаги роста за 4 дела недели: сыт, не скучает, копилка по плану, траты по плану (`GameEngine.finishWeek`). Тесты `GameEngineTest.twoWeeksWithAllDeedsGrowPetToTeenAndRaisePocketMoney`, `boredHungryWeekGivesFewStepsAndMoodStopsAtBored`, `PetTest.growthNeverGoesDown`, `EconomyBalanceTest` | реализовано |
| 2.5.10 | Объяснение причины настроения | Нажатие на показатель → `HomeInfoDialog` объясняет, от чего растёт и падает; итоги недели называют дела. Тест `GameEngineTest.summaryReportsStageChange` | реализовано |
| 2.5.11 | Завершённые задания, прогресс цели, итоги периода | Экран «Прогресс» (`ProgressScreen`): рост, цель, итоги прошлой недели с планом и фактом, задания по темам, сбывшиеся цели, журнал. Тест `GameEngineTest.topicProgressCountsDoneTasksPerTopic` | реализовано |
| 2.5.11 | Справочник терминов | «Настройки» → «Словарик» (`SettingsScreen`, `glossary`) | реализовано |
| 2.5.12 | Барьер для взрослого | Пример на сложение двух двузначных чисел (`AdultScreen`, `AdultViewModel`). Тесты `AdultViewModelTest.lockedSectionCannotChangeDataOrSettings`, `wrongAnswersDoNotUnlockAndThreeAttemptsRefreshChallenge` | реализовано |
| 2.5.12 | Цели, темы, прогресс без негативных оценок | `AdultScreen`: «Чему учится ребёнок», «Общий прогресс», «Пройденные темы» — только факты, без оценок | реализовано |
| 2.5.12 | Правила начисления баллов родителем | Не делаем: ТЗ оставляет на усмотрение команды; доход — только из игры, чтобы экономика оставалась честной | не требуется |
| 2.5.13 | Сохранение после перезапуска | Вся игра и настройки — JSON-файлы DataStore, запись после каждого действия (`DataStoreGameStore`, `GameRepositoryImpl`). Тесты `DataStoreGameStoreTest.savedGameSurvivesRestart`, `GameRepositoryImplTest.gameContinuesAfterRestart`, `PetCreationViewModelTest.petCreatedInOneLaunchIsRestoredInTheNext`, `DataStoreSettingsRepositoryTest.updatedSettingsSurviveRestart` | реализовано |
| 2.5.13 | Демо-режим с тестовым профилем и сбросом | «Для взрослых» → «Включить демо» / «Сбросить демо»: профиль «Финни Демо», неделя завершается кнопкой в тот же день (`GameRepositoryImpl.resetToDemo`). Тесты `GameRepositoryImplTest.demoStartsWithStartBalanceOnFirstWeek`, `GameEngineTest.demoWeekFinishesTheSameDay`, `AdultViewModelTest.demoCanBeRepeatedAndStartsWithCleanProgress` | реализовано |
| 2.5.14 | Новое задание без переработки логики | Объект `Task` в `TaskContent.kt`, правила и экраны не меняются ([05](05-content-model.md#как-добавить-новое-задание)). Тест `ContentMinimumsTest` | реализовано |

## Минимальный контент (ТЗ 2.6)

| Элемент | Минимум | Сейчас | Проверка | Статус |
|---|---|---|---|---|
| Питомец | 9 комбинаций | 9 раскрасок кота | `ContentMinimumsTest.atLeastNinePetLooksAndThreeStages` | реализовано |
| Периоды | 5 подряд в демо | без ограничений, неделя завершается кнопкой | `GameEngineTest.demoWeekFinishesTheSameDay`, `EconomyBalanceTest` (прогон 5 недель) | реализовано |
| Задания | 6 по 3 темам | 6 (по 2 в теме) + 5 приключений | `ContentMinimumsTest`: число, темы, верный и ошибочный ответ | реализовано |
| Покупки | 8 двух типов | 11 (3 обязательных, 8 необязательных) | `ContentMinimumsTest.atLeastEightPurchasesOfBothTypes` | реализовано |
| Цели | 3 | 4 | `ContentMinimumsTest.atLeastThreeGoals` | реализовано |
| Стадии | 3 | 3 | `ContentMinimumsTest`, `GameEngineTest.twoWeeksWithAllDeedsGrowPetToTeenAndRaisePocketMoney` | реализовано |

## Нефункциональные (ТЗ 3)

| Пункт | Требование | Где | Статус |
|---|---|---|---|
| 3.1 | Android 8.0+ | `minSdk = 26` (Android 8.0) | реализовано |
| 3.1 | Портрет от 360 dp | Вёрстка на Compose с прокруткой; прогон на экране 360×640 dp не проведён | в работе |
| 3.1 | Проверка на физическом устройстве (ОЗУ ≥ 3 ГБ) | Отчёт — [09](09-test-cases.md#отчёт-о-проверке-на-устройстве-заполняется-перед-сдачей), пока пустой | не начато |
| 3.1 | Аппаратные разрешения не нужны для обязательного сценария | Единственное разрешение `RECORD_AUDIO` — для необязательного «Кот повторяет слова», выключено по умолчанию | реализовано |
| 3.1 | Работа без интернета | Нет разрешения `INTERNET` и сетевых зависимостей: всё работает офлайн | реализовано |
| 3.2 | Учебный контент отделён от интерфейса | Контент — `data/content/*.kt` и `assets/content/feedback.json`, экраны получают его через `Content` ([05](05-content-model.md)) | реализовано |
| 3.3 | Подписанный релизный APK, уникальный package | Package `ru.larpinovplay.finniapp`, `versionName 1.0`, `versionCode 1`. `assembleRelease` собирает неподписанный APK, подпись вручную через `apksigner` ([README](../README.md#релизная-сборка)); ключ команды ещё не заведён | в работе |
| 3.3 | Черновик карточки RuStore | Есть иконка 512×512 (`docs/rustore/icon-512.png`); нет описаний, категории, скриншотов и обоснования возрастной маркировки | в работе |
| 3.3 | Права на изображения, шрифты, звуки, библиотеки | [licenses.md](licenses.md): библиотеки, шрифт, иконки, звуки описаны; источник исходной 3D-модели, иконки приложения и сцен комнаты помечен «⚠ уточнить» | в работе |
| 3.4 | Разделение логики, контента, хранения, UI | Пакеты `domain/` (правила без Android), `data/` (хранение и контент), `presentation/` (экраны) в модуле `:app` ([06](06-architecture.md)) | реализовано |
| 3.4 | Нет секретов в репозитории | Ключей, паролей и токенов нет; `.gitignore`: `keystore.properties`, `*.jks`, `*.keystore` | реализовано |
| 3.4 | Только нужные разрешения, каждое обосновано | `RECORD_AUDIO` — только для «Кот повторяет слова»: выключено по умолчанию, доступ спрашивается при включении переключателя в «Настройках» ([06](06-architecture.md)). Тесты `DataStoreSettingsRepositoryTest.voiceRepeatIsOffByDefault`, `SettingsViewModelTest` | реализовано |
| 3.4 | Запуск ≤ 5 с, отклик ≤ 1 с | На эмуляторе главный экран ~0,6 с, 3D-питомец ~3 с; замер на физическом устройстве не проведён | в работе |
| 3.4 | Нет падений, потери прогресса и тупиков в сценарии | Сценарий проходится на эмуляторе; ошибки записи показываются и не теряют сохранение (`DataStoreGameStoreTest`, `GameRepositoryImplTest.failedSaveLeavesSnapshotUnchangedAndReportsError`); прогон на устройстве не проведён | в работе |
| 3.4 | Ключевая логика покрыта тестами | 177 unit-тестов: бюджет, покупки, копилка, рост, сохранение, ViewModel (`./gradlew :app:testDebugUnitTest`) | реализовано |
| 3.5 | Без аккаунта и персональных данных | Нужны только раскраска и игровое имя питомца, данные не покидают устройство | реализовано |
| 3.5 | Без рекламы, подписок, покупок и внешних ссылок | Нет SDK рекламы и платежей, нет `INTERNET`, нет ссылок в интерфейсе | реализовано |
| 3.5 | Тексты не пугают и не стыдят | Правила текстов в [05](05-content-model.md#тексты-обратной-связи--assetscontentfeedbackjson): ошибка — через питомца и следующий шаг. Открытый вопрос: клипы и звуки «ударов» при нажатии на голову и лапы (`PetHits`) стоит обосновать или смягчить | в работе |
| 3.5 | Сброс и удаление доступны взрослому | «Для взрослых» → «Сбросить профиль», «Удалить все данные» с подтверждением. Тесты `AdultViewModelTest.resetPreservesSettingsAndReturnsToCreation`, `deleteAlsoRestoresSettings`, `cancelledConfirmationDoesNotResetProfile` | реализовано |
| 3.6 | Короткие фразы, термины с объяснением | Словарик в «Настройках», подсказки у показателей | реализовано |
| 3.6 | Элементы не меньше 48 × 48 dp | Кнопки и строки настроек от 48 dp | реализовано |
| 3.6 | Основной текст от 16 sp, читаемость при увеличении шрифта | Основной текст 16–17 sp; около 90 подписей 11–15 sp (вторичный текст на карточках) без обоснования в [07](07-screens.md); увеличение шрифта до 200 % не проверено | в работе |
| 3.6 | Цвет — не единственный способ передать смысл | Состояния продублированы словами и иконками: «Вкл/Выкл», «копим», «надето», галочки дел | реализовано |
| 3.6 | Единообразная кнопка «Назад», контраст, TalkBack | Общий `ScreenHeader` с описанием для TalkBack; живой прогон с TalkBack не проведён | в работе |
| 3.6 | Звуки и анимации отключаются | «Настройки» → «Звуки», «Анимации» | реализовано |
| 3.6 | Действия, меняющие прогресс, требуют подтверждения | Покупка, снятие из копилки, сброс, удаление, демо — через окна подтверждения | реализовано |

## Сдача (ТЗ 4, 5, 7.2) — где что лежит

| № | Требование | Файл | Статус |
|---|---|---|---|
| 5.1 | README: назначение, состав, быстрый запуск | [README.md](../README.md) | реализовано |
| 5.2 | Окружение, версии, сборка релизного APK | [README.md](../README.md#релизная-сборка) | реализовано |
| 5.3 | Архитектура | [06-architecture.md](06-architecture.md) (часть текста описывает прежний модуль `:domain`, нужно обновить) | в работе |
| 5.4 | Структура данных профиля, экономики, заданий | [02-domain-model.md](02-domain-model.md), [05-content-model.md](05-content-model.md) | в работе |
| 5.5 | Матрица требований | этот файл | реализовано |
| 5.6 | Формулы баланса, наград, состояния и роста | [04-rules-and-formulas.md](04-rules-and-formulas.md), [11-economy.md](11-economy.md) | в работе |
| 5.7 | Карта образовательного контента | [05-content-model.md](05-content-model.md): таблицы заданий и приключений | реализовано |
| 5.8 | UX/UI и настройки доступности | [07-screens.md](07-screens.md) | в работе |
| 5.9 | Разрешения, данные, удаление профиля | [06-architecture.md](06-architecture.md), [03-processes.md](03-processes.md) | реализовано |
| 5.10 | Тест-кейсы и отчёт с устройства | [09-test-cases.md](09-test-cases.md): тест-кейсы есть, отчёт пустой | в работе |
| 5.11 | Ограничения и план развития | [10-team-plan.md](10-team-plan.md) | в работе |
| 5.12 | Лицензии | [licenses.md](licenses.md) | в работе |
| 5 | Документ в DOCX или PDF | — | не начато |
| 4 | Презентация (PPTX или PDF), резервное видео до 3 минут | — | не начато |
| 7.2 | Релизный тег финальной версии | — | не начато |
