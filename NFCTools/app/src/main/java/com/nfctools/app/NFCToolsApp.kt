package com.nfctools.app

import android.app.Application
import android.content.Context

class NFCToolsApp : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
