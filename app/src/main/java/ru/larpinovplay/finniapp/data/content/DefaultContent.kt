package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.content.Content

/** Справочники, зашитые в код. Заменится загрузкой из JSON-файлов в assets/content (docs/05-content-model.md). */
fun defaultContent(): Content = Content(
    shopItems = defaultShopItems,
    goals = defaultGoals,
    tasks = defaultTasks,
)
