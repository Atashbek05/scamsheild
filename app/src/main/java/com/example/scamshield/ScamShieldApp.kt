package com.example.scamshield

import android.app.Application

class ScamShieldApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
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
