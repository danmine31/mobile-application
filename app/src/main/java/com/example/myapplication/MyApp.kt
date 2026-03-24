package com.example.myapplication

import android.app.Application
import org.osmdroid.config.Configuration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = "com.example.myapplication"
    }
}
