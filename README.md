# Питомец Финни

Игровое Android-приложение для детей 7–11 лет: ребёнок заботится о виртуальном питомце, планируя
игровой бюджет на обязательное, необязательное и копилку. Прототип для конкурса Департамента финансов
города Москвы (ТЗ 2026). Без реальных денег, рекламы, аккаунтов и сети — все данные только на устройстве.

## Документация

Вся проектная документация — в папке [docs/](docs/README.md): продукт и границы, доменная модель,
бизнес-процессы, формулы экономики, учебный контент, архитектура, экраны, матрица
требований, тест-кейсы, план команды. Начинать с [docs/README.md](docs/README.md).
Лицензии сторонних материалов — [docs/licenses.md](docs/licenses.md).

## Стек

- Kotlin 2.4, Jetpack Compose (Material 3), Navigation 3
- Koin — внедрение зависимостей
- Jetpack DataStore + kotlinx.serialization — сохранение игры и настроек (JSON-файлы на устройстве)
- Filament (gltfio) — 3D-питомец
- JUnit 4 + kotlinx-coroutines-test — unit-тесты без эмулятора
- `minSdk 26` (Android 8.0, ТЗ 3.1), `targetSdk 37`, `applicationId ru.larpinovplay.finniapp`,
  `versionName 1.0`, `versionCode 1`

## Состав репозитория

Один Gradle-модуль `:app`, слои разделены по пакетам (подробно — [docs/06-architecture.md](docs/06-architecture.md)):

```
app/src/main/java/ru/larpinovplay/finniapp/
  domain/        игровая логика без Android: модели, правила экономики (GameEngine, GameRules), задания
  data/          хранение (DataStore), репозитории, учебный контент (data/content)
  presentation/  экраны Compose, ViewModel, навигация, 3D-питомец
  app/           Application и модули Koin
app/src/main/assets/
  content/       тексты обратной связи (feedback.json)
  cat/           3D-модели кота по стадиям роста и раскраски
app/src/test/    unit-тесты логики, контента, хранения и ViewModel
docs/            документация
scripts/dev/     вспомогательные скрипты для ручной проверки на эмуляторе
```

## Быстрый запуск

Требования: JDK 17 или новее, Android SDK с platform 37 (Android Studio актуальной стабильной версии
ставит всё нужное), Gradle wrapper из репозитория.

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Установка на устройство:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Unit-тесты (без эмулятора):

```bash
./gradlew :app:testDebugUnitTest
```

Полная проверка перед сдачей — тесты, lint и сборка:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

## Релизная сборка

```bash
./gradlew :app:assembleRelease
```

APK: `app/build/outputs/apk/release/app-release-unsigned.apk`. Подпись в сборку пока не подключена: ключей
и паролей в репозитории нет и не будет (ТЗ 3.4). Для сдачи APK подписывается ключом команды, который
хранится вне репозитория, утилитой `apksigner` из Android SDK build-tools:

```bash
apksigner sign --ks /путь/вне/репозитория/finni-release.jks --out finni-release.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Версия и номер сборки — в `app/build.gradle.kts` (`versionName`, `versionCode`).

## Демонстрационный режим и сброс

Главный экран → шестерёнка «Настройки» → «Для взрослых» (арифметический барьер) → «Включить демо» /
«Сбросить профиль» / «Удалить все данные». В демо-режиме недели завершаются кнопкой, без ожидания
реального времени. Порядок демонстрации и ожидаемые значения — в
[docs/09-test-cases.md](docs/09-test-cases.md#e2e-обязательный-сценарий).

## Статус

Обязательный сценарий ТЗ (Приложение А, шаги 1–12) реализован. Статусы по пунктам требований — в
[docs/08-requirements-matrix.md](docs/08-requirements-matrix.md).
