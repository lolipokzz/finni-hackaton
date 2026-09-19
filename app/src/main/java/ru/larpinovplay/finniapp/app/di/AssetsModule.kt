package ru.larpinovplay.finniapp.app.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.larpinovplay.finniapp.data.content.FEEDBACK_ASSET
import ru.larpinovplay.finniapp.data.content.parseFeedback

/** Всё, что читается из assets и потому требует Context. Отдельно от [appModule], чтобы тот поднимался в JVM-тесте. */
val assetsModule = module {
    single { parseFeedback(androidContext().assets.open(FEEDBACK_ASSET).bufferedReader().use { it.readText() }) }
}
