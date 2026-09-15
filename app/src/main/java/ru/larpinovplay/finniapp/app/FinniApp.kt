package ru.larpinovplay.finniapp.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.larpinovplay.finniapp.app.di.appModule

class FinniApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FinniApp)
            modules(appModule)
        }
    }
}
