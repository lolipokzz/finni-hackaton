# Лицензии сторонних материалов

Перечень по ТЗ, раздел 3.3 («все изображения, шрифты, звуки и библиотеки должны иметь право на использование»)
и разделу 5, п. 12. Дополняется при добавлении любого стороннего ассета или библиотеки.

«Собственная разработка» — материал сделан командой проекта; права на него принадлежат команде, и она
разрешает его использование и распространение в составе приложения.

## Библиотеки

В APK попадают только зависимости `implementation` из `app/build.gradle.kts`. Версии — в
`gradle/libs.versions.toml`.

| Библиотека | Версия | Зачем | Лицензия |
|---|---|---|---|
| Kotlin stdlib | 2.4.20 | язык | Apache License 2.0 |
| kotlinx.serialization (json) | 1.11.0 | сохранение игры и настроек, `feedback.json` | Apache License 2.0 |
| kotlinx.coroutines (транзитивно) | 1.11.0 | асинхронность | Apache License 2.0 |
| AndroidX Core KTX | 1.19.0 | системные утилиты | Apache License 2.0 |
| AndroidX Activity Compose | 1.13.0 | точка входа Compose | Apache License 2.0 |
| Jetpack Compose (BOM) — UI, Graphics, Material 3, Tooling Preview | 2026.09.00 | интерфейс | Apache License 2.0 |
| AndroidX Lifecycle (runtime, ViewModel Compose, ViewModel Navigation 3, runtime Compose) | 2.11.0 | ViewModel, жизненный цикл | Apache License 2.0 |
| AndroidX Navigation 3 (runtime, ui) | 1.1.7 | навигация между экранами | Apache License 2.0 |
| AndroidX DataStore | 1.2.1 | хранение на устройстве | Apache License 2.0 |
| Koin (core, android, compose, compose-viewmodel) | 4.2.2 | внедрение зависимостей | Apache License 2.0 |
| Filament (filament-android, gltfio-android, filament-utils-android) | 1.71.5 | 3D-питомец | Apache License 2.0 |

Только для тестов, в APK не попадают: JUnit 4.13.2 (Eclipse Public License 1.0), kotlinx-coroutines-test,
koin-test (Apache License 2.0), AndroidX Test, Espresso, Compose UI Test (Apache License 2.0).

Apache License 2.0 и EPL 1.0 разрешают использование и распространение в составе приложения, в том числе
бесплатного; нужно сохранять уведомления об авторских правах. Сторонних SDK аналитики, рекламы и платежей нет.

## Шрифты

Отдельных шрифтов в приложении нет: `FinniFont = FontFamily.Default` (`presentation/theme/Type.kt`), то есть
системный шрифт Android (Roboto на большинстве устройств). Файлы шрифтов в APK не входят.

## Иконки

Иконки интерфейса (`app/src/main/res/drawable/ic_*.xml`) подобраны через каталог Supericons и
перекрашены в цвета приложения. Источник каждой иконки указан комментарием в начале её файла.

| Набор | Откуда | Лицензия | Где используется |
|---|---|---|---|
| MingCute Icon | npm `mingcute_icon` 2.9.72, https://github.com/Richard9394/MingCute | Apache License 2.0 | почти все иконки |
| Phosphor Icons | npm `@phosphor-icons/core`, https://github.com/phosphor-icons/core | MIT | `ic_cap`, `ic_glasses`, `ic_soap`, `ic_fruits` (в MingCute нет подходящих) |

Монета `ic_coin` (золотая, с лапкой) нарисована командой, она не из набора.

## Иконка приложения

Иконка (`mipmap-*/ic_launcher*`) и иконка 512×512 для карточки RuStore (`docs/rustore/icon-512.png`) сделаны из
изображения Финни — собственной разработки команды. Фон адаптивной иконки — градиент, продолжающий фон
изображения.

## Звуки

Файлы — `app/src/main/res/raw/`. Проигрывает `PetSounds` (`presentation/components/PetSounds.kt`).

| Файл | Что это | Источник | Лицензия |
|---|---|---|---|
| `pet_purr.wav` | мурчание, пока питомца гладят | запись «Cat - Purr - Meow.wav», автор soundmary, https://freesound.org/s/117612/ | Creative Commons 0 (общественное достояние) |
| `pet_hit_head.wav`, `pet_hit_foot.wav` | реакция на нажатие по голове и лапе | синтезированный командой удар + короткое «мяу», вырезанное из той же записи #117612 | CC0 (исходник) + собственная работа команды |
| `pet_boing.wav` | «бойнг» | синтезирован командой | собственная работа команды |

CC0 не требует указывать автора; автор указан из вежливости и для проверяемости.

«Кот повторяет слова» (`PetVoice`) не использует записанных звуков: он в реальном времени меняет голос с
микрофона на устройстве и ничего не сохраняет.

## 3D-модели питомца

Файлы — `app/src/main/assets/cat/`.

| Файл | Что это | Источник | Лицензия |
|---|---|---|---|
| `baby.glb`, `teen.glb`, `adult.glb` | кот на трёх стадиях роста: меш, скелет `KittenRig`, материал `Kitten_Opaque`, текстура шерсти, вещи гардероба (`Acc_*`) | модель кота, стадии роста, анимации (приветствие, покой, эмоции, реакции на нажатия, поглаживание, «SixSeven») и вещи гардероба сделаны командой в Blender (экспорт Khronos glTF Blender I/O) | собственная разработка |
| `skins/*.webp` | 8 раскрасок шерсти | перекрашенная командой текстура шерсти модели | собственная разработка |

## Фоны и картинки комнаты

Файлы — `app/src/main/res/drawable-nodpi/`. Все сняты в Blender той же камерой, что и питомец, чтобы кот стоял
на полу (см. `presentation/components/RoomBackground.kt`).

| Файл | Что это | Источник | Лицензия |
|---|---|---|---|
| `room_background.jpg` | комната Финни | рендер сцены, собранной командой в Blender | собственная разработка |
| `room_background_renovated.jpg` | комната после цели «Ремонт в комнате» | рендер той же сцены с другими обоями, полом и мебелью | собственная разработка |
| `room_bed.png`, `room_bike.png`, `room_sea.png` | кроватка, велосипед, сувенир с моря — слои поверх комнаты | рендеры командой в Blender | собственная разработка |
| `trip_sea.jpg` | пляж во время поездки на море | рендер командой в Blender | собственная разработка |

Сцены собраны командой без сторонних моделей, HDRI и текстур.

## При добавлении новых материалов

- Сторонний материал — только с лицензией, разрешающей распространение в составе приложения; строка в этом файле
  добавляется в том же PR.
- Если лицензия требует указать автора (CC BY и подобные), автор указывается и в приложении («Настройки» →
  «О приложении»), и в карточке RuStore.
