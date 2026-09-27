package com.example

import android.app.Application
import com.example.data.AppContainer

class LifeManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}
