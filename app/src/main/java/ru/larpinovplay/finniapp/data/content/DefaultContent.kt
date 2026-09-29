package ru.larpinovplay.finniapp.data.content

import ru.larpinovplay.finniapp.domain.content.Content

/** Справочники, зашитые в код. Заменится загрузкой из JSON-файлов в assets/content. */
fun defaultContent(): Content = Content(
    shopItems = defaultShopItems,
    goals = defaultGoals,
    levels = defaultLevels,
    adventures = defaultAdventures,
)
