package com.example.scamshield

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScamShieldApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        instance = this
        FirebaseApp.initializeApp(this)
        CoroutineScope(Dispatchers.IO).launch {
            val enabled = container.settingsRepository.settings.first().crashReportingEnabled
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        }
    }

    companion object {
        @Volatile
        private var instance: ScamShieldApp? = null

        fun container(): AppContainer =
            instance?.container
                ?: error("ScamShieldApp not initialised — declare android:name in the manifest")

        fun appContext(): android.content.Context =
            instance ?: error("ScamShieldApp not initialised — declare android:name in the manifest")
    }
}
