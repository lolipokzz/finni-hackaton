package ru.larpinovplay.finniapp.presentation.screens.home

/** Разделы, доступные с главного экрана (ТЗ 2.5.3, второй пункт). */
enum class HomeSection(val title: String) {
    TASKS("Задания"),
    SHOP("Магазин"),
    SAVINGS("Копилка"),
    PROGRESS("Прогресс"),
    SETTINGS("Настройки"),
    ADULT("Для взрослых"),
}
