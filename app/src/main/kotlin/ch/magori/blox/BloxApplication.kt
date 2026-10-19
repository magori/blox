package ch.magori.blox

import android.app.Application
import ch.magori.blox.di.AppContainer

/** The application: it creates the [AppContainer] once, for the whole life of the process (ADR 0007). */
class BloxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
