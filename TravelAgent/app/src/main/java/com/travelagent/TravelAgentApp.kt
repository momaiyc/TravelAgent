package com.travelagent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TravelAgentApp : Application() {
    
    companion object {
        lateinit var instance: TravelAgentApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
