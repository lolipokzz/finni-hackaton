# Питомец Финни

Игровое Android-приложение для детей 7–11 лет: ребёнок заботится о виртуальном питомце, планируя
игровой бюджет на обязательное, желаемое и накопления. Прототип для конкурса Департамента финансов
города Москвы (ТЗ 2026). Без реальных денег, рекламы, аккаунтов и сети — все данные только на устройстве.

## Документация

Вся проектная документация — в папке [docs/](docs/README.md): продукт и границы, доменная модель,
бизнес-процессы, формулы экономики, формат учебного контента, архитектура, экраны, матрица
требований, тест-кейсы, план команды. Начинать с [docs/README.md](docs/README.md).

## Стек

- Kotlin 2.2, Jetpack Compose (Material 3), Navigation Compose
- Jetpack DataStore + kotlinx.serialization для локального состояния
- Чистый Kotlin/JVM-модуль `:domain` с игровой логикой и unit-тестами (JUnit)
- `minSdk 27`, `targetSdk 36`, `applicationId ru.larpinovplay.finniapp`

## Состав репозитория

```
app/      Android-приложение: UI, навигация, хранение, загрузка контента из assets
domain/   игровая логика, модели, правила, валидатор контента (без Android)  — создаётся на этапе 0
docs/     документация
```

## Быстрый запуск

Требования: JDK 17, Android Studio (актуальная стабильная) или Android SDK с platform 36, Gradle wrapper из репозитория.

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Установка на устройство:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Тесты игровой логики (без эмулятора):

```bash
./gradlew :domain:test
```

## Релизная сборка

1. Создать keystore вне репозитория и файл `keystore.properties` в корне (он в `.gitignore`):
   ```
   storeFile=/абсолютный/путь/finni-release.jks
   storePassword=...
   keyAlias=finni
   keyPassword=...
   ```
2. Собрать:
   ```bash
   ./gradlew :app:assembleRelease
   ```
3. APK: `app/build/outputs/apk/release/app-release.apk`. Версия и номер сборки — в `app/build.gradle.kts`
   (`versionName`, `versionCode`) и дублируются в документации при сдаче.

## Демонстрационный режим и сброс

Раздел «Для взрослых» на главном экране (арифметический барьер) → «Демо-режим» / «Сбросить профиль» /
«Удалить все данные». Порядок демонстрации и ожидаемые значения — в [docs/09-test-cases.md](docs/09-test-cases.md#e2e-обязательный-сценарий).

## Статус

Проект на этапе 0 (каркас). Актуальные статусы требований — в
[docs/08-requirements-matrix.md](docs/08-requirements-matrix.md).
